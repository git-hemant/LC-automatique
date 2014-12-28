/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc.request;

import java.io.IOException;
import java.util.Map;

import com.json.parsers.JSONParser;
import com.json.parsers.JsonParserFactory;

/**
 * @author Hemant
 */
public class BalanceRequest extends AbstractRequest {
	
	public BalanceRequest(char[] investorId, char[] apiKey) {
		super(investorId, apiKey);
	}
	
	public float availableCash() throws IOException {
		JsonParserFactory factory = JsonParserFactory.getInstance();
		JSONParser parser = factory.newJsonParser();
		Map map = parser.parseJson(prepareRequest("availablecash", Type.ACCOUNT).execute().returnContent().asStream(), "UTF-8");
		return new Float((String) map.get("availableCash")).floatValue();
	}
}
