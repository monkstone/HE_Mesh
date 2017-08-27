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
import java.util.Collection;
import java.util.Iterator;

import wblut.geom.WB_Classification;
import wblut.geom.WB_GeometryOp;
import wblut.geom.WB_GeometryOp3D;
import wblut.geom.WB_Plane;

/**
 * Multiple planar cuts of a mesh. No faces are removed.
 *
 * @author Frederik Vanhoutte (W:Blut)
 *
 */
public class HEM_MultiSliceSurface extends HEM_Modifier {
	/** Cut planes. */
	private ArrayList<WB_Plane> planes;
	/** Store cut faces. */
	public HE_Selection cutFaces;
	/** The new edges. */
	public HE_Selection newEdges;
	/** The offset. */
	private double offset;

	/**
	 * Set offset.
	 *
	 * @param d
	 *            offset
	 * @return self
	 */
	public HEM_MultiSliceSurface setOffset(final double d) {
		offset = d;
		return this;
	}

	/**
	 * Instantiates a new HEM_MultiSlice surface.
	 */
	public HEM_MultiSliceSurface() {
		super();
	}

	/**
	 * Set cut planes from an arrayList of WB_Plane.
	 *
	 * @param planes
	 *            arrayList of WB_Plane
	 * @return self
	 */
	public HEM_MultiSliceSurface setPlanes(final Collection<WB_Plane> planes) {
		this.planes = new ArrayList<WB_Plane>();
		this.planes.addAll(planes);
		return this;
	}

	/**
	 * Set cut planes from an array of WB_Plane.
	 *
	 * @param planes
	 *            array of WB_Plane
	 * @return self
	 */
	public HEM_MultiSliceSurface setPlanes(final WB_Plane[] planes) {
		this.planes = new ArrayList<WB_Plane>();
		for (final WB_Plane plane : planes) {
			this.planes.add(plane);
		}
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.hemesh.HE_Modifier#apply(wblut.hemesh.HE_Mesh)
	 */
	@Override
	protected HE_Mesh applySelf(final HE_Mesh mesh) {
		cutFaces = new HE_Selection(mesh);
		newEdges = new HE_Selection(mesh);
		mesh.resetFaceInternalLabels();
		mesh.resetEdgeInternalLabels();
		if (planes == null) {
			return mesh;
		}
		final HEM_SliceSurface slice = new HEM_SliceSurface();

		boolean unique = true;
		WB_Plane Pi, Pj;
		for (int i = 0; i < planes.size(); i++) {
			Pi = planes.get(i);
			unique = true;
			for (int j = 0; j < i; j++) {
				Pj = planes.get(j);
				if (WB_GeometryOp3D.isEqual(Pi, Pj)) {
					unique = false;
					break;
				}
			}
			if (unique) {

				slice.setPlane(Pi).setOffset(offset);
				slice.applySelf(mesh);
				cutFaces.add(slice.cutFaces);

				newEdges.add(slice.newEdges);

			}
		}
		mesh.resetEdgeInternalLabels();
		cutFaces.cleanSelection();
		cutFaces.collectEdgesByFace();
		final Iterator<HE_Halfedge> eItr = cutFaces.eItr();
		HE_Halfedge he;
		while (eItr.hasNext()) {
			he = eItr.next();
			for (int i = 0; i < planes.size(); i++) {
				if (WB_GeometryOp.classifySegmentToPlane3D(he.getVertex(), he.getEndVertex(),
						planes.get(i)) == WB_Classification.ON) {
					he.setInternalLabel(1);
					newEdges.add(he);
					break;
				}
			}
		}
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
		selection.parent.resetFaceInternalLabels();
		selection.parent.resetEdgeInternalLabels();
		cutFaces = new HE_Selection(selection.parent);
		newEdges = new HE_Selection(selection.parent);
		if (planes == null) {
			return selection.parent;
		}
		final HEM_SliceSurface slice = new HEM_SliceSurface();
		boolean unique = true;
		WB_Plane Pi, Pj;
		for (int i = 0; i < planes.size(); i++) {
			Pi = planes.get(i);
			unique = true;
			for (int j = 0; j < i; j++) {
				Pj = planes.get(j);
				if (WB_GeometryOp3D.isEqual(Pi, Pj)) {
					unique = false;
					break;
				}
			}
			if (unique) {

				slice.setPlane(Pi).setOffset(offset);
				slice.apply(selection);
				cutFaces.add(slice.cutFaces);
			}
		}
		selection.parent.resetEdgeInternalLabels();
		cutFaces.cleanSelection();
		cutFaces.collectEdgesByFace();
		final Iterator<HE_Halfedge> eItr = cutFaces.eItr();
		HE_Halfedge he;
		while (eItr.hasNext()) {
			he = eItr.next();
			for (int i = 0; i < planes.size(); i++) {
				if (WB_GeometryOp.classifySegmentToPlane3D(he.getVertex(), he.getEndVertex(),
						planes.get(i)) == WB_Classification.ON) {
					he.setInternalLabel(1);
					newEdges.add(he);
					break;
				}
			}
		}
		return selection.parent;
	}
}
