/*
 * HE_Mesh  Frederik Vanhoutte - www.wblut.com
 * 
 * https://github.com/wblut/HE_Mesh
 * A Processing/Java library for for creating and manipulating polygonal meshes.
 * 
 * Public Domain: http://creativecommons.org/publicdomain/zero/1.0/
 */

package wblut.math;

/**
 * @author FVH
 *
 *         Penning easing functions, straight translation of the umpteen
 *         implementations out there
 *
 *         http://robertpenner.com/easing/
 *
 *         TERMS OF USE - EASING EQUATIONS
 *
 *         Open source under the BSD License.
 *
 *         Copyright © 2001 Robert Penner All rights reserved.
 *
 *         Redistribution and use in source and binary forms, with or without
 *         modification, are permitted provided that the following conditions
 *         are met:
 *
 *         Redistributions of source code must retain the above copyright
 *         notice, this list of conditions and the following disclaimer.
 *         Redistributions in binary form must reproduce the above copyright
 *         notice, this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *         Neither the name of the author nor the names of contributors may be
 *         used to endorse or promote products derived from this software
 *         without specific prior written permission. THIS SOFTWARE IS PROVIDED
 *         BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 *         IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *         WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *         ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 *         BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 *         OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 *         OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *         BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 *         WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 *         OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 *         EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */
public class WB_Ease {

	private static final double pmn = 1.70158; // Penner’s Magic Number.

	public static enum EaseType {
		IN, OUT, INOUT
	};// 0= in, 1=out, 2=inout

	public static interface Ease {
		public double ease(EaseType type, double t, double... parameters);

		public double easeIn(double t, double... parameters);

		public double easeOut(double t, double... parameters);

		public double easeInOut(double t, double... parameters);

		public double ease(EaseType type, double t);

		public double easeIn(double t);

		public double easeOut(double t);

		public double easeInOut(double t);
	}

	public static Ease linear = new EaseLinear();
	public static Ease quad = new EaseQuad();
	public static Ease cubic = new EaseCubic();
	public static Ease quart = new EaseQuart();
	public static Ease quint = new EaseQuint();
	public static Ease sine = new EaseSine();
	public static Ease expo = new EaseExpo();
	public static Ease circ = new EaseCirc();
	public static Ease back = new EaseBack();
	public static Ease bounce = new EaseBounce();
	public static Ease elastic = new EaseElastic();

	// simple linear tweening - no easing, no acceleration

	static abstract class EaseAbstract implements Ease {

		@Override
		public double ease(final EaseType type, final double t, final double... params) {
			switch (type) {
			case IN:
				return easeIn(t, params);
			case OUT:
				return easeOut(t, params);
			default:
				return easeInOut(t, params);

			}
		};

		@Override
		public double ease(final EaseType type, final double t) {
			switch (type) {
			case IN:
				return easeIn(t);
			case OUT:
				return easeOut(t);
			default:
				return easeInOut(t);

			}
		};

	}

	public static class EaseLinear extends EaseAbstract {

		@Override
		public double easeIn(final double t, final double... params) {
			return params[1] * t / params[2] + params[20];
		};

		@Override
		public double easeOut(final double t, final double... params) {
			return params[1] * t / params[2] + params[0];
		};

		@Override
		public double easeInOut(final double t, final double... params) {
			return params[1] * t / params[2] + params[0];
		};

		@Override
		public double easeIn(final double t) {
			return t;
		};

		@Override
		public double easeOut(final double t) {
			return t;
		};

		@Override
		public double easeInOut(final double t) {
			return t;
		};
	}

	public static class EaseQuad extends EaseAbstract {
		@Override
		public double easeIn(double t, final double... params) {
			t /= params[2];
			return params[1] * t * t + params[0];
		};

		@Override
		public double easeIn(final double t) {
			return t * t;
		};

		// quadratic easing out - decelerating to zero velocity

		@Override
		public double easeOut(double t, final double... params) {
			t /= params[2];
			return -params[1] * t * (t - 2) + params[0];
		};

		@Override
		public double easeOut(final double t) {
			return -t * (t - 2);
		};

		// quadratic easing in/out - acceleration until halfway, then
		// deceleration

		@Override
		public double easeInOut(double t, final double... params) {
			t /= 0.5 * params[2];
			if (t < 1.0) {
				return params[1] * 0.5 * t * t + params[0];
			}
			t--;
			return -params[1] * 0.5 * (t * (t - 2) - 1.0) + params[0];
		};

		@Override
		public double easeInOut(double t) {
			t /= 0.5;
			if (t < 1.0) {
				return 0.5 * t * t;
			}
			t--;
			return -0.5 * (t * (t - 2) - 1.0);
		};

	}

	public static class EaseCubic extends EaseAbstract {
		@Override
		public double easeIn(double t, final double... params) {
			t /= params[2];
			return params[1] * t * t * t + params[0];
		};

		@Override
		public double easeIn(final double t) {
			return t * t * t;
		};

		// cubic easing3 out - decelerating to zero velocity

		@Override
		public double easeOut(double t, final double... params) {
			t /= params[2];
			t--;
			return params[1] * (t * t * t + 1.0) + params[0];
		};

		@Override
		public double easeOut(double t) {
			t--;
			return t * t * t + 1.0;
		};

		// cubic easing in/out - acceleration until halfway, then deceleration

		@Override
		public double easeInOut(double t, final double... params) {
			t /= 0.5 * params[2];
			if (t < 1.0) {
				return params[1] * 0.5 * t * t * t + params[0];
			}
			t -= 2;
			return params[1] * 0.5 * (t * t * t + 2) + params[0];
		};

		@Override
		public double easeInOut(double t) {
			t /= 0.5;
			if (t < 1.0) {
				return 0.5 * t * t * t;
			}
			t -= 2;
			return 0.5 * (t * t * t + 2);
		};
	}

	public static class EaseQuart extends EaseAbstract {

		@Override
		public double easeIn(double t, final double... params) {
			t /= params[2];
			return params[1] * t * t * t * t + params[0];
		};

		@Override
		public double easeIn(final double t) {
			return t * t * t * t;
		};

		@Override
		public double easeOut(double t, final double... params) {
			t /= params[2];
			t--;
			return -params[1] * (t * t * t * t - 1.0) + params[0];
		};

		@Override
		public double easeOut(double t) {
			t--;
			return -(t * t * t * t - 1.0);
		};

		@Override
		public double easeInOut(double t, final double... params) {
			t /= 0.5 * params[2];
			if (t < 1.0) {
				return params[1] * 0.5 * t * t * t * t + params[0];
			}
			t -= 2;
			return -params[1] * 0.5 * (t * t * t * t - 2) + params[0];
		};

		@Override
		public double easeInOut(double t) {
			t /= 0.5;
			if (t < 1.0) {
				return 0.5 * t * t * t * t;
			}
			t -= 2;
			return -0.5 * (t * t * t * t - 2);
		};
	}

	public static class EaseQuint extends EaseAbstract {

		@Override
		public double easeIn(double t, final double... params) {
			t /= params[2];
			return params[1] * t * t * t * t * t + params[0];
		};

		@Override
		public double easeIn(final double t) {
			return t * t * t * t * t;
		};

		@Override
		public double easeOut(double t, final double... params) {
			t /= params[2];
			t--;
			return params[1] * (t * t * t * t * t + 1.0) + params[0];
		};

		@Override
		public double easeOut(double t) {
			t--;
			return t * t * t * t * t + 1.0;
		};

		@Override
		public double easeInOut(double t, final double... params) {
			t /= 0.5 * params[2];
			if (t < 1.0) {
				return params[1] * 0.5 * t * t * t * t * t + params[0];
			}
			t -= 2;
			return params[1] * 0.5 * (t * t * t * t * t + 2) + params[0];
		};

		@Override
		public double easeInOut(double t) {
			t /= 0.5;
			if (t < 1.0) {
				return 0.5 * t * t * t * t * t;
			}
			t -= 2;
			return 0.5 * (t * t * t * t * t + 2);
		};
	}

	public static class EaseSine extends EaseAbstract {

		@Override
		public double easeIn(final double t, final double... params) {
			return -params[1] * Math.cos(t / params[2] * (Math.PI * 0.5)) + params[1] + params[0];
		};

		@Override
		public double easeIn(final double t) {
			return -Math.cos(t * (Math.PI * 0.5)) + 1.0;
		};

		@Override
		public double easeOut(final double t, final double... params) {
			return params[1] * Math.sin(t / params[2] * (Math.PI * 0.5)) + params[0];
		};

		@Override
		public double easeOut(final double t) {
			return Math.sin(t * (Math.PI * 0.5));
		};

		@Override
		public double easeInOut(final double t, final double... params) {
			return -params[1] * 0.5 * (Math.cos(Math.PI * t / params[2]) - 1.0) + params[0];
		};

		@Override
		public double easeInOut(final double t) {
			return -0.5 * (Math.cos(Math.PI * t) - 1.0);
		};
	}

	public static class EaseExpo extends EaseAbstract {

		@Override
		public double easeIn(final double t, final double... params) {
			return params[1] * Math.pow(2, 10 * (t / params[2] - 1.0)) + params[0];
		};

		@Override
		public double easeIn(final double t) {
			return Math.pow(2, 10 * (t - 1.0));
		};

		@Override
		public double easeOut(final double t, final double... params) {
			return params[1] * (-Math.pow(2, -10 * t / params[2]) + 1.0) + params[0];
		};

		@Override
		public double easeOut(final double t) {
			return -Math.pow(2, -10 * t) + 1.0;
		};

		@Override
		public double easeInOut(double t, final double... params) {
			t /= 0.5 * params[2];
			if (t < 1.0) {
				return params[1] * 0.5 * Math.pow(2, 10 * (t - 1.0)) + params[0];
			}
			t--;
			return params[1] * 0.5 * (-Math.pow(2, -10 * t) + 2) + params[0];
		};

		@Override
		public double easeInOut(double t) {
			t /= 0.5;
			if (t < 1.0) {
				return 0.5 * Math.pow(2, 10 * (t - 1.0));
			}
			t--;
			return 0.5 * (-Math.pow(2, -10 * t) + 2);
		};
	}

	public static class EaseCirc extends EaseAbstract {

		@Override
		public double easeIn(double t, final double... params) {
			t /= params[2];
			return -params[1] * (Math.sqrt(1.0 - t * t) - 1.0) + params[0];
		};

		@Override
		public double easeIn(final double t) {
			return -(Math.sqrt(1.0 - t * t) - 1.0);
		};

		@Override
		public double easeOut(double t, final double... params) {
			t /= params[2];
			t--;
			return params[1] * Math.sqrt(1.0 - t * t) + params[0];
		};

		@Override
		public double easeOut(double t) {
			t--;
			return Math.sqrt(1.0 - t * t);
		};

		@Override
		public double easeInOut(double t, final double... params) {
			t /= 0.5 * params[2];
			if (t < 1.0) {
				return -params[1] * 0.5 * (Math.sqrt(1.0 - t * t) - 1.0) + params[0];
			}
			t -= 2;
			return params[1] * 0.5 * (Math.sqrt(1.0 - t * t) + 1.0) + params[0];
		};

		@Override
		public double easeInOut(double t) {
			t /= 0.5;
			if (t < 1.0) {
				return -0.5 * (Math.sqrt(1.0 - t * t) - 1.0);
			}
			t -= 2;
			return 0.5 * (Math.sqrt(1.0 - t * t) + 1.0);
		};
	}

	public static class EaseBack extends EaseAbstract {
		double s;

		public EaseBack() {
			s = pmn;
		}

		public EaseBack(final double s) {
			this.s = s;
		}

		@Override
		public double easeIn(double t, final double... params) {
			t /= params[2];
			return params[1] * t * t * ((s + 1.0) * t - s) + params[0];
		}

		@Override
		public double easeIn(final double t) {
			return t * t * ((s + 1.0) * t - s);
		}

		@Override
		public double easeOut(double t, final double... params) {
			t /= params[2];
			t--;
			return params[1] * (t * t * ((s + 1.0) * t + s) + 1.0) + params[0];
		}

		@Override
		public double easeOut(double t) {
			t--;
			return t * t * ((s + 1.0) * t + s) + 1.0;
		}

		@Override
		public double easeInOut(double t, final double... params) {
			t /= params[2] * 0.5;
			if (t < 1.0) {
				return params[1] / 2 * (t * t * ((s + 1.0) * t - s)) + params[0];
			}
			t -= 2;
			return params[1] / 2 * (t * t * ((s + 1.0) * t + s) + 2) + params[0];
		}

		@Override
		public double easeInOut(double t) {
			t /= 0.5;
			if (t < 1.0) {
				return 0.5 * (t * t * ((s * 1.525 + 1.0) * t - s * 1.525));
			}
			t -= 2.0;

			return 0.5 * (t * t * ((s * 1.525 + 1.0) * t + s * 1.525) + 2.0);
		}

	}

	public static class EaseBounce extends EaseAbstract {

		@Override
		public double easeIn(final double t, final double... params) {
			return params[1] - easeOut(params[2] - t, 0.0, params[1], params[2]) + params[0];
		}

		@Override
		public double easeIn(final double t) {
			return 1.0 - easeOut(1.0 - t, 0.0, 1.0, 1.0);
		}

		@Override
		public double easeOut(double t, final double... params) {
			t /= params[2];
			if (t < 1.0 / 2.75) {
				return params[1] * (7.5625 * t * t) + params[0];
			} else if (t < 2 / 2.75) {
				return params[1] * (7.5625 * (t -= 1.5 / 2.75) * t + .75) + params[0];
			} else if (t < 2.5 / 2.75) {
				return params[1] * (7.5625 * (t -= 2.25 / 2.75) * t + .9375) + params[0];
			} else {
				return params[1] * (7.5625 * (t -= 2.625 / 2.75) * t + .984375) + params[0];
			}
		}

		@Override
		public double easeOut(double t) {

			if (t < 1.0 / 2.75) {
				return 7.5625 * t * t;
			} else if (t < 2 / 2.75) {
				return 7.5625 * (t -= 1.5 / 2.75) * t + .75;
			} else if (t < 2.5 / 2.75) {
				return 7.5625 * (t -= 2.25 / 2.75) * t + .9375;
			} else {
				return 7.5625 * (t -= 2.625 / 2.75) * t + .984375;
			}
		}

		@Override
		public double easeInOut(final double t, final double... params) {

			if (t < params[2] * 0.5) {
				return easeIn(t * 2.0, 0.0, params[1], params[2]) * 0.5 + params[0];
			} else {
				return easeOut(t * 2.0 - params[2], 0, params[1], params[2]) * 0.5 + params[1] * .5 + params[0];
			}
		}

		@Override
		public double easeInOut(final double t) {

			if (t < 0.5) {
				return easeIn(t * 2.0, 0.0, 1.0, 1.0) * 0.5;
			} else {
				return easeOut(t * 2.0 - 1.0, 0, 1.0, 1.0) * 0.5 + 0.5;
			}
		}
	}

	public static class EaseElastic extends EaseAbstract {

		double a;
		double p;

		public EaseElastic() {
			p = 0.3;
			a = 1.0;

		}

		public EaseElastic(final double a, final double p) {
			this.p = p;
			this.a = a;
		}

		@Override
		public double easeIn(double t, final double... params) {
			double s;
			if (t == 0) {
				return params[0];
			}
			t /= params[2];
			if (t == 1) {
				return params[0] + params[1];
			}
			double la;
			double lp = p * params[2];
			if (a < Math.abs(params[1])) {
				la = params[1];
				s = lp / 4;
			} else {
				la = a;
				s = lp / (2 * Math.PI) * Math.asin(params[1] / a);
			}
			t--;
			return -(la * Math.pow(2, 10 * t) * Math.sin((t * params[2] - s) * (2 * Math.PI) / lp)) + params[0];
		}

		@Override
		public double easeIn(double t) {
			double s;
			if (t == 0) {
				return 0;
			}
			if (t == 1) {
				return 1;
			}
			double la;
			double lp = p;
			if (a < 1) {
				la = 1;
				s = lp / 4;
			} else {
				la = a;
				s = lp / (2 * Math.PI) * Math.asin(1 / a);
			}
			t--;
			return -(la * Math.pow(2, 10 * t) * Math.sin((t - s) * (2 * Math.PI) / lp));
		}

		@Override
		public double easeOut(double t, final double... params) {
			double s;
			if (t == 0) {
				return params[0];
			}
			t /= params[2];
			if (t == 1) {
				return params[0] + params[1];
			}
			double la;
			double lp = p * params[2];
			if (a < Math.abs(params[1])) {
				la = params[1];
				s = lp / 4;
			} else {
				la = a;
				s = lp / (2 * Math.PI) * Math.asin(params[1] / la);
			}
			return la * Math.pow(2, -10 * t) * Math.sin((t * params[2] - s) * (2 * Math.PI) / lp) + params[1]
					+ params[0];
		}

		@Override
		public double easeOut(final double t) {
			double s;
			if (t == 0) {
				return 0;
			}
			if (t == 1) {
				return 1;
			}
			double la;
			double lp = p;
			if (a < 1) {
				la = 1;
				s = lp / 4;
			} else {
				la = a;
				s = lp / (2 * Math.PI) * Math.asin(1 / la);
			}
			return la * Math.pow(2, -10 * t) * Math.sin((t - s) * (2 * Math.PI) / lp) + 1;
		}

		@Override
		public double easeInOut(double t, final double... params) {
			double s;
			if (t == 0) {
				return params[0];
			}
			t /= params[2] * 0.5;
			if (t == 2) {
				return params[0] + params[1];
			}
			double lp = p * params[2] * 1.5;
			double la;
			if (a < Math.abs(params[1])) {
				la = params[1];
				s = lp / 4;
			} else {
				la = a;
				s = lp / (2 * Math.PI) * Math.asin(params[1] / la);
			}
			if (t < 1) {
				t--;
				return -0.5 * (la * Math.pow(2, 10 * t) * Math.sin((t * params[2] - s) * (2 * Math.PI) / lp))
						+ params[0];
			}
			t--;
			return a * Math.pow(2, -10 * t) * Math.sin((t * params[2] - s) * (2 * Math.PI) / lp) * 0.5 + params[1]
					+ params[0];
		}

		@Override
		public double easeInOut(double t) {
			double s;
			if (t == 0) {
				return 0;
			}
			t /= 0.5;
			if (t == 2) {
				return 1;
			}
			double la;
			double lp = p * 1.5;
			if (a < 1) {
				la = 1;
				s = lp / 4;
			} else {
				la = a;
				s = lp / (2 * Math.PI) * Math.asin(1 / la);
			}
			if (t < 1) {
				t--;
				return -0.5 * (la * Math.pow(2, 10 * t) * Math.sin((t - s) * (2 * Math.PI) / lp));
			}
			t--;
			return la * Math.pow(2, -10 * t) * Math.sin((t - s) * (2 * Math.PI) / lp) * 0.5 + 1;
		}
	}

}
