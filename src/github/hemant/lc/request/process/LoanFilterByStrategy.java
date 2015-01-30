/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc.request.process;

import github.hemant.lc.Log;
import github.hemant.lc.config.StrategyConfig;
import github.hemant.lc.request.WebRequestException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

class LoanFilterByStrategy {
	private StrategyConfig config;
	private Map<String, Integer> filterStatistics;
	private static ScriptEngine engine;

	public LoanFilterByStrategy(StrategyConfig strategyConfig) {
		config = strategyConfig;
		filterStatistics = new HashMap<String, Integer>();
	}

	public List<Map> filter(List<Map> allLoans, Log log) throws WebRequestException {
		List<Map> filteredLoans = new ArrayList<Map>();
		for (Map loan : allLoans) {
			StringBuffer exclusionReason = new StringBuffer();
			if (isLoanIncludedByData(loan, exclusionReason) 
					&& isLoanIncludedByDerivedData(loan, exclusionReason)) {
				filteredLoans.add(loan);
			} else {
				// For common cases like grade and subgrade we don't want to show messages.
				if (exclusionReason.length() > 0) {
					//log.debug("Id: " + LoanUtil.loanId(loan) + " reason for filter: " + exclusionReason);
				}
			}
		}
		return filteredLoans;
	}
	
	// Filter by data - start
	private boolean isLoanIncludedByData(Map loan, StringBuffer exclusionReason) throws WebRequestException {
		Map<String, Object> dataFilterMap = config.getLoansDataFilter();
		if (dataFilterMap == null || dataFilterMap.isEmpty()) return true;
		for (Iterator<?> iterator = dataFilterMap.keySet().iterator(); iterator.hasNext();) {
			String filterField = null;
			String value = null;
			try {
				filterField = (String) iterator.next();
				value = (String) loan.get(filterField);
				// fieldMap is the set of filters we need to perform on.
				@SuppressWarnings("unchecked")
				Map<String, Object> fieldMap = (Map<String, Object>) dataFilterMap.get(filterField);
				if (!isLoanIncludedByField(loan, filterField, value, fieldMap, exclusionReason)) {
					return false;
				}
			} catch (Exception e) {
				String msg = "Exception processing field: " + filterField + " value: " + value;
				throw new WebRequestException(msg, e);
			}
		}
		return true;
	}
	
	private boolean isLoanIncludedByField(Map loan, String fieldName, String value, Map<String, Object> fieldMap, StringBuffer exclusionReason) 
			throws WebRequestException {
		// Look for any inclusions
		@SuppressWarnings("unchecked")
		Map<String, Object> fieldIncludeMap = (Map<String, Object>) fieldMap.get("include");
		if (fieldIncludeMap != null && !isFieldValueInMap(fieldIncludeMap, value)) {
			if (!fieldName.toLowerCase().contains("grade"))
				exclusionReason.append("include - " + fieldName + " value: " + value + " not found in pre-defined list: " + fieldIncludeMap);
			registerExclusion("include", fieldName);
			return false;
		}
		
		// Look for any exclusions
		@SuppressWarnings("unchecked")
		Map<String, Object> fieldExcludeMap = (Map<String, Object>) fieldMap.get("exclude");
		if (fieldExcludeMap != null && isFieldValueInMap(fieldExcludeMap, value)) {
			if (!fieldName.toLowerCase().contains("grade")) 
				exclusionReason.append("exclude - " + fieldName + " value: " + value + "  found in pre-defined list: " + fieldExcludeMap);
			registerExclusion("exclude", fieldName);
			return false;
		}
		
		// Look for minimum length check on the value.
		Object minLength = fieldMap.get("minLength");
		if (minLength != null) {
			// Check if the value meet the minimum length filter.
			if (isNull(value) || value.toString().length() < Integer.parseInt(minLength.toString())) {
				exclusionReason.append("minLength - " + fieldName + " value: " + value + "  is more than allowed minLength: " + minLength);
				registerExclusion("minLength", fieldName);
				return false;
			}
		}
		
		// Look for greater than operator on the value.
		Object greaterThan = fieldMap.get("greaterThan");
		if (greaterThan != null) {
			if (isNull(value)) {
				exclusionReason.append("greaterThan - " + fieldName + " value: " + value + "  is null instead of number");				
				registerExclusion("greaterThan", fieldName);
				return false;
			}
			greaterThan = evaluateExpressionValue(loan, (String) greaterThan);
			// Check if the field value is greater than value specified in the filter.
			if (!(Double.parseDouble(value.toString()) > Double.parseDouble(greaterThan.toString()))) {
				exclusionReason.append("greaterThan - " + fieldName + " value: " + value + "  is not greater than :" + greaterThan);
				return false;
			}
		}
		
		// Look for less than operator on the value.
		Object lessThan = fieldMap.get("lessThan");
		if (lessThan != null) {
			if (isNull(value)) {
				exclusionReason.append("lessThan - " + fieldName + " value: " + value + "  is null instead of number");
				registerExclusion("lessThan", fieldName);
				return false;
			}
			lessThan = evaluateExpressionValue(loan, (String) lessThan);
			// Check if the field value is less than value specified in the filter.
			if (!(Double.parseDouble(value.toString()) < Double.parseDouble(lessThan.toString()))) {
				exclusionReason.append("lessThan - " + fieldName + " value: " + value + "  is not less than :" + lessThan);
				registerExclusion("lessThan", fieldName);
				return false;
			}
		}
		
		Object isEmpty = fieldMap.get("isEmpty");
		if (isEmpty != null) {
			boolean shouldBeEmpty = Boolean.parseBoolean(isEmpty.toString()); 
			if ((shouldBeEmpty && value != null) || (!shouldBeEmpty && isNull(value))) {
				exclusionReason.append("isEmpty - " + fieldName + " value: " + value + "  is expected to be empty :" + shouldBeEmpty);
				registerExclusion("isEmpty", fieldName);
				return false;
			}
		}
		
		return true;
	}

	// TODO - This evaluates RHS of greaterThan or lessThan
	// but may we can have entire expression input by the user
	// instead of just RHS.
	private String evaluateExpressionValue(Map loan, String value) throws WebRequestException {
		value = value.trim();
		// If the RHS value specified by the user is numeric literal
		// then return the value.
		if (Utils.isNumeric(value)) return value;
		
		// At this point either the user has written complex expression
		// in the RHS, or there is typo in the RHS. We will assume it is
		// a expression which might event have loan fields, so let's try
		// to replace loan fields in the value.
		for (Iterator iterator = loan.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			String keyValue = (String) loan.get(key);
			if (value.contains(key)) {
				// Replace all key with values
				value = value.replaceAll(key, keyValue);
			}
		}
		
		// Now use expression evaluator to convert this expression
		// into numeric value.
		if (engine == null) {
			ScriptEngineManager mgr = new ScriptEngineManager();
		    engine = mgr.getEngineByName("JavaScript");
		}
		try {
			value = engine.eval(value).toString();
		} catch (ScriptException e) {
			throw new WebRequestException("Error evaluating expression " + value, e);
		}
		// At this point value should be a number
		return value;
	}

	/**
	 * Returns true if field value matches any of the values in field map.
	 * @param fieldMap
	 * @param value
	 * @return
	 */
	private boolean isFieldValueInMap(Map<String, Object> fieldMap, Object value) {
		int counter = 1;
		while (true) {
			Object fieldValue = fieldMap.get(Integer.toString(counter++));
			if (fieldValue == null) break;
			if (isValueSame(value, fieldValue)) return true;
		}
		return false;
	}
	// Filter by data - end

	// Filter by derived data - start
	private boolean isLoanIncludedByDerivedData(Map loan, StringBuffer exclusionReason) {
		double percentageFunded = LoanUtil.getPercentageFunded(loan);
		// Exclude loans which are not funded at all.
		double expectedFunded = config.getLoansPopularityConfig().getMinPercentageFunded();
		if (percentageFunded < expectedFunded) {
			exclusionReason.append("minPercentageFunded - funded: " + percentageFunded + " expected min funded: " + expectedFunded);
			registerExclusion("followPopular:minPercentageFunded");
			return false;
		}
		BigDecimal popIndex = LoanUtil.getPopularityIndex(loan);
		BigDecimal expectedPopIndex = config.getLoansPopularityConfig().getMinPopularityIndex();
		if (popIndex.compareTo(expectedPopIndex) != 1) {
			exclusionReason.append("minPopularityIndex - popularity index: " + popIndex + " expected popularity index: " + expectedPopIndex);
			registerExclusion("followPopular:minPopularityIndex");
			return false;
		}
		
		// Check if the user has specified some constraint here.
		if (config.getLoansPopularityConfig().getAverageFundingPerPerson() != -1) {
			double loanFunded = LoanUtil.loanFundedAmount(loan);
			Integer investorCount = LoanUtil.investorCount(loan);
			// TODO - investorCount is coming null as of now.
			if ((loanFunded / investorCount.intValue()) < config.getLoansPopularityConfig().getAverageFundingPerPerson()) {
				registerExclusion("followPopular:averageFundingPerPerson");
				return false;
			}
		}
		return true;
	}	
	// Filter by derived data - end
	private static boolean isValueSame(Object o1, Object o2) {
		if (o1 == o2 || o1.equals(o2)) return true;
		if (o1 != null && o2 != null && o1.toString().equals(o2.toString())) return true;
		return false;
	}
	
	public String getStatistics() {
		Iterator<String> keys = filterStatistics.keySet().iterator();
		StringBuilder msg = new StringBuilder();
		while (keys.hasNext()) {
			String statKey = keys.next();
			Object value = filterStatistics.get(statKey);
			msg.append(statKey).append("=").append(value);
			msg.append(" ");
		}
		return msg.toString();
	}

	protected void registerExclusion(String op, String fieldName) {
		String key = op + "-" + fieldName;
		registerExclusion(key);
	}

	protected void registerExclusion(String key) {
		Integer count = filterStatistics.get(key);
		if (count == null) {
			count = new Integer(1);
		} else {
			count = new Integer(count.intValue() + 1);
		}
		filterStatistics.put(key, count);
	}

	protected boolean isNull(String v) {
		return v == null || v.length() == 0 || "null".equals(v);
	}
}
