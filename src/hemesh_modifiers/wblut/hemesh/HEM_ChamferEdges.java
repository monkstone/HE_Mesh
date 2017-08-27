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

import java.util.ArrayList;
import java.util.Iterator;

import wblut.geom.WB_Classification;
import wblut.geom.WB_Plane;
import wblut.geom.WB_Point;
import wblut.geom.WB_Vector;

/**
 * Chamfer all convex edges.
 *
 * @author Frederik Vanhoutte (W:Blut)
 *
 */
public class HEM_ChamferEdges extends HEM_Modifier {
	/** Chamfer distance. */
	private double distance;

	/**
	 * Instantiates a new HEM_ChamferEdges.
	 */
	public HEM_ChamferEdges() {
		super();
		distance = 0;
	}

	/**
	 * Set chamfer distance along edge normals.
	 *
	 * @param d
	 *            distance
	 * @return self
	 */
	public HEM_ChamferEdges setDistance(final double d) {
		distance = d;
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.hemesh.modifiers.HEB_Modifier#modify(wblut.hemesh.HE_Mesh)
	 */
	@Override
	protected HE_Mesh applySelf(final HE_Mesh mesh) {
		if (distance == 0) {
			return mesh;
		}
		final ArrayList<WB_Plane> cutPlanes = new ArrayList<WB_Plane>();
		final Iterator<HE_Halfedge> eItr = mesh.eItr();
		HE_Halfedge e;
		while (eItr.hasNext()) {
			e = eItr.next();
			if (e.getVertex().getVertexType() == WB_Classification.CONVEX
					|| e.getEndVertex().getVertexType() == WB_Classification.CONVEX) {
				final WB_Vector N = new WB_Vector(e.getEdgeNormal());
				final WB_Point O = new WB_Point(N).mulSelf(-distance);
				N.mulSelf(-1);
				O.addSelf(e.getHalfedgeCenter());
				final WB_Plane P = new WB_Plane(O, N);
				cutPlanes.add(P);
			}
		}
		final HEM_MultiSlice msm = new HEM_MultiSlice();
		msm.setPlanes(cutPlanes).setSimpleCap(true);
		mesh.modify(msm);
		return mesh;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.hemesh.modifiers.HEB_Modifier#modifySelected(wblut.hemesh.HE_Mesh)
	 */
	@Override
	protected HE_Mesh applySelf(final HE_Selection selection) {
		if (distance == 0) {
			return selection.parent;
		}
		final ArrayList<WB_Plane> cutPlanes = new ArrayList<WB_Plane>();
		selection.collectEdgesByFace();
		final Iterator<HE_Halfedge> eItr = selection.parent.eItr();
		HE_Halfedge e;
		while (eItr.hasNext()) {
			e = eItr.next();
			if (e.getVertex().getVertexType() == WB_Classification.CONVEX
					|| e.getEndVertex().getVertexType() == WB_Classification.CONVEX) {
				final WB_Vector N = new WB_Vector(e.getEdgeNormal());
				final WB_Point O = new WB_Point(N).mulSelf(-distance);
				N.mulSelf(-1);
				O.addSelf(e.getHalfedgeCenter());
				final WB_Plane P = new WB_Plane(O, N);
				cutPlanes.add(P);
			}
		}
		final HEM_MultiSlice msm = new HEM_MultiSlice();
		msm.setPlanes(cutPlanes).setSimpleCap(true);
		selection.parent.modify(msm);
		return selection.parent;
	}
}