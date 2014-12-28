/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc.request;

public class WebRequestException extends Exception {
	
	private static final long serialVersionUID = 3844749038259416285L;

	public WebRequestException(Throwable t) {
		super(t);
	}
	
	public WebRequestException(String msg, Throwable t) {
		super(msg, t);
	}
}
