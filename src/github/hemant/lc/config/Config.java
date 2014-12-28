/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc.config;

import github.hemant.lc.Log;
import github.hemant.lc.request.process.PortfolioIdTracker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.json.parsers.JSONParser;
import com.json.parsers.JsonParserFactory;

/**
 * This class represents the configuration file specified by the user.
 */
public class Config {

	private char[] apiKey;
	private char[] investorId;
	private Map<String, Object> configMap;
	// By default we will keep test mode to true
	// until and unless user specified false.
	private boolean testMode = true;
	private boolean onlyRecentlyListed;
	private String logFile;
	private String trackFile;
	private int retryCount = 1;
	private int retryGap;
	private List<String> cronSchedule;
	private StrategiesConfig strategiesConfig;
	private int logLevel = Log.INFO;
	private int showPopularLoans;
	
	public char[] apiKey() {
		return apiKey;
	}
	
	public char[] investorId() {
		return investorId;
	}
	
	public boolean testMode() {
		return testMode;
	}
	
	public int logLevel() {
		return logLevel;
	}
	
	public int showPopularLoans() {
		return showPopularLoans;
	}
	
	public boolean onlyRecentlyListed() {
		return onlyRecentlyListed;
	}
	
	public String logFile() {
		return logFile;
	}
	
	public String trackFile() {
		return trackFile;
	}
	
	public int retryCount() {
		return retryCount;
	}
	
	public int retryGap() {
		return retryGap;
	}
	
	public Map<String, Object> configMap() {
		return configMap;
	}
	
	public List<String> cronSchedule() {
		return cronSchedule;
	}
	
	public List<StrategyConfig> activeStrategies() {
		return strategiesConfig.activeStrategies();
	}
	
	@SuppressWarnings("unchecked")
	public static Config loadConfig() throws ConfigLoadException {
		String filePath = System.getProperty("config.file");
		if (filePath == null) {
			throw new ConfigLoadException(
					"Please specify the configuration file using argument -Dconfig.file=<filepath>");
			
		}
		File file = new File(filePath);
		if (!file.isFile() && !file.canRead()) {
			throw new ConfigLoadException(
					"Invalid value for system property config.file: "
							+ System.getProperty("config.file")
							+ " .Unable to access file: "
							+ file.getAbsolutePath());
		}
		
		JsonParserFactory factory = JsonParserFactory.getInstance();
		JSONParser parser = factory.newJsonParser();
		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(file);
		} catch (IOException e) {
			// Ideally this should never happen as we have tested we can read the file earlier.
			throw new ConfigLoadException(e.getMessage());
		}
		Config config = new Config();
		try {
			config.configMap = (Map<String, Object>) parser.parseJson(inputStream, "UTF-8");
			config.initialize(parser, file);
		} finally {
			if (inputStream != null)
				try {inputStream.close();} catch (IOException e) {}
		}
		return config;
	}

	private void initialize(JSONParser parser, File file) throws ConfigLoadException {
		apiKey = strValue(configMap, ConfigConstants.API_KEY, true).toCharArray();
		investorId = strValue(configMap, ConfigConstants.INVESTOR_ID, true).toCharArray();
		testMode = Boolean.parseBoolean(configMap.get(ConfigConstants.TEST_MODE).toString());
		onlyRecentlyListed = Boolean.parseBoolean(configMap.get(ConfigConstants.USE_RECENTLY_LISTED_LOANS).toString());
		trackFile = configMap.get(ConfigConstants.TRACK_FILE).toString();
		// Log file is optional
		if (configMap.get(ConfigConstants.LOG_FILE) != null)
			logFile = configMap.get(ConfigConstants.LOG_FILE).toString();
		if (configMap.get(ConfigConstants.LOG_LEVEL) != null)
			logLevel = parseLogLevel(configMap.get(ConfigConstants.LOG_LEVEL).toString());
		
		if (configMap.get(ConfigConstants.SHOW_POPULAR_LOANS) != null)
			showPopularLoans = Integer.parseInt(configMap.get(ConfigConstants.SHOW_POPULAR_LOANS).toString());
		
		
		// Retry is optional
		if (configMap.get(ConfigConstants.RETRY_COUNT) != null)
			retryCount = Integer.parseInt(configMap.get(ConfigConstants.RETRY_COUNT).toString());
		if (configMap.get(ConfigConstants.RETRY_GAP) != null)
			retryGap = Integer.parseInt(configMap.get(ConfigConstants.RETRY_GAP).toString());
		strategiesConfig = StrategiesConfigLoader.load(parser, file, (Map<?, ?>) configMap.get(ConfigConstants.STRATEGIES));
		if (configMap.get(ConfigConstants.SCHEDULE) != null) {
			Map<?, ?> scheduleMap = (Map<?, ?>) configMap.get(ConfigConstants.SCHEDULE);
			cronSchedule = new ArrayList<String>();				
			for (int i = 1;;i++) {
				String sched = (String) scheduleMap.get("" + i);
				if (sched == null) break;
				cronSchedule.add(sched);
			}
		}
		
		// Now based on the portfolio name in strategy we will retrieve portfolio id.
		List<StrategyConfig> strategies = strategiesConfig.activeStrategies();
		for (StrategyConfig strategy : strategies) {
			String portfolioName = strategy.targetPortfolioName();
			if (portfolioName != null && portfolioName.length() > 0) {
				try {
					Long portfolioId = PortfolioIdTracker.INSTANCE.getPortfolioId(portfolioName, this);
					if (portfolioId == null) {
						Log.instance(this).error("Unable to find portfolio '" + portfolioName + "' in your account, please make sure the portfolio name is exactly same and in-correct case, as name is case sensitive.");
						throw new ConfigLoadException("Invalid portfolio name: " + portfolioName);
					}
					strategy.setTargetPortfolioId(portfolioId);
				} catch (IOException e) {
					Log log = Log.instance(this);
					log.error("Unable to retrieve numeric portfolio id for the portfolio name: " + portfolioName);
					log.error("Please make sure you are investor id and API key is correct.");
					throw new ConfigLoadException(e);
				}
			}
		}
	}

	private static String strValue(Map<String, Object> map, String key, boolean exceptionOnEmpty)
			throws ConfigLoadException {
		if (exceptionOnEmpty && !map.containsKey(key)) {
			throw new ConfigLoadException("Please specify value for " + key + " in the config file.");
		}
		return (String) map.get(key);
	}

	private static int parseLogLevel(String string) {
		if (string == null)
			return Log.INFO;
		string = string.toLowerCase();
		if ("debug".equals(string)) {
			return Log.DEBUG;
		} else if ("info".equals(string)) {
			return Log.INFO;
		} else if ("error".equals(string)) {
			return Log.ERROR;
		}
		return Log.INFO;
	}
}
