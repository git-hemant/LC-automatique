package github.hemant.lc.request.process;

import github.hemant.lc.config.Config;
import github.hemant.lc.request.PortfoliosOwnedRequest;
import github.hemant.lc.request.PortfoliosOwnedRequest.Portfolio;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to keep in-memory list of portfolio name and there unique
 * numeric id which is required in the order.
 *  
 */
public enum PortfolioIdTracker {
	// Singelton instance.
	INSTANCE;
	
	// Map where key is name of portfolio and value is portfolio id which is unique
	// number assigned to each portfolio.
	private Map<String, Long> portfolioIds = new HashMap<String, Long>();

	public void cachePortfolioIds(Config config) throws IOException {
		// Remove any existing ids from map.
		portfolioIds.clear();
		
		// Now get list of portfolio's from server.
		PortfoliosOwnedRequest req = new PortfoliosOwnedRequest(config.investorId(), config.apiKey());
		Portfolio[] portfolios = req.getPortfolios();
		for (Portfolio portfolio : portfolios) {
			
			// This is simple validation as string name is as unique as id.
			if (portfolioIds.containsKey(portfolio.getName())) {
				throw new RuntimeException("Duplicate portfolio name: " + portfolio.getName());
			}
			portfolioIds.put(portfolio.getName(), portfolio.getId());
		}
	}
	
	public Long getPortfolioId(String portfolioName, Config config) throws IOException {
		Long id = portfolioIds.get(portfolioName);
		// This portfolio might be created by the user later and not cached yet
		// in this case we will refresh our list of portfolio's.
		if (id == null) {
			cachePortfolioIds(config);
			id = portfolioIds.get(portfolioName);
		}
		return id;
	}
}
