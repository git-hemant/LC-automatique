/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc.request.process;

import github.hemant.lc.Noninstantiable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class LoanUtil extends Noninstantiable {

	private static final String FUNDED_AMT = "fundedAmount";	
	private static final String LOAN_AMT = "loanAmount";
	private static final String LOAN_LIST_DATE = "listD";
	private static final String LOAN_INVESTOR_COUNT = "investorCount";
	private static final String LOAN_ID = "id";

	public static double loanAmount(Map loan) {
		return Double.parseDouble((String) loan.get(LOAN_AMT));
	}

	public static double loanFundedAmount(Map loan) {
		return Double.parseDouble((String) loan.get(FUNDED_AMT));
	}
	
	public static XMLGregorianCalendar loanListedDate(Map loan) {
		try {
			return DatatypeFactory.newInstance().newXMLGregorianCalendar((String)loan.get(LOAN_LIST_DATE));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	// This API would throw NPE/NFE because investor count is coming null. 
	public static int investorCount(Map loan) {
		return Integer.parseInt((String) loan.get(LOAN_INVESTOR_COUNT)) ; 
	}
	
	public static String loanId(Map loan) {
		return (String) loan.get(LOAN_ID);
	}
	
	private static final int PRECISION = 10;
	
	public static double getPercentageFunded(Map loan) {
		double fundedPercentage = (LoanUtil.loanFundedAmount(loan) / LoanUtil.loanAmount(loan) * 100);
		return fundedPercentage;
	}
	
	public static BigDecimal getPopularityIndex(Map loan) {
		double percentageFunded = getPercentageFunded(loan);
		if (percentageFunded == 0.0) return null;
		XMLGregorianCalendar time = LoanUtil.loanListedDate(loan);
		// time diff in the second 
		long timeDiff = (System.currentTimeMillis() - time.toGregorianCalendar().getTimeInMillis()) / 1000;
		return new BigDecimal(percentageFunded).divide(new BigDecimal(timeDiff), PRECISION, RoundingMode.HALF_UP);
	}
	
	public static List<Map> popularLoans(List<Map> loans) {
		Set<LoanFundedDelta> popularLoanSet = new TreeSet<LoanFundedDelta>();
		for (int i = 0; i < loans.size(); i++) {
			Map loan = (Map) loans.get(i);
			LoanFundedDelta fudingDelta = calculateDelta(loan);
			if (fudingDelta != null) popularLoanSet.add(fudingDelta);
		}
		List<Map> popularLoansList = new ArrayList<Map>(loans.size());
		for (LoanFundedDelta lfd : popularLoanSet) {
			popularLoansList.add(lfd.loan());
		}
		return popularLoansList;
	}
	
	private static LoanFundedDelta calculateDelta(Map newLoan) {
		double percentageFunded = getPercentageFunded(newLoan);
		// Exclude loan which is not funded at all.
		if (percentageFunded == 0.0) return null;
		
		LoanFundedDelta lfd = new LoanFundedDelta();
		XMLGregorianCalendar time = LoanUtil.loanListedDate(newLoan);
		// time diff in the second 
		long timeDiff = (System.currentTimeMillis() - time.toGregorianCalendar().getTimeInMillis()) / 1000;
		lfd.popularityIndex = new BigDecimal(percentageFunded).divide(new BigDecimal(timeDiff), PRECISION, RoundingMode.HALF_UP);
		lfd.loan = newLoan;
		return lfd;
	}

	public static class LoanFundedDelta implements Comparable<LoanFundedDelta> {
		// (Funded percentage) / (time elapse)
		// Any number above .5 is good number though max can be 100.
		private BigDecimal popularityIndex;
		private Map loan;
		
		public Map loan() {
			return loan;
		}
		
		public BigDecimal popularityIndex() {
			return popularityIndex;
		}
		
		public int compareTo(LoanFundedDelta diff) {
			return diff.popularityIndex.compareTo(popularityIndex);
		}
	}
}
