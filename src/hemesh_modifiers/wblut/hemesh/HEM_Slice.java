/*
 *
 */
package wblut.hemesh;

import java.util.Iterator;
import java.util.List;

import wblut.core.WB_ProgressCounter;
import wblut.geom.WB_Classification;
import wblut.geom.WB_GeometryOp3D;
import wblut.geom.WB_Plane;

/**
 * Planar cut of a mesh. Faces on positive side of cut plane are removed.
 *
 * @author Frederik Vanhoutte (W:Blut)
 *
 */
public class HEM_Slice extends HEM_Modifier {
	/** Cut plane. */
	private WB_Plane P;
	/**
	 * HEM_slice keeps the part of the mesh on the positive side of the plane.
	 * Reverse planar cut.
	 */
	private boolean reverse = false;
	/**
	 * Cap holes?. Capping holes does not work properly with
	 * self-intersection...
	 */
	private boolean capHoles = true;
	/** The simple cap. */
	private boolean simpleCap = true;

	private boolean optimizeCap = false;

	/** Store cut faces. */
	public HE_Selection cutFaces;
	/** Store cap faces. */
	public HE_Selection capFaces;
	/** The offset. */
	private double offset;
	/**
	 *
	 */
	HEM_SliceSurface ss;

	/**
	 * Set offset.
	 *
	 * @param d
	 *            offset
	 * @return self
	 */
	public HEM_Slice setOffset(final double d) {
		offset = d;
		return this;
	}

	/**
	 * Instantiates a new HEM_Slice.
	 */
	public HEM_Slice() {
		super();
	}

	/**
	 * Set cut plane.
	 *
	 * @param P
	 *            cut plane
	 * @return self
	 */
	public HEM_Slice setPlane(final WB_Plane P) {
		this.P = P;
		return this;
	}

	/**
	 * Sets the plane.
	 *
	 * @param ox
	 *            the ox
	 * @param oy
	 *            the oy
	 * @param oz
	 *            the oz
	 * @param nx
	 *            the nx
	 * @param ny
	 *            the ny
	 * @param nz
	 *            the nz
	 * @return the hE m_ slice
	 */
	public HEM_Slice setPlane(final double ox, final double oy, final double oz, final double nx, final double ny,
			final double nz) {
		P = new WB_Plane(ox, oy, oz, nx, ny, nz);
		return this;
	}

	/**
	 * Set reverse option.
	 *
	 * @param b
	 *            true, false
	 * @return self
	 */
	public HEM_Slice setReverse(final Boolean b) {
		reverse = b;
		return this;
	}

	/**
	 * Set option to cap holes.
	 *
	 * @param b
	 *            true, false;
	 * @return self
	 */
	public HEM_Slice setCap(final Boolean b) {
		capHoles = b;
		return this;
	}

	/**
	 * Sets the simple cap.
	 *
	 * @param b
	 *            the b
	 * @return the hE m_ slice
	 */
	public HEM_Slice setSimpleCap(final Boolean b) {
		simpleCap = b;
		return this;
	}

	public HEM_Slice setOptimizeCap(final boolean b) {
		optimizeCap = b;
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.hemesh.HE_Modifier#apply(wblut.hemesh.HE_Mesh)
	 */
	@Override
	protected HE_Mesh applySelf(final HE_Mesh mesh) {
		mesh.isSurface();
		tracker.setStartStatus(this, "Starting HEM_Slice.");
		cutFaces = new HE_Selection(mesh);
		capFaces = new HE_Selection(mesh);
		// no plane defined
		if (P == null) {
			tracker.setStopStatus(this, "No cutplane defined. Exiting HEM_Slice.");
			return mesh;
		}
		// empty mesh
		if (mesh.getNumberOfVertices() == 0) {
			tracker.setStopStatus(this, "Empty mesh. Exiting HEM_Slice.");
			return mesh;
		}
		WB_Plane lP = P.get();
		if (reverse) {
			lP.flipNormal();
		}
		lP = new WB_Plane(lP.getNormal(), -lP.d() + offset);
		ss = new HEM_SliceSurface().setPlane(lP);
		mesh.modify(ss);
		cutFaces = ss.cutFaces;
		final HE_Selection newFaces = new HE_Selection(mesh);
		HE_Face face;
		WB_ProgressCounter counter = new WB_ProgressCounter(mesh.getNumberOfFaces(), 10);
		tracker.setCounterStatus(this, "Classifying faces.", counter);
		final Iterator<HE_Face> fItr = mesh.fItr();
		while (fItr.hasNext()) {
			face = fItr.next();
			final WB_Classification cptp = WB_GeometryOp3D.classifyPointToPlane3D(face.getFaceCenter(), lP);
			if (cptp == WB_Classification.FRONT || cptp == WB_Classification.ON) {
				if (face.isDegenerate()) {

				}
				newFaces.add(face);

			} else {
				if (cutFaces.contains(face)) {
					cutFaces.remove(face);
				}
			}
			counter.increment();
		}
		tracker.setDuringStatus(this, "Removing unwanted faces.");
		mesh.replaceFaces(newFaces.getFacesAsArray());
		cutFaces.cleanSelection();
		mesh.cleanUnusedElementsByFace();
		if (capHoles) {
			tracker.setDuringStatus(this, "Capping holes.");
			if (simpleCap) {
				HEM_CapHoles ch = new HEM_CapHoles();
				mesh.modify(ch);
				capFaces.addFaces(ch.caps);
			} else {
				final List<HE_Path> cutpaths = ss.getPaths();
				if (cutpaths.size() == 1) {
					HEM_CapHoles ch = new HEM_CapHoles();
					mesh.modify(ch);
					capFaces.addFaces(ch.caps);

				} else {
					tracker.setDuringStatus(this, "Triangulating cut paths.");
					final long[] triKeys = HET_PlanarPathTriangulator.getTriangleKeys(cutpaths, lP);
					HE_Face tri = null;
					HE_Vertex v0, v1, v2;
					HE_Halfedge he0, he1, he2;
					for (int i = 0; i < triKeys.length; i += 3) {
						tri = new HE_Face();
						v0 = mesh.getVertexWithKey(triKeys[i]);
						v1 = mesh.getVertexWithKey(triKeys[i + 1]);
						v2 = mesh.getVertexWithKey(triKeys[i + 2]);
						he0 = new HE_Halfedge();
						he1 = new HE_Halfedge();
						he2 = new HE_Halfedge();
						mesh.setHalfedge(tri, he0);
						mesh.setVertex(he0, v0);
						mesh.setVertex(he1, v1);
						mesh.setVertex(he2, v2);
						mesh.setNext(he0, he1);
						mesh.setNext(he1, he2);
						mesh.setNext(he2, he0);
						mesh.setFace(he0, tri);
						mesh.setFace(he1, tri);
						mesh.setFace(he2, tri);
						capFaces.add(tri);
						mesh.add(tri);
						mesh.add(he0);
						mesh.add(he1);
						mesh.add(he2);
					}

				}
			}
		}
		mesh.pairHalfedges();
		mesh.capHalfedges();
		if (optimizeCap) {
			HET_MeshOp.improveTriangulation(mesh, capFaces);

		}
		tracker.setStopStatus(this, "Ending HEM_Slice.");
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
		return applySelf(selection.parent);
	}

	public static void main(final String[] args) {
		HEC_Torus creator = new HEC_Torus(80, 200, 6, 16);
		HE_Mesh mesh = new HE_Mesh(creator);

		HEM_Slice modifier = new HEM_Slice();

		WB_Plane P = new WB_Plane(0, 0, 0, 0, 0, 1);
		modifier.setPlane(P);
		modifier.setOffset(0);
		modifier.setCap(true);

		modifier.setReverse(false);
		modifier.setSimpleCap(false);
		mesh.modify(modifier);

	}
}
