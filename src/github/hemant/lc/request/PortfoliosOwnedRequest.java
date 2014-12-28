/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc.request;

import github.hemant.lc.config.Config;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.http.client.fluent.Request;

import com.json.parsers.JSONParser;
import com.json.parsers.JsonParserFactory;

public class PortfoliosOwnedRequest extends AbstractRequest {

	public PortfoliosOwnedRequest(char[] investorId, char[] apiKey) {
		super(investorId, apiKey);
	}
	
	public static void main(String[] args) throws Exception {
		Config config = Config.loadConfig();
		PortfoliosOwnedRequest req = new PortfoliosOwnedRequest(config.investorId(), config.apiKey());
		req.getPortfolios();
	}
	
	public Portfolio[] getPortfolios() throws IOException {
		Request req = prepareRequest("portfolios", Type.ACCOUNT);
		JsonParserFactory factory = JsonParserFactory.getInstance();
		JSONParser parser = factory.newJsonParser();
		Map map = parser.parseJson(req.execute().returnContent().asStream(), "UTF-8");
		List<Map> portfoliosList = (List<Map>) map.get("myPortfolios");
		Portfolio portfolio[] = new Portfolio[portfoliosList.size()];
		for (int i = 0, size = portfoliosList.size(); i < size; i++) {
			Map mapPortfolio = portfoliosList.get(i);
			long id = Long.parseLong((String)mapPortfolio.get("portfolioId"));
			String name = (String) mapPortfolio.get("portfolioName");
			String desc = (String) mapPortfolio.get("portfolioDescription");
			portfolio[i] = new Portfolio(id, name, desc);
		}
		
		return portfolio;
	}

	/**
	 * Wraps the portfolio information
	 */
	public static class Portfolio {
		private long id;
		private String name;
		private String description;
		
		private Portfolio(long id, String name, String description) {
			this.id = id;
			this.name = name;
			this.description = description;
		}

		public long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}
	}
}
