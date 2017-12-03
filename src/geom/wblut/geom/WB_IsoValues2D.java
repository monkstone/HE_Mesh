/*
 * HE_Mesh  Frederik Vanhoutte - www.wblut.com
 *
 * https://github.com/wblut/HE_Mesh
 * A Processing/Java library for for creating and manipulating polygonal meshes.
 *
 * Public Domain: http://creativecommons.org/publicdomain/zero/1.0/
 */
package wblut.geom;

import processing.core.PApplet;
import processing.core.PImage;
import wblut.math.WB_ScalarParameter;

/**
 * @author FVH
 *
 */
public interface WB_IsoValues2D {
	public enum Mode {
		RED, GREEN, BLUE, HUE, SAT, BRI, ALPHA
	};

	public double value(int i, int j);

	public int getWidth();

	public int getHeight();

	public class Grid2D implements WB_IsoValues2D {
		private double[][] values;
		private int width, height;

		public Grid2D(final double[][] values) {
			width = values.length;
			height = values.length > 0 ? values[0].length : 0;
			this.values = new double[width][height];
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					this.values[i][j] = values[i][j];
				}
			}
		}

		public Grid2D(final float[][] values) {
			width = values.length;
			height = values.length > 0 ? values[0].length : 0;
			this.values = new double[width][height];
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					this.values[i][j] = values[i][j];
				}
			}
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.geom.WB_IsoValues2D#value(int, int, int)
		 */
		@Override
		public double value(final int i, final int j) {
			return values[i][j];
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.geom.WB_IsoValues2D#getWidth()
		 */
		@Override
		public int getWidth() {

			return width;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.geom.WB_IsoValues2D#getHeight()
		 */
		@Override
		public int getHeight() {

			return height;
		}

	}

	public class GridRaw2D implements WB_IsoValues2D {
		private double[][] values;
		private int width, height;

		public GridRaw2D(final double[][] values) {
			this.values = values;
			width = values.length;
			height = values.length > 0 ? values[0].length : 0;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.geom.WB_IsoValues2D#value(int, int, int)
		 */
		@Override
		public double value(final int i, final int j) {
			// TODO Auto-generated method stub
			return values[i][j];
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.geom.WB_IsoValues2D#getWidth()
		 */
		@Override
		public int getWidth() {

			return width;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.geom.WB_IsoValues2D#getHeight()
		 */
		@Override
		public int getHeight() {

			return height;
		}

	}

	public class Function2D implements WB_IsoValues2D {
		private double fxi, fyi, dfx, dfy;
		private WB_ScalarParameter function;

		public Function2D(final WB_ScalarParameter function, final double xi, final double yi, final double dx,
				final double dy) {
			this.function = function;
			fxi = xi;
			fyi = yi;
			dfx = dx;
			dfy = dy;
			this.function = function;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.geom.WB_IsoValues2D#value(int, int)
		 */
		@Override
		public double value(final int i, final int j) {
			return function.evaluate(fxi + i * dfx, fyi + j * dfy);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.geom.WB_IsoValues2D#getWidth()
		 */
		@Override
		public int getWidth() {

			return 0;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.geom.WB_IsoValues2D#getHeight()
		 */
		@Override
		public int getHeight() {

			return 0;
		}
	}

	public class HashGrid2D implements WB_IsoValues2D {
		private WB_HashGridDouble2D values;
		private int width, height;

		public HashGrid2D(final WB_HashGridDouble2D values) {
			this.values = values;
			width = values.getWidth();
			height = values.getHeight();
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.geom.WB_IsoValues2D#value(int, int, int)
		 */
		@Override
		public double value(final int i, final int j) {
			return values.getValue(i, j);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.geom.WB_IsoValues2D#getWidth()
		 */
		@Override
		public int getWidth() {

			return width;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.geom.WB_IsoValues2D#getHeight()
		 */
		@Override
		public int getHeight() {

			return height;
		}

	}

	public class ImageGrid2D implements WB_IsoValues2D {
		private PImage image;
		private Mode mode;
		private PApplet home;

		public ImageGrid2D(final String path, final PApplet home, final int width, final int height) {
			this.image = home.loadImage(path);
			image.resize(width, height);
			mode = Mode.BRI;
			this.home = home;

		}

		public ImageGrid2D(final String path, final PApplet home) {
			image = home.loadImage(path);
			mode = Mode.BRI;
			this.home = home;
		}

		public ImageGrid2D(final String path, final PApplet home, final int width, final int height, final Mode mode) {
			this.image = home.loadImage(path);
			image.resize(width, height);
			this.mode = mode;
			this.home = home;

		}

		public ImageGrid2D(final String path, final PApplet home, final Mode mode) {
			image = home.loadImage(path);
			this.mode = mode;
			this.home = home;
		}

		public ImageGrid2D(final PImage image, final PApplet home, final int width, final int height) {
			// PGraphics is a PImage but does not support resize(). Making a
			// copy with get() solves this potential problem.
			this.image = image.get();
			this.image.resize(width, height);
			mode = Mode.BRI;
			this.home = home;

		}

		public ImageGrid2D(final PImage image, final PApplet home) {
			this.image = image;
			mode = Mode.BRI;
			this.home = home;
		}

		public ImageGrid2D(final PImage image, final PApplet home, final int width, final int height, final Mode mode) {
			// PGraphics is a PImage but does not support resize(). Making a
			// copy with get() solves this potential problem.
			this.image = image.get();

			this.image.resize(width, height);
			this.mode = mode;
			this.home = home;

		}

		public ImageGrid2D(final PImage image, final PApplet home, final Mode mode) {
			this.image = image;
			this.mode = mode;
			this.home = home;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.geom.WB_IsoValues2D#value(int, int, int)
		 */
		@Override
		public double value(final int i, final int j) {
			int color = image.get(i, j);
			switch (mode) {
			case BRI:
				return home.brightness(color);
			case HUE:
				return home.hue(color);
			case SAT:
				return home.saturation(color);
			case RED:
				return home.red(color);
			case GREEN:
				return home.green(color);
			case BLUE:
				return home.blue(color);
			case ALPHA:
				return home.alpha(color);
			default:
				return home.brightness(color);
			}

		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.geom.WB_IsoValues2D#getWidth()
		 */
		@Override
		public int getWidth() {

			return image.width;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.geom.WB_IsoValues2D#getHeight()
		 */
		@Override
		public int getHeight() {

			return image.height;
		}

	}

}
