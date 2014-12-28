/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc.request.process;

import github.hemant.lc.Log;
import github.hemant.lc.config.Config;
import github.hemant.lc.config.StrategyConfig;
import github.hemant.lc.request.SubmitOrderRequest;
import github.hemant.lc.request.WebRequestException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

// Execute the strategy
public class StrategyExecutor {

	/** 
	 * This map is used to track the max orders per day. 
	 * Key is date in string format mm/dd/yyyy and value is map
	 * where in value map key is name of strategy and value is AtomicInteger which
	 * tracks the count of orders.
	 */
	private static Map<String, Map<String, AtomicInteger>> maxOrdersTrack = new HashMap<String, Map<String, AtomicInteger>>();
	private Config config;
	private StrategyConfig strategyConfig;
	private Log log;
	private int successfulOrdersInThisSession;
	
	public StrategyExecutor(Config config, StrategyConfig strategyConfig) {
		this.strategyConfig = strategyConfig;
		this.config = config;
		this.log = Log.instance(config);
		this.successfulOrdersInThisSession = 0;
	}
	
	private static String _getDateKey() {
		StringBuilder buff = new StringBuilder();
		Calendar cal = Calendar.getInstance();
		buff.append((cal.get(Calendar.MONTH) + 1));
		buff.append("/");
		buff.append(cal.get(Calendar.DAY_OF_MONTH));
		buff.append("/");
		buff.append(cal.get(Calendar.YEAR));
		return buff.toString();
	}
	
	public static int successfulOrderCountForDay(String strategyName) {
		Map<String, AtomicInteger> maxCount = maxOrdersTrack.get(_getDateKey());
		if (maxCount != null) {
			AtomicInteger ai = maxCount.get(strategyName);
			if (ai != null) return ai.get();
		}
		return 0;
	}
	
	public int successfulOrdersInThisSession() {
		return successfulOrdersInThisSession;
	}
	
	public int maxAllowedOrdersPerDay() {
		return strategyConfig.getMaxOrdersPerDay();
	}
	
	public String strategyName() {
		return strategyConfig.getName();
	}
	
	private int remainingAllowedOrders() {
		return maxAllowedOrdersPerDay() - successfulOrderCountForDay(strategyName());
	}
	
	public boolean isActive() {
		return (strategyConfig.isActive() && remainingAllowedOrders() > 0);
	}

	public Map execute(List<Map> qualifiedLoans) throws WebRequestException 
	{
		//Utils.printLoanInfo(log, availableLoans);
		// Select the loans which we order based on configured count.
		List<Map> selectedLoans = Utils.maxLoans(qualifiedLoans, null, remainingAllowedOrders());
		if (config.logLevel() == Log.DEBUG) {
			log.info("Strategy " + strategyName() + " following loans would be ordered:");
			Utils.printLoanInfo(log, selectedLoans);
		}
		if (config.testMode()) {
			log.error("Skipped ordering loans because 'testMode' is true.");
		} else {
			String trackFile = config.trackFile();
			File file = new File(trackFile);
			try {
				if (!file.exists()) file.createNewFile();
			} catch (IOException e) {
				throw new WebRequestException(e);
			}			
			Properties previousOrders = new Properties();
			FileInputStream inputStream = null;
			try {
				inputStream = new FileInputStream(file);
				previousOrders.load(inputStream);
			} catch (IOException e) {
				throw new WebRequestException(e); 
			}
			
			// Verification step, this would throw exception if loan is already ordered.
			// as we have already filtered ordered loans, so this should never happen
			// in ideal conditions.
			verifyLoansNotAlreadyOrdered(previousOrders, qualifiedLoans);
			
			SubmitOrderRequest orderRequest = new SubmitOrderRequest(config.investorId(), config.apiKey());
			Map orderResult = orderRequest.submitOrder(strategyConfig, selectedLoans);
			
			int successfulOrders = Utils.countOfSuccessfullOrders(orderResult);
			
			// Check for error during the execution.
			if (successfulOrders == 0 && orderResult.get("errors") != null) {
				List<Map> orderErrors = (List<Map>) orderResult.get("errors");
				if (orderErrors != null) {
					List<String> errorTxt = new ArrayList<String>(orderErrors.size());
					for (Map orderError : orderErrors) {
						String msg = (String) orderError.get("message");
						if (msg != null && !errorTxt.contains(msg)) {
							errorTxt.add(msg);
						}
					}
					log.error("Order submittion returned error: " + errorTxt);
				}
				
				return orderResult;
			}
			
			// Update the map which tracks previous orders.
			updateOrderCount(successfulOrders);
			if (successfulOrders > 0) {
				log.info("Strategy: \"" + strategyConfig.getName() + "\" executed successful loans: " + successfulOrders);
				showOrderResult(orderResult);
				
				// Now save these newly ordered loans in the track file.
				List<Map> orderConfirmations = (List<Map>) orderResult.get("orderConfirmations");
				for (Map orderConfirmation : orderConfirmations) {
					List executionStatus = (List) orderConfirmation.get("executionStatus");
					if (executionStatus.contains("ORDER_FULFILLED")) {
						String loanId = (String) orderConfirmation.get("loanId");
						Map<String, String> loan = findLoanById(qualifiedLoans, loanId);
						previousOrders.setProperty(loanId, Utils.brief(loan));
					}
				}
				FileOutputStream outputStream = null;
				try {
					outputStream = new FileOutputStream(file);
					previousOrders.store(outputStream, "Updated on " + new Date() + " ordered loans: " + successfulOrders);
				} catch (IOException e) {
					// This exception warrent throwing exception.
					throw new WebRequestException(e);
				}
				if (outputStream != null)
				try {
					outputStream.flush();
					outputStream.close();
				} catch (IOException e) {
				}
				try {
					inputStream.close();
				} catch (IOException e) {
				}				
			}
			return orderResult;
		}
		return null;
	}
	
	private Map<String, String> findLoanById(List<Map> loans, String loanId) {
		for (Map loan : loans) {
			if (loanId.equals(LoanUtil.loanId(loan))) return loan;
		}
		return null;
	}

	private void verifyLoansNotAlreadyOrdered(Properties previousOrders, List<Map> qualifiedLoans) {
		for (Map loan : qualifiedLoans) {
			String id = LoanUtil.loanId(loan);
			if (previousOrders.getProperty(id) != null) {
				throw new RuntimeException("Loan id: " + id + " is already present in tracking file.");
			}
		}
	}

	private void updateOrderCount(int successfulOrders) {
		this.successfulOrdersInThisSession = successfulOrders;
		String dateKey = _getDateKey();
		Map<String, AtomicInteger> strategiesCountMap = maxOrdersTrack.get(dateKey);
		if (strategiesCountMap == null) {
			// Take the lock on the static object.
			synchronized (maxOrdersTrack) {
				// Try again in the synchronized block
				strategiesCountMap = maxOrdersTrack.get(dateKey);
				// If we are create key for the new day, then
				// show summary of previous day and delete previous days key. 
				if (strategiesCountMap == null) {
					showTrackingInfoAndClearMap();
					strategiesCountMap = new HashMap<String, AtomicInteger>();
					maxOrdersTrack.put(dateKey, strategiesCountMap);
				}
			}
		}
		
		// At this point strategiesCountMap will not be null
		AtomicInteger ai = strategiesCountMap.get(strategyConfig.getName());
		// This is first order for this strategy.
		if (ai == null) {
			ai = new AtomicInteger(successfulOrders);
			strategiesCountMap.put(strategyConfig.getName(), ai);
		} else {
			// Update existing order count of the strategy.
			ai.addAndGet(successfulOrders);
		}
	}

	// Show the order count for all the strategies for the day and add new
	// key for the current day.
	private void showTrackingInfoAndClearMap() {
		if (maxOrdersTrack.size() > 0) {
			Iterator<String> previousDays = maxOrdersTrack.keySet().iterator();
			while (previousDays.hasNext()) {
				String prevDay = previousDays.next();
				Map<String, AtomicInteger> dayMap = maxOrdersTrack.get(prevDay);
				if (dayMap.size() > 0) {
					log.error("------Summary for the day: " + prevDay + "-------");
					Iterator<String> strategies = dayMap.keySet().iterator();
					while (strategies.hasNext()) {
						String strategy = strategies.next();
						log.error(strategy + " " + dayMap.get(strategy) + " orders.");
					}
				}
			}
			maxOrdersTrack.clear();
		}
	}

	private void showOrderResult(Map orderResult) {
		List<Map> orderConfirmations = (List<Map>) orderResult.get("orderConfirmations");
		for (Map orderConfirmation : orderConfirmations) {
			
			String successMsg = " Id: " + orderConfirmation.get("loanId") + " Amount invested: "
					+ orderConfirmation.get("investedAmount") + " Status: " + orderConfirmation.get("executionStatus");
			log.info(successMsg);
		}
	}

	@SuppressWarnings("unused")
	private void executeAgain() {
		// If we had more loans available then selected loans initially then
		// we should validate if some order didn't succeed then can we send
		// more order from existing filter list.
//		if (availableLoans.size() > selectedLoans.size()) {
//			int orderCount = Utils.countOfSuccessfullOrders(orderResult);
//			// Check if some of the loans didn't succeed.
//			if (orderCount < selectedLoans.size()) {
//				// Make one more attempt to re-use the existing filtered loans.
//				log.info("Filtered loans size is: " + availableLoans.size() + " while successfull orders are: "
//						+ orderCount
//						+ ", so we will attempt to use existing set of filtered loans for the remaining order.");
//				selectedLoans = Utils.maxLoans(availableLoans, selectedLoans, maxAllowedOrders - orderCount);
//				SubmitOrderResult secondOrderResult = new SubmitLoanOrder(config).submitOrder(selectedLoans, maxAllowedOrders - orderCount);
//				List<OrderInstructConfirmation> orderConfirmations = secondOrderResult.getInstructConfirmations();
//				orderResult.getInstructConfirmations().addAll(orderConfirmations);
//			}
//		}
	}
}
