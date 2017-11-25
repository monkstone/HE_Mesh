/*
 * HE_Mesh  Frederik Vanhoutte - www.wblut.com
 * 
 * https://github.com/wblut/HE_Mesh
 * A Processing/Java library for for creating and manipulating polygonal meshes.
 * 
 * Public Domain: http://creativecommons.org/publicdomain/zero/1.0/
 */
package wblut.geom;

import wblut.math.WB_ScalarParameter;

/**
 * @author FVH
 *
 */
public interface WB_IsoValues3D {
	public double value(int i, int j, int k);

	public class Grid3D implements WB_IsoValues3D {
		private double[][][] values;

		public Grid3D(final double[][][] values) {
			int lx = values.length;
			int ly = lx == 0 ? 0 : values[0].length;
			int lz = ly == 0 ? 0 : values[0][0].length;
			this.values = new double[lx][ly][lz];
			for (int i = 0; i < values.length; i++) {
				for (int j = 0; j < values[0].length; j++) {
					for (int k = 0; k < values[0].length; k++) {
						this.values[i][j][k] = values[i][j][k];
					}
				}

			}
		}

		public Grid3D(final float[][][] values) {
			int lx = values.length;
			int ly = lx == 0 ? 0 : values[0].length;
			int lz = ly == 0 ? 0 : values[0][0].length;
			this.values = new double[lx][ly][lz];
			for (int i = 0; i < values.length; i++) {
				for (int j = 0; j < values[0].length; j++) {
					for (int k = 0; k < values[0].length; k++) {
						this.values[i][j][k] = values[i][j][k];
					}
				}

			}
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.geom.WB_IsoValues2D#value(int, int, int)
		 */
		@Override
		public double value(final int i, final int j, final int k) {
			return values[i][j][k];
		}

	}

	public class GridRaw3D implements WB_IsoValues3D {
		private double[][][] values;

		public GridRaw3D(final double[][][] values) {
			this.values = values;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.geom.WB_IsoValues3D#value(int, int, int)
		 */
		@Override
		public double value(final int i, final int j, final int k) {
			return values[i][j][k];
		}

	}

	public class Function3D implements WB_IsoValues3D {
		private double fxi, fyi, fzi, dfx, dfy, dfz;
		private WB_ScalarParameter function;

		public Function3D(final WB_ScalarParameter function, final double xi, final double yi, final double zi,
				final double dx, final double dy, final double dz) {
			this.function = function;
			fxi = xi;
			fyi = yi;
			fzi = zi;
			dfx = dx;
			dfy = dy;
			dfz = dz;
			this.function = function;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.geom.WB_IsoValues3D#value(int, int)
		 */
		@Override
		public double value(final int i, final int j, final int k) {
			return function.evaluate(fxi + i * dfx, fyi + j * dfy, fzi + k * dfz);
		}
	}

	public class HashGrid3D implements WB_IsoValues3D {
		private WB_HashGridDouble values;

		public HashGrid3D(final WB_HashGridDouble values) {
			this.values = values;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.geom.WB_IsoValues2D#value(int, int, int)
		 */
		@Override
		public double value(final int i, final int j, final int k) {
			return values.getValue(i, j, k);
		}

	}

}
