/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc.config;

/**
 * Exception which is thrown when the configuration file is not valid or any
 * other issue in reading the configuration file.
 */
public class ConfigLoadException extends Exception {

	private static final long serialVersionUID = 1L;

	public ConfigLoadException(String msg) {
		super(msg);
	}
	
	public ConfigLoadException(Exception source) {
		super(source);
	}
}
