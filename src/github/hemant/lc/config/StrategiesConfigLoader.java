/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc.config;

import github.hemant.lc.Noninstantiable;

import java.io.File;
import java.util.Map;

import com.json.parsers.JSONParser;

/**
 * Loader for the strategy.
 */
class StrategiesConfigLoader extends Noninstantiable {
	
	public static StrategiesConfig load(JSONParser parser, File configFile, Map<?, ?> map) throws ConfigLoadException {
		StrategiesConfig sc = new StrategiesConfig();
		for (int i = 1;; i++) {
			Map<?, ?> strategyMap = (Map<?, ?>) map.get(String.valueOf(i));
			// We have loaded all the strategies
			if (strategyMap == null)
				break;
			sc.addStrategy(StrategyConfigLoader.load(parser, configFile, strategyMap));
		}
		return sc;
	}
}
