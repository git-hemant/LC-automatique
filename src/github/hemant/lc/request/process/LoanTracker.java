/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc.request.process;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps in-memory record of all the loans ordered. This class is essentially
 * used to avoid sending requests for loans which are already ordered or loans
 * which can't be order because they are almost full and thus avoid running
 * unnecessary requests.
 * TODO For larger accounts with hundreds of thousands of loans this class can be
 * be potentially memory leak, as it never releases the loan and instead we can
 * store loans ordered only for the current date.
 */
public enum LoanTracker {
	// Singelton instance.
	INSTANCE;
	
	private Map<Long, Boolean> ordersPlacedInSession = new HashMap<Long, Boolean>();
	
	public boolean isLoanOrdered(long id) { 
		return ordersPlacedInSession.containsKey(id); 
	}
	
	public void addLoanOrdered(long id) {
		if (ordersPlacedInSession.containsKey(id)) { 
			throw new RuntimeException("Loan id: " + id + " is already tracked.");
		}
		ordersPlacedInSession.put(id, Boolean.TRUE);
	}
}
