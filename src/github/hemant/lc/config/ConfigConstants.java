/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc.config;

import github.hemant.lc.Noninstantiable;

/**
 * Constants used in the configuration file.
 */
public class ConfigConstants extends Noninstantiable {

	public static final String INVESTOR_ID = "InvestorId";
	public static final String API_KEY = "ApiKey";
	public static final String USE_RECENTLY_LISTED_LOANS = "onlyRecentlyListed";
	public static final String LOANS_FILTER = "loansFilter";
	public static final String TEST_MODE = "testMode";
	public static final String ACCOUNT_NUMBER = "accountNumber";
	public static final String LOG_FILE = "logFile";
	public static final String LOG_LEVEL = "logLevel";
	public static final String SHOW_POPULAR_LOANS = "showPopularLoans";
	public static final String TRACK_FILE = "trackFile";
	public static final String RETRY_COUNT = "retryCount";
	public static final String RETRY_GAP = "retryGap";
	public static final String FOLLOW_POPULAR = "followPopular";
	public static final String MIN_POPULARITY_INDEX = "minPopularityIndex";
	public static final String MIN_PERCENTAGE_FUNDED = "minPercentageFunded";
	public static final String AVG_FUNDING_PER_PERSON = "averageFundingPerPerson";
	public static final String SCHEDULE = "schedule";
	
	////////////////////////////////////////////////////////////////////////////
	// Strategy constants
	////////////////////////////////////////////////////////////////////////////
	public static final String STRATEGIES = "strategies";
	public static final String STRATEGY_NAME = "name";
	public static final String STRATEGY_ACTIVE = "active";
	public static final String STRATEGY_AMOUNT_PER_NOTE = "amountPerNote";
	public static final String STRATEGY_MAX_ORDERS_PER_DAY = "maxOrdersPerDay";
	public static final String STRATEGY_TARGET_PORTFOLIO_ID = "targetPortfolio";
	public static final String STRATEGY_INCLUDE_FILE = "includeFile";
}
