/*
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package wblut.hemesh;

/**
 * @author FVH
 *
 */
public class HET_Factory {
	/**
	 *
	 */
	protected HET_Factory() {

	}

	/**
	 *
	 */
	private static final HET_Factory factory = new HET_Factory();

	/**
	 *
	 *
	 * @return
	 */
	public static HET_Factory instance() {
		return factory;
	}

}
