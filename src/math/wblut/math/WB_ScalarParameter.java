/*
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package wblut.math;

/**
 *
 *
 *
 */
public interface WB_ScalarParameter {

	public static final WB_ScalarParameter ZERO = new WB_ConstantScalarParameter(0.0);

	/**
	 *
	 *
	 * @param x
	 * @return
	 */
	public double evaluate(double... x);
}
