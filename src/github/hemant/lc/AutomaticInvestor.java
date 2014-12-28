/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc;

import github.hemant.lc.config.Config;
import github.hemant.lc.config.ConfigLoadException;
import github.hemant.lc.config.StrategyConfig;
import github.hemant.lc.request.BalanceRequest;
import github.hemant.lc.request.ListedLoansRequest;
import github.hemant.lc.request.WebRequestException;
import github.hemant.lc.request.process.LoanUtil;
import github.hemant.lc.request.process.StrategyExecutor;
import github.hemant.lc.request.process.Utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * This is one of the main entry point class as it is invoked by the scheduler
 * for running the job.
 */
public class AutomaticInvestor {

	private Map<XMLGregorianCalendar, Integer> previousLoanRequests = new HashMap<XMLGregorianCalendar, Integer>();
	private Map<String, StrategyExecutor> strategyExecutorMap = new HashMap<String, StrategyExecutor>();
	// List of the strategies which have successfully made all the orders specified in the configuration.
	// Key is the name of strategy and boolean is always true.
	private Map<String, Boolean> strategiesMadeAllOrders = new HashMap<String, Boolean>();
	private Config config;
	private Log log;
	private int currentRetryCount = 1;

	public static void invest() {
		try {
			final AutomaticInvestor as = new AutomaticInvestor();
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					System.out.println("Shutdown hook invoked: Retry count: " + as.currentRetryCount);
				}
			});
			as.process();
			System.out.println(new Date() + " Successfully finished execution.");
		} catch (Throwable e) {
			System.out.println(new Date() + " Exception during execution " + e);
			e.printStackTrace();
		}
	}

	private void initialize() throws ConfigLoadException {
		config = Config.loadConfig();
		log = Log.instance(config);
		String msg = "Started application ";
		msg += config.testMode() ? "in test mode so no order would be submitted."
				: "in production mode so order can be submitted.";
		log.debug(msg);
	}

	private void process() throws ConfigLoadException {
		try {
			initialize();
			
			int minInvestmentAmt = findMinimumInvestmentAmount(config.activeStrategies());
			// First check if Balance is less than 25
			float balance = new BalanceRequest(config.investorId(), config.apiKey()).availableCash();
			
			if (balance < minInvestmentAmt) {
				String msg = "Skipping the execution as balance is $" + balance 
						+ " which is below minimum required investment of $" + minInvestmentAmt + ".";
				log.info(msg);
				return;
			}
	
			for (;currentRetryCount <= config.retryCount(); currentRetryCount++) {
				try {
					execute(config);
					// Check if all the strategies has ordered there maximum quota
					// in that case we don't need to keep retrying.
					if (strategiesMadeAllOrders.size() == strategyExecutorMap.size()) {
						break;
					}
				} catch (WebRequestException we) {
					log.error("Exception during retry count: " + currentRetryCount);
					log.error(we);
				}
				sleep();
			}
		} catch (Throwable e) {
			if (log != null) {
				log.error(e);
			}
			else {
				e.printStackTrace();
			}
		} finally {
			showSummary();
			log.close();
		}
	}

	/**
	 * Find the minium investment amount which we need in any of the strategy.
	 */
	private int findMinimumInvestmentAmount(List<StrategyConfig> activeStrategies) {
		int minAmt = -1;
		for (StrategyConfig strategy : activeStrategies) {
			if (minAmt == -1) {
				minAmt = strategy.getAmountPerNote();
			} else if (minAmt > strategy.getAmountPerNote()) {
				minAmt = strategy.getAmountPerNote();
			}
		}
		return minAmt;
	}

	private void showSummary() {
		Iterator<String> iterator = strategyExecutorMap.keySet().iterator();
		String msg = "";
		int totalOrders = 0;
		while (iterator.hasNext()) {
			String statName = iterator.next();
			StrategyExecutor se = strategyExecutorMap.get(statName);
			int ordersInDayForStrategy = StrategyExecutor.successfulOrderCountForDay(statName);
			if (ordersInDayForStrategy > 0) {
				totalOrders += ordersInDayForStrategy;
				msg += "- Strategy: \"" + se.strategyName() + "\" succesfull orders: " + se.successfulOrdersInThisSession() 
						+ " today total order: " + ordersInDayForStrategy 
						+ " total allowed orders for day: " + se.maxAllowedOrdersPerDay();
				msg += System.lineSeparator(); 
			}
		}
		if (totalOrders == 0) {
			log.debug("No orders got placed during this exceution.");
		} else {
			log.debug("Successfully placed " + totalOrders + " orders using following strategies");
			log.debug(msg);
		}
	}

	private synchronized void sleep() {
		try {
			this.wait(1000 * config.retryGap());
		} catch (Exception e) {
		}
	}

	private void execute(Config config) throws WebRequestException {
		long time = System.currentTimeMillis();
		
		ListedLoansRequest loansRequest = new ListedLoansRequest(config.investorId(), config.apiKey());
		List<Map> allLoans = loansRequest.newLoans(!config.onlyRecentlyListed());
		int beforeFilter = allLoans.size();
		log.debug("Retry count: " + currentRetryCount + ". Result of loan request found: " + beforeFilter + " loans, request executed in "
				+ ((System.currentTimeMillis() - time) / 1000) + " seconds.");
		// Skip rest of the steps if we can't find any loans.
		if (beforeFilter == 0) {
			return;
		}

		// TODO - This is optimization step.
		/*
		Integer previousLoanResultCount = previousLoanRequests.get(loanResult.getAsOfDate());
		if (previousLoanResultCount != null && previousLoanResultCount.intValue() == beforeFilter) {
			return;
		}
		// We don't have any reason to keep all previous records, as we want
		// only last record.
		previousLoanRequests.clear();
		// Store the result in previousLoanRequests for any next request.
		previousLoanRequests.put(loanResult.getAsOfDate(), new Integer(beforeFilter));
		*/

		// If the user want to see popular loans
		if (config.showPopularLoans() > 0) {
			showPopularLoans(allLoans, config.showPopularLoans());
		}
		
		// Now filter out the following loans
		// a) Which doesn't qualify 
		Map<String, List<Map>> qualfiedLoansByStrategy = loansRequest.filterLoans(allLoans, config);
		
		// Access strategies once, if they are not cached yet.
		List<StrategyConfig> strategiesConfig = config.activeStrategies();
		if (strategyExecutorMap.size() == 0) {
			for (StrategyConfig strategyConfig : strategiesConfig) {
				getStrategyExecutor(strategyConfig);
			}
		}
		
		if (qualfiedLoansByStrategy.size() == 0) return;
		for (StrategyConfig strategyConfig : strategiesConfig) {
			StrategyExecutor se = getStrategyExecutor(strategyConfig);
			// Check if this strategy already made all the orders and no more active
			if (!se.isActive()) continue;

			// First check if we have some valid loans for this strategy.
			if (!qualfiedLoansByStrategy.containsKey(se.strategyName())) continue;
			
			List<Map> strategyLoans = qualfiedLoansByStrategy.get(se.strategyName());
			// Check if we have any loans available to order in this strategy.
			if (strategyLoans.size() > 0) {
				se.execute(strategyLoans);
				// Check if the strategy has made all the orders now, in that case add this
				// strategy in separate bucket.
				if (!se.isActive()) {
					strategiesMadeAllOrders.put(se.strategyName(), Boolean.TRUE);
				}
			}
		}
	}

	private StrategyExecutor getStrategyExecutor(StrategyConfig strategyConfig) {
		StrategyExecutor se = strategyExecutorMap.get(strategyConfig.getName());
		if (se == null) {
			se = new StrategyExecutor(config, strategyConfig);
			strategyExecutorMap.put(strategyConfig.getName(), se);
		}
		return se;
	}

	private void showPopularLoans(List<Map> availableLoans, int top) {
		List<Map> popularLoans = LoanUtil.popularLoans(availableLoans);
		int topLoanCount = 0;
		// We are using log.error here because user can control
		// from the configuration whether he wants to see any popular
		// loans or not.
		log.error("Attempt: " + currentRetryCount + " Following are " + top + " most popular loans from total " + availableLoans.size() + ": ");
		for (Map popLoan : popularLoans) {
			if (topLoanCount >= top)
				break;
			topLoanCount++;
			log.error(" " + Utils.brief(popLoan));
		}
	}
}
