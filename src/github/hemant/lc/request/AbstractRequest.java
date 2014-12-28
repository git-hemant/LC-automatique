/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc.request;

import org.apache.http.client.fluent.Request;

/**
 * All REST requests will be sub-classes of this class.
 */
public class AbstractRequest {

	protected static final String BASE_URL = "https://api.lendingclub.com/api/investor/v1/";

	protected static enum Type {
		ACCOUNT, LOANS, ACCOUNT_POST
	};

	private char[] investorId;
	private char[] apiKey;
	
	public AbstractRequest(char[] investorId, char[] apiKey) {
		this.investorId = investorId;
		this.apiKey = apiKey;
	}
	
	protected String getUrl(String reqSuffix, Type type) {
		String url = BASE_URL;
		if (type == Type.ACCOUNT || type == Type.ACCOUNT_POST) {
			url += "accounts/" + new String(investorId) + "/";
		} else if (type == Type.LOANS) {
			url += "loans/";
		}
		return url + reqSuffix;
	}
	
	protected char[] investorId() {
		return investorId;
	}

	protected Request prepareRequest(String reqSuffix, Type type) {
		Request request;
		if (type == Type.ACCOUNT_POST) {
			request = Request.Post(getUrl(reqSuffix, type));
		} else {
			request = Request.Get(getUrl(reqSuffix, type));
		}
		request.addHeader("Authorization", new String(apiKey));
		// All requests we are making would be of type json.
		request.addHeader("Content-Type", "application/json");
		return request;
	}
	
	
}
