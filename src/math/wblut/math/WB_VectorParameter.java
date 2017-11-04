/*
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package wblut.math;

import wblut.geom.WB_Coord;

/**
 *
 *
 *
 */
public interface WB_VectorParameter {
	/**
	 *
	 *
	 * @param x
	 * @return
	 */
	public WB_Coord evaluate(double... x);
}
