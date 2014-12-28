/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc.config;

import github.hemant.lc.Noninstantiable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;

import com.json.parsers.JSONParser;

/**
 * Loader for the strategy, this is responsible for parsing json of strategy
 * into StrategyConfig.
 */
class StrategyConfigLoader extends Noninstantiable {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static StrategyConfig load(JSONParser parser, File configFile, Map<?, ?> map) throws ConfigLoadException
	{
		// First check if we have some input file to be added in the original strategy map.
		Iterator<?> keys = map.keySet().iterator();
		while (keys.hasNext()) {
			String stKey = (String) keys.next();
			if (ConfigConstants.STRATEGY_INCLUDE_FILE.equals(stKey)) {
				String inputFile = (String) map.get(stKey);
				File fileInputFile = new File(configFile.getParent(), inputFile);
				FileInputStream inputStream = null;
				try {
					inputStream = new FileInputStream(fileInputFile);
					Map inputFileMap = parser.parseJson(inputStream, "UTF-8");
					map.putAll(inputFileMap);
				} catch (IOException e) {
					// Ideally this should never happen as we have tested we can read the file earlier.
					throw new ConfigLoadException(e.getMessage());
				}
			}
		}
				
		StrategyConfig sc = new StrategyConfig();
		sc.setName(map.get(ConfigConstants.STRATEGY_NAME).toString());
		sc.setActive(Boolean.parseBoolean(map.get(ConfigConstants.STRATEGY_ACTIVE).toString()));
		sc.setAmountPerNote(Integer.parseInt(map.get(ConfigConstants.STRATEGY_AMOUNT_PER_NOTE).toString()));
		sc.setMaxOrdersPerDay(Integer.parseInt(map.get(ConfigConstants.STRATEGY_MAX_ORDERS_PER_DAY).toString()));
		sc.setTargetPortfolioName((String) map.get(ConfigConstants.STRATEGY_TARGET_PORTFOLIO_ID));
		sc.setLoansDataFilter((Map<String, Object>)map.get(ConfigConstants.LOANS_FILTER));
		Map<?, ?> popMap = (Map<?, ?>) map.get(ConfigConstants.FOLLOW_POPULAR);
		if (popMap != null) {
			StrategyPopularityConfig popConfig = new StrategyPopularityConfig();
			if (popMap.get(ConfigConstants.MIN_POPULARITY_INDEX) != null)
				popConfig.setMinPopularityIndex(new BigDecimal((String)popMap.get(ConfigConstants.MIN_POPULARITY_INDEX)));
			else
				popConfig.setMinPopularityIndex(new BigDecimal(0));
			popConfig.setMinPercentageFunded(Double.parseDouble((String) popMap.get(ConfigConstants.MIN_PERCENTAGE_FUNDED)));
			if (popMap.get(ConfigConstants.AVG_FUNDING_PER_PERSON) != null) {
				int avgFunding = Integer.valueOf((String) popMap.get(ConfigConstants.AVG_FUNDING_PER_PERSON));
				if (avgFunding < 25) {
					throw new ConfigLoadException(ConfigConstants.AVG_FUNDING_PER_PERSON + " cant be less than 25.");
				}
				popConfig.setAverageFundingPerPerson(avgFunding);
			}
			sc.setLoansPopularityConfig(popConfig);
		}
		
		return sc;
	}
}
