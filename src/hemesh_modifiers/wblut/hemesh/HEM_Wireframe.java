/*
 * This file is part of HE_Mesh, a library for creating and manipulating meshes.
 * It is dedicated to the public domain. To the extent possible under law,
 * I , Frederik Vanhoutte, have waived all copyright and related or neighboring
 * rights.
 *
 * This work is published from Belgium. (http://creativecommons.org/publicdomain/zero/1.0/)
 *
 */
package wblut.hemesh;

import wblut.math.WB_ConstantScalarParameter;
import wblut.math.WB_ScalarParameter;

/**
 *
 */
public class HEM_Wireframe extends HEM_Modifier {
	/**
	 *
	 */
	private WB_ScalarParameter strutR;
	/**
	 *
	 */
	private WB_ScalarParameter maxStrutOffset;
	/**
	 *
	 */
	private int facetN;
	/**
	 *
	 */
	private WB_ScalarParameter angleFactor;
	/**
	 *
	 */
	private double fillFactor;
	/**
	 *
	 */
	private double fidget;
	/**
	 *
	 */
	private boolean cap;
	/**
	 *
	 */
	private boolean taper;

	/**
	 *
	 */
	public HEM_Wireframe() {
		facetN = 4;
		angleFactor = new WB_ConstantScalarParameter(0.5);
		fidget = 1.0001;
		fillFactor = 0.99;
		maxStrutOffset = new WB_ConstantScalarParameter(Double.MAX_VALUE);
		cap = true;
		taper = false;
	}

	/**
	 *
	 *
	 * @param r
	 * @return
	 */
	public HEM_Wireframe setStrutRadius(final double r) {
		strutR = new WB_ConstantScalarParameter(r);
		return this;
	}

	/**
	 *
	 *
	 * @param r
	 * @return
	 */
	public HEM_Wireframe setStrutRadius(final WB_ScalarParameter r) {
		strutR = r;
		return this;
	}

	/**
	 *
	 *
	 * @param r
	 * @return
	 */
	public HEM_Wireframe setMaximumStrutOffset(final double r) {
		maxStrutOffset = new WB_ConstantScalarParameter(r);
		return this;
	}

	/**
	 *
	 *
	 * @param r
	 * @return
	 */
	public HEM_Wireframe setMaximumStrutOffset(final WB_ScalarParameter r) {
		maxStrutOffset = r;
		return this;
	}

	/**
	 *
	 *
	 * @param N
	 * @return
	 */
	public HEM_Wireframe setStrutFacets(final int N) {
		facetN = N;
		return this;
	}

	/**
	 *
	 *
	 * @param af
	 * @return
	 */
	public HEM_Wireframe setAngleOffset(final double af) {
		angleFactor = new WB_ConstantScalarParameter(af);
		return this;
	}

	/**
	 *
	 *
	 * @param af
	 * @return
	 */
	public HEM_Wireframe setAngleOffset(final WB_ScalarParameter af) {
		angleFactor = af;
		return this;
	}

	/**
	 *
	 *
	 * @param f
	 * @return
	 */
	public HEM_Wireframe setFidget(final double f) {
		fidget = f;
		return this;
	}

	/**
	 *
	 *
	 * @param ff
	 * @return
	 */
	public HEM_Wireframe setFillFactor(final double ff) {
		fillFactor = ff;
		return this;
	}

	/**
	 *
	 *
	 * @param b
	 * @return
	 */
	public HEM_Wireframe setCap(final boolean b) {
		cap = b;
		return this;
	}

	/**
	 *
	 *
	 * @param b
	 * @return
	 */
	public HEM_Wireframe setTaper(final boolean b) {
		taper = b;
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.hemesh.creators.HEC_Creator#createBase()
	 */
	@Override
	protected HE_Mesh applySelf(final HE_Mesh mesh) {
		if (strutR == null || facetN < 3) {
			return mesh;
		}
		final HEC_FromFrame ff = new HEC_FromFrame();
		ff.setFrame(mesh);
		ff.setAngleOffset(angleFactor);
		ff.setCap(cap);
		ff.setStrutFacets(facetN);
		ff.setFidget(fidget);
		ff.setFillFactor(fillFactor);
		ff.setTaper(taper);
		ff.setStrutRadius(strutR);
		ff.setMaximumStrutOffset(maxStrutOffset);
		mesh.setNoCopy(ff.create());
		return mesh;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seewblut.hemesh.modifiers.HEM_Modifier#applySelected(wblut.hemesh.core.
	 * HE_Selection)
	 */
	@Override
	protected HE_Mesh applySelf(final HE_Selection selection) {
		return applySelf(selection.parent);
	}
}
