/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc.request.process;

import github.hemant.lc.Log;
import github.hemant.lc.config.StrategyConfig;
import github.hemant.lc.request.WebRequestException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoanFilter {

	/**
	 * This API ensures return map will not have duplicate loan id's in the
	 * return value, so that one loan id can be part of results of only one
	 * strategy.
	 * 
	 * @param loans
	 *            Loans which needs to be filtered
	 * @param activeStrategies
	 *            Active strategies which can be used for filtering.
	 * @return Returns Map where key is name of the strategy and value is the
	 *         list of loans which got qualified for that strategy.
	 * @throws WebRequestException 
	 */
	public Map<String, List<Map>> filter(Log log, List<Map> loansList, List<StrategyConfig> activeStrategies) throws WebRequestException {
		Map<String, List<Map>> result = new HashMap<String, List<Map>>();
		// We will keep filtering this list.
		List loans = loansList;
		for (int i = 0; i < activeStrategies.size(); i++) {
			StrategyConfig strategy = activeStrategies.get(i);
			if (!isStrategyActive(strategy)) continue;
			
			// First apply the data filter.
			LoanFilterByStrategy filterByStrategy = new LoanFilterByStrategy(strategy);
			List<Map> strategySelectedLoans = filterByStrategy.filter(loans, log);
			// Show statistics on why loans were not included or included
			if (log.isDebug()) {
				log.debug("Strategy: " + strategy.getName() + " loans: " + loans.size() + " filter stastics: " + filterByStrategy.getStatistics());
			}
			
			// Apply the derived data filter, if we have any loans selected by previous filter.
			if (strategySelectedLoans.size() > 0) {
				// Now remove the loans which are selected by this strategy from main list
				// so that future iterations don't include those loans. This way we will
				// avoid duplicate loans across strategies.
				loans = excludeSelectedLoans(loans, strategySelectedLoans);
				result.put(strategy.getName(), strategySelectedLoans);
			}
		}
		
		return result;
	}
	
	private List excludeSelectedLoans(List allLoans, List strategySelectedLoans) {

		// Create a map of the loan id which is selected.
		Map<String, Boolean> mapStrategySelectedLoans = new HashMap<String, Boolean>();
		for (int i = 0; i < strategySelectedLoans.size(); i++) {
			Map loan = (Map) strategySelectedLoans.get(i);
			mapStrategySelectedLoans.put(LoanUtil.loanId(loan), Boolean.TRUE);
		}
		
		List newLoansList = new ArrayList(allLoans.size() - mapStrategySelectedLoans.size());
		for (int i = 0; i < allLoans.size(); i++) {
			Map loan = (Map) allLoans.get(i);
			if (!mapStrategySelectedLoans.containsKey(LoanUtil.loanId(loan))) {
				newLoansList.add(loan);
			}
		}
		
		return newLoansList;
	}

	private boolean isStrategyActive(StrategyConfig strategy) {
		return strategy.isActive();
	}

	class LoanSelectionStatus {
		private boolean status;
		private String selectedByStrategy;
		
		LoanSelectionStatus(boolean status, String selectedByStrategy) {
			this.status = status;
			this.selectedByStrategy = selectedByStrategy;
		}

		boolean status() {
			return status;
		}

		String selectedByStrategy() {
			return selectedByStrategy;
		}

	}
}
