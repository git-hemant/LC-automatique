/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc.config;

import java.util.Map;

/**
 * Container for configuration related to strategy.
 */
public class StrategyConfig {

	private String name;
	private boolean active;
	private int amountPerNote;
	private int maxOrdersPerDay;
	private String targetPortfolioName;
	private Long targetPortfolioId;
	private Map<String, Object> loansDataFilter;
	private StrategyPopularityConfig loansPopularityConfig;

	// Ensure the package level access for the constructor
	StrategyConfig() {
	}
	
	public String targetPortfolioName() {
		return targetPortfolioName;
	}
	
	public void setTargetPortfolioName(String targetPortfolioName) {
		this.targetPortfolioName = targetPortfolioName;
		if (this.targetPortfolioName != null) {
			this.targetPortfolioName = this.targetPortfolioName.trim();
		}
	}

	public Map<String, Object> getLoansDataFilter() {
		return loansDataFilter;
	}

	public void setLoansDataFilter(Map<String, Object> loansDataFilter) {
		this.loansDataFilter = loansDataFilter;
	}

	public StrategyPopularityConfig getLoansPopularityConfig() {
		return loansPopularityConfig;
	}

	public void setLoansPopularityConfig(StrategyPopularityConfig loansPopularityConfig) {
		this.loansPopularityConfig = loansPopularityConfig;
	}

	public void setTargetPortfolioId(Long targetPortfolioId) {
		this.targetPortfolioId = targetPortfolioId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public int getAmountPerNote() {
		return amountPerNote;
	}

	public void setAmountPerNote(int amountPerNote) {
		this.amountPerNote = amountPerNote;
	}

	public int getMaxOrdersPerDay() {
		return maxOrdersPerDay;
	}

	public void setMaxOrdersPerDay(int maxOrderCount) {
		this.maxOrdersPerDay = maxOrderCount;
	}

	public Long getTargetPortfolioId() {
		return targetPortfolioId;
	}

}
