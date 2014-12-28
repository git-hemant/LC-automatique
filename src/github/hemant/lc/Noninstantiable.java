/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc;

/**
 * This class should be extended by all the classes which provide static methods
 * and doesn't need to be instantiated.
 * 
 * @author Hemant
 */
public class Noninstantiable {

	public Noninstantiable() {
		throw new RuntimeException("Invalid usage - this class can't be instantiated.");
	}
}
