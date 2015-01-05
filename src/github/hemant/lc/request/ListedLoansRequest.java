/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc.request;

import github.hemant.lc.Log;
import github.hemant.lc.config.Config;
import github.hemant.lc.request.process.LoanTracker;
import github.hemant.lc.request.process.LoanFilter;
import github.hemant.lc.request.process.LoanUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.json.parsers.JSONParser;
import com.json.parsers.JsonParserFactory;

/**
 * @author Hemant
 */
public class ListedLoansRequest extends AbstractRequest {
	
	public ListedLoansRequest(char[] investorId, char[] apiKey) {
		super(investorId, apiKey);
	}

	// Get the list of qualified loans which meet the qualifying criteria.
	// Returns Map<String, List<Map>> where key of the map is name of strategy
	// and value is list of loans which qualfied for that strategy.
	// This method ensures there are no duplicate loan id's across strategies.
	// and they are not previously ordered.
	public Map<String, List<Map>> filterLoans(List<Map> allLoans, Config config) throws WebRequestException {
		Log log = Log.instance(config);
		try {
			Map<String, List<Map>> strategyLoans = new LoanFilter().
					filter(log, allLoans, config.activeStrategies());
			return filterAlreadyOrdered(strategyLoans,  config.trackFile());
		} catch (Exception e) {
			throw new WebRequestException(e);
		}
	}
	
	public List<Map> newLoans(Log log, boolean includeAll) throws WebRequestException {
		JsonParserFactory factory = JsonParserFactory.getInstance();
		JSONParser parser = factory.newJsonParser();
		Map map;
		try {
			map = parser.parseJson(prepareRequest("listing" + (includeAll ? "?showAll=true" : ""), Type.LOANS).execute().returnContent().asStream(), "UTF-8");
			if (!map.containsKey("loans")) {
				log.error("Response doesn't have any 'loans' attribute, response string: " + map);
			}
			return (List<Map>) map.get("loans");
		} catch (Exception e) {
			throw new WebRequestException(e);
		}
	}
	
	private Map<String, List<Map>> filterAlreadyOrdered(Map<String, List<Map>> filteredLoans, String trackFile)
		throws WebRequestException
	{
		// Load the previous order tracking file to exclude any loan which is already ordered earlier.
		File file = new File(trackFile);
		try {
			if (!file.exists()) file.createNewFile();
		} catch (IOException e) {
			throw new WebRequestException(e);
		}
		FileInputStream fis = null;
		Properties previousOrders = new Properties();
		try {
			fis = new FileInputStream(file);
			previousOrders.load(fis);
		} catch (IOException e) {
			throw new WebRequestException(e); 
		}

		Iterator<String> iterator = filteredLoans.keySet().iterator();
		while (iterator.hasNext()) {
			String strategy = iterator.next();
			List<Map> loans = filteredLoans.get(strategy);
			List<Map> existingLoans = new ArrayList<Map>();
			for (int i = 0; i < loans.size(); i++) {
				Map loan = loans.get(i);
				String loanId = LoanUtil.loanId(loan);
				// Exclude any loan which was successfully ordered in any of the previous
				// sessions or ordered in this JVM session.
				String value = previousOrders.getProperty(loanId);
				if (value != null) {
					existingLoans.add(loan);
				} else if (LoanTracker.INSTANCE.isLoanOrdered(Long.parseLong(loanId))) {
					existingLoans.add(loan);
				}
			}
			if (existingLoans.size() > 0) {
				loans.removeAll(existingLoans);
			}
		}
		
		// Now iterate through all loans again, and do following
		// a) Remove strategy, if the list of loans is empty.
		// b) All all loans in the global tracker, as we are going
		//    to order all these loans.
		Map<String, List<Map>> allNewLoans = new HashMap<String, List<Map>>();
		for (iterator = filteredLoans.keySet().iterator(); iterator.hasNext();) {
			String strategy = iterator.next();
			
			List<Map> strategyLoans = filteredLoans.get(strategy);
			if (strategyLoans.size() > 0) {
				List<Map> newStrategyLoans = new ArrayList<Map>();
				allNewLoans.put(strategy, newStrategyLoans);
				
				//Now add all of these loans for this strategy in global tracker.
				for (int i = 0; i < strategyLoans.size(); i++) {
					Map loan = strategyLoans.get(i);
					long loanId = Long.parseLong(LoanUtil.loanId(loan));
					if (LoanTracker.INSTANCE.isLoanOrdered(loanId)) {
						// Ideally this should never happen and this is for validation only
						// as we have already ensured duplicate loan is not present even
						// in different strategies, and we have already excluded loans
						// which we were already tracking.
						throw new RuntimeException("Loan id:" + loanId + " is already added.");
					}
					newStrategyLoans.add(loan);
					LoanTracker.INSTANCE.addLoanOrdered(loanId);
				}
			}
		}
		
		return allNewLoans;
	}
}
