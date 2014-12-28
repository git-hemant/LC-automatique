/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc.config;

import java.math.BigDecimal;

/**
 * Wraps the module of configuration which is related to derived data from loan
 * information.
 */
public class StrategyPopularityConfig {

	private BigDecimal minPopularityIndex;
	private double minPercentageFunded;
	private int averageFundingPerPerson = -1;

	public int getAverageFundingPerPerson() {
		return averageFundingPerPerson;
	}

	void setAverageFundingPerPerson(int averageFundingPerPerson) {
		this.averageFundingPerPerson = averageFundingPerPerson;
	}

	public BigDecimal getMinPopularityIndex() {
		return minPopularityIndex;
	}

	void setMinPopularityIndex(BigDecimal minPopularityIndex) {
		this.minPopularityIndex = minPopularityIndex;
	}

	public double getMinPercentageFunded() {
		return minPercentageFunded;
	}

	void setMinPercentageFunded(double minPercentageFunded) {
		this.minPercentageFunded = minPercentageFunded;
	}
}
