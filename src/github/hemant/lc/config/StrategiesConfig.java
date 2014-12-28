/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for multiple strategies.
 */
public class StrategiesConfig {

	private List<StrategyConfig> strategies = new ArrayList<StrategyConfig>(5);
	
	public void addStrategy(StrategyConfig strategyConfig) {
		strategies.add(strategyConfig);
	}
	
	public List<StrategyConfig> activeStrategies() 
	{
		List<StrategyConfig> activeStrategies = new ArrayList<StrategyConfig>();
		for (StrategyConfig strategy : strategies)  if (strategy.isActive()) activeStrategies.add(strategy);
		return activeStrategies;
	}
}
