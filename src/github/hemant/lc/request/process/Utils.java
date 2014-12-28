/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc.request.process;

import github.hemant.lc.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Utils {

	private Utils() {
	}

	public static String brief(Map<String, String> loan) {
		StringBuilder builder = new StringBuilder();
		builder.append("Id=").append(LoanUtil.loanId(loan));
		builder.append(" ");
		builder.append("PopIndex=").append(LoanUtil.getPopularityIndex(loan));
		builder.append(" ");
		builder.append("Funded%=").append(LoanUtil.getPercentageFunded(loan));
		builder.append(" ");
		String[] fields = {"subGrade", "addrState", "annualInc", "revolUtil", "revolBal", "purpose", "inqLast6Mths", "totalBalExMort", "mthsSinceLastDelinq" };
		for (int i = 0; i < fields.length; i++) {
			if (i > 0) builder.append(" ");
			builder.append(fields[i] + "=").append(loan.get(fields[i]));
		}
		return builder.toString();
	}
	
	public static int countOfSuccessfullOrders(Map orderResult) {
		int count = 0;
		List<Map> orderConfirmations = (List<Map>) orderResult.get("orderConfirmations");
		if (orderConfirmations == null) return count;
		for (Map orderConfirmation : orderConfirmations) {
			List status = (List) orderConfirmation.get("executionStatus");
			if (status.contains("ORDER_FULFILLED")) count++;
		}
		return count;
	}
	

	public static void printLoanInfo(Log log, List<Map> loans) {
		for (Map loan : loans) {
			log.info(Utils.brief(loan));
		}
	}
	
	public static List<Map> maxLoans(List<Map> loans, List<Map> excludeLoans, int max) {
		int orderSize = Math.min(max, loans.size());
		List<Map> orderReadyList = new ArrayList<Map>(orderSize);
		for (int i = 0; orderReadyList.size() < orderSize;i++) {
			if (excludeLoans != null && excludeLoans.contains(loans.get(i))) {
				continue;
			}
			orderReadyList.add(loans.get(i));
		}
		return orderReadyList;
	}
	
}
