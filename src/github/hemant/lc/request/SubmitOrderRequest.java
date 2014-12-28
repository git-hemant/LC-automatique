/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc.request;

import github.hemant.lc.config.StrategyConfig;
import github.hemant.lc.request.process.LoanUtil;

import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;

import com.json.parsers.JSONParser;
import com.json.parsers.JsonParserFactory;

public class SubmitOrderRequest extends AbstractRequest {

	public SubmitOrderRequest(char[] investorId, char[] apiKey) {
		super(investorId, apiKey);
	}

	public Map submitOrder(StrategyConfig strategyConfig, List<Map> loans) throws WebRequestException {
		Request request = prepareRequest("orders", Type.ACCOUNT_POST);
		String jsonString = createJsonRequestString(strategyConfig, loans);
		request.bodyString(jsonString, ContentType.DEFAULT_TEXT);
		JsonParserFactory factory = JsonParserFactory.getInstance();
		JSONParser parser = factory.newJsonParser();
		try {
			Response response = request.execute();
			HttpResponse httpResponse = response.returnResponse();
			String responseStr = EntityUtils.toString(httpResponse.getEntity());
			return parser.parseJson(responseStr);
		} catch (Exception e) {
			throw new WebRequestException(e);
		}
	}
	
	private String createJsonRequestString(StrategyConfig strategyConfig, List<Map> loans)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("{");sb.append("\"aid\": ");
		sb.append(new String(investorId()));
		sb.append(",");
		sb.append("\"orders\":[");
		int i = 0;
		for (Map loan : loans) {
			if (i++ > 0) {
				sb.append(",");
			}
			sb.append("{");
			sb.append("\"loanId\": ");
			sb.append(LoanUtil.loanId(loan));
			sb.append(",");
			sb.append("\"requestedAmount\": ");
			sb.append(strategyConfig.getAmountPerNote());
			if (strategyConfig.getTargetPortfolioId() != null) {
				sb.append(",");
				sb.append("\"portfolioId\":");
				sb.append(strategyConfig.getTargetPortfolioId());
			}
			sb.append("}");
		}
		sb.append("]}");
		return sb.toString();
	}
}
