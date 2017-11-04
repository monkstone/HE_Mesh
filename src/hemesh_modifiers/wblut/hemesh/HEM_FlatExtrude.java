/*
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package wblut.hemesh;

import java.util.List;
import java.util.Map;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.primitive.LongDoubleHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;

import wblut.core.WB_ProgressCounter;
import wblut.geom.WB_Classification;
import wblut.geom.WB_Coord;
import wblut.geom.WB_GeometryFactory;
import wblut.geom.WB_GeometryOp3D;
import wblut.geom.WB_IntersectionResult;
import wblut.geom.WB_Point;
import wblut.geom.WB_Polygon;
import wblut.geom.WB_Segment;
import wblut.math.WB_ConstantScalarParameter;
import wblut.math.WB_Epsilon;
import wblut.math.WB_ScalarParameter;

/**
 * Extrudes and scales a face along its face normal.
 *
 * @author Frederik Vanhoutte (W:Blut)
 *
 */
public class HEM_FlatExtrude extends HEM_Modifier {
	/**
	 *
	 */
	private static WB_GeometryFactory gf = new WB_GeometryFactory();

	/** Threshold angle for hard edges. */
	private double thresholdAngle;
	/** Chamfer factor or distance. */
	private WB_ScalarParameter chamfer;
	/** Hard edge chamfer distance. */
	private WB_ScalarParameter hardEdgeChamfer;
	/** Extrusion mode. */
	private boolean relative;

	/** Halfedge normals. */
	private LongObjectHashMap<WB_Coord> _halfedgeNormals;
	/** Extrusion widths. */
	private LongDoubleHashMap _halfedgeEWs;
	/** Face centers. */
	private Map<Long, WB_Coord> _faceCenters;

	private HE_Selection walls;
	private HE_Selection extruded;
	private HE_Selection failed;

	public HEM_FlatExtrude() {
		super();
		thresholdAngle = -1;
		chamfer = new WB_ConstantScalarParameter(0.0);
		hardEdgeChamfer = new WB_ConstantScalarParameter(0.0);
		relative = true;
	}

	public HEM_FlatExtrude setChamfer(final double c) {
		chamfer = WB_Epsilon.isZero(c) ? WB_ScalarParameter.ZERO : new WB_ConstantScalarParameter(c);
		return this;
	}

	public HEM_FlatExtrude setChamfer(final WB_ScalarParameter c) {
		chamfer = c;
		return this;
	}

	public HEM_FlatExtrude setHardEdgeChamfer(final double c) {
		hardEdgeChamfer = WB_Epsilon.isZero(c) ? WB_ScalarParameter.ZERO : new WB_ConstantScalarParameter(c);
		return this;
	}

	public HEM_FlatExtrude setThresholdAngle(final double a) {
		thresholdAngle = a;
		return this;
	}

	public HEM_FlatExtrude setHardEdgeChamfer(final WB_ScalarParameter c) {
		hardEdgeChamfer = c;
		return this;
	}

	/**
	 * Set chamfer mode.
	 *
	 * @param relative
	 *            true/false
	 * @return self
	 */
	public HEM_FlatExtrude setRelative(final boolean relative) {
		this.relative = relative;
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.hemesh.HE_Modifier#apply(wblut.hemesh.HE_Mesh)
	 */
	@Override
	protected HE_Mesh applySelf(final HE_Mesh mesh) {
		tracker.setStartStatus(this, "Starting HEM_FlatExtrude.");
		mesh.resetFaceInternalLabels();
		walls = new HE_Selection(mesh);
		extruded = new HE_Selection(mesh);
		failed = new HE_Selection(mesh);
		if (chamfer == WB_ScalarParameter.ZERO) {
			tracker.setStopStatus(this, "Exiting HEM_FlatExtrude.");
			return mesh;
		}
		HE_Face f;
		HE_Halfedge he;
		WB_Coord c;
		final List<HE_Face> faces = mesh.getFaces();

		_faceCenters = mesh.getKeyedFaceCenters();
		final int nf = faces.size();
		WB_ProgressCounter counter = new WB_ProgressCounter(nf, 10);

		tracker.setCounterStatus(this, "Collecting halfedge information per face.", counter);
		_halfedgeNormals = new LongObjectHashMap<WB_Coord>();
		_halfedgeEWs = new LongDoubleHashMap();
		for (int i = 0; i < nf; i++) {
			f = faces.get(i);
			c = _faceCenters.get(f.getKey());
			he = f.getHalfedge();
			do {
				_halfedgeNormals.put(he.key(), he.getHalfedgeNormal());
				_halfedgeEWs.put(he.key(), he.getHalfedgeDihedralAngle() < thresholdAngle
						? hardEdgeChamfer.evaluate(c.xd(), c.yd(), c.zd()) : chamfer.evaluate(c.xd(), c.yd(), c.zd()));
				he = he.getNextInFace();
			} while (he != f.getHalfedge());
			counter.increment();
		}

		mesh.getFaces();

		applyFlat(mesh, faces);

		HET_Texture.cleanUVW(mesh);
		mesh.addSelection("extruded", extruded);
		mesh.addSelection("walls", walls);
		mesh.addSelection("failed", failed);
		tracker.setStopStatus(this, "Exiting HEM_FlatExtrude.");
		return mesh;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.hemesh.HE_Modifier#apply(wblut.hemesh.HE_Mesh)
	 */
	@Override
	protected HE_Mesh applySelf(final HE_Selection selection) {
		tracker.setStartStatus(this, "Starting HEM_FlatExtrude.");
		selection.parent.resetFaceInternalLabels();
		walls = new HE_Selection(selection.parent);
		extruded = new HE_Selection(selection.parent);
		failed = new HE_Selection(selection.parent);
		if (chamfer == WB_ScalarParameter.ZERO) {
			tracker.setStopStatus(this, "Exiting HEM_FlatExtrude.");
			return selection.parent;
		}
		HE_Face f;
		HE_Halfedge he;
		WB_Coord c;
		final List<HE_Face> faces = selection.getFaces();

		_faceCenters = selection.getKeyedFaceCenters();
		final int nf = faces.size();
		WB_ProgressCounter counter = new WB_ProgressCounter(nf, 10);

		tracker.setCounterStatus(this, "Collecting halfedge information per face.", counter);
		_halfedgeNormals = new LongObjectHashMap<WB_Coord>();
		_halfedgeEWs = new LongDoubleHashMap();
		for (int i = 0; i < nf; i++) {
			f = faces.get(i);
			c = _faceCenters.get(f.getKey());
			he = f.getHalfedge();
			do {
				_halfedgeNormals.put(he.key(), he.getHalfedgeNormal());
				_halfedgeEWs.put(he.key(), he.getHalfedgeDihedralAngle() < thresholdAngle
						? hardEdgeChamfer.evaluate(c.xd(), c.yd(), c.zd()) : chamfer.evaluate(c.xd(), c.yd(), c.zd()));
				he = he.getNextInFace();
			} while (he != f.getHalfedge());
			counter.increment();
		}

		selection.parent.getFaces();

		applyFlat(selection.parent, faces);

		HET_Texture.cleanUVW(selection.parent);
		selection.parent.addSelection("extruded", extruded);
		selection.parent.addSelection("walls", walls);
		selection.parent.addSelection("failed", failed);
		tracker.setStopStatus(this, "Exiting HEM_FlatExtrude.");
		return selection.parent;
	}

	/**
	 * Apply flat extrusion.
	 *
	 * @param mesh
	 * @param faces
	 * @return mesh
	 */
	private HE_Mesh applyFlat(final HE_Mesh mesh, final List<HE_Face> faces) {
		final HE_Selection sel = new HE_Selection(mesh);
		sel.addFaces(faces);
		sel.collectHalfedges();
		sel.getHalfedges();
		final int nf = faces.size();
		WB_ProgressCounter counter = new WB_ProgressCounter(nf, 10);

		tracker.setCounterStatus(this, "Creating flat extrusions.", counter);
		for (int i = 0; i < nf; i++) {
			if (!applyFlatToOneFace(i, faces, mesh)) {
				failed.add(faces.get(i));
			}
		}

		counter.increment();

		return mesh;
	}

	/**
	 * Apply flat extrusion to one face.
	 *
	 * @param id
	 * @param selFaces
	 * @param mesh
	 * @return true, if successful
	 */
	private boolean applyFlatToOneFace(final int id, final List<HE_Face> selFaces, final HE_Mesh mesh) {
		final HE_Face f = selFaces.get(id);
		final WB_Coord fc = _faceCenters.get(f.key());
		final List<HE_Vertex> faceVertices = new FastList<HE_Vertex>();
		final List<HE_Halfedge> faceHalfedges = new FastList<HE_Halfedge>();
		final List<WB_Coord> faceHalfedgeNormals = new FastList<WB_Coord>();
		final List<WB_Coord> faceEdgeCenters = new FastList<WB_Coord>();
		final List<HE_Vertex> extFaceVertices = new FastList<HE_Vertex>();
		HE_Halfedge he = f.getHalfedge();
		do {
			faceVertices.add(he.getVertex());
			faceHalfedges.add(he);
			faceHalfedgeNormals.add(_halfedgeNormals.get(he.key()));
			faceEdgeCenters.add(he.getHalfedgeCenter());
			extFaceVertices.add(he.getVertex().copy());
			he = he.getNextInFace();
		} while (he != f.getHalfedge());
		boolean isPossible = true;
		final int n = faceVertices.size();
		if (relative == true) {
			double ch;
			for (int i = 0; i < n; i++) {
				final HE_Vertex v = faceVertices.get(i);
				final WB_Point diff = new WB_Point(fc).subSelf(v);
				he = faceHalfedges.get(i);
				ch = Math.max(_halfedgeEWs.get(he.key()), _halfedgeEWs.get(he.getPrevInFace().key()));
				diff.mulSelf(ch);
				diff.addSelf(v);
				extFaceVertices.get(i).set(diff);
			}
		} else {
			final double[] d = new double[n];
			for (int i = 0; i < n; i++) {
				d[i] = _halfedgeEWs.get(faceHalfedges.get(i).key());
			}
			if (f.getFaceType() == WB_Classification.CONVEX) {
				final WB_Point[] vPos = new WB_Point[n];
				for (int i = 0; i < n; i++) {
					final HE_Vertex v = faceVertices.get(i);
					vPos[i] = new WB_Point(v);
				}
				WB_Polygon poly = gf.createSimplePolygon(vPos);
				poly = poly.trimConvexPolygon(d);
				if (poly.getNumberOfShellPoints() > 2) {

					for (int i = 0; i < n; i++) {
						extFaceVertices.get(i).set(poly.closestPoint(faceVertices.get(i)));
					}
				} else {
					isPossible = false;
				}
			} else {
				WB_Coord v1 = new WB_Point(faceVertices.get(n - 1));
				WB_Coord v2 = new WB_Point(faceVertices.get(0));
				for (int i = 0, j = n - 1; i < n; j = i, i++) {
					final WB_Coord n1 = faceHalfedgeNormals.get(j);
					final WB_Coord n2 = faceHalfedgeNormals.get(i);
					final WB_Coord v3 = faceVertices.get((i + 1) % n);
					final WB_Segment S1 = new WB_Segment(WB_Point.addMul(v1, d[j], n1), WB_Point.addMul(v2, d[j], n1));
					final WB_Segment S2 = new WB_Segment(WB_Point.addMul(v2, d[i], n2), WB_Point.addMul(v3, d[i], n2));
					final WB_IntersectionResult ir = WB_GeometryOp3D.getIntersection3D(S1, S2);
					final WB_Coord p = ir.dimension == 0 ? (WB_Point) ir.object : ((WB_Segment) ir.object).getCenter();
					extFaceVertices.get(i).set(p);
					v1 = v2;
					v2 = v3;
				}
			}
		}
		if (isPossible) {
			extruded.add(f);
			f.setInternalLabel(1);
			final List<HE_Halfedge> newhes = new FastList<HE_Halfedge>();
			int c = 0;
			he = f.getHalfedge();
			do {
				final HE_Face fNew = new HE_Face();
				walls.add(fNew);
				fNew.copyProperties(f);
				fNew.setInternalLabel(2);
				final HE_Halfedge heOrig1 = he;
				final HE_Halfedge heOrig2 = he.getPair();
				final HE_Halfedge heNew1 = new HE_Halfedge();
				final HE_Halfedge heNew2 = new HE_Halfedge();
				final HE_Halfedge heNew3 = new HE_Halfedge();
				final HE_Halfedge heNew4 = new HE_Halfedge();
				final int cp = (c + 1) % faceVertices.size();
				final HE_Vertex v1 = faceVertices.get(c);
				final HE_Vertex v2 = faceVertices.get(cp);
				final HE_Vertex v4 = extFaceVertices.get(c);
				final HE_Vertex v3 = extFaceVertices.get(cp);
				mesh.setVertex(heNew1, v1);
				mesh.setHalfedge(v1, heNew1);
				mesh.setFace(heNew1, fNew);
				mesh.setHalfedge(fNew, heNew1);
				mesh.setPair(heNew1, heOrig2);
				mesh.setNext(heNew1, heNew2);
				mesh.setVertex(heNew2, v2);
				mesh.setHalfedge(v2, heNew2);
				mesh.setFace(heNew2, fNew);
				mesh.setNext(heNew2, heNew3);
				mesh.setVertex(heNew3, v3);
				mesh.setHalfedge(v3, heNew3);
				mesh.setFace(heNew3, fNew);
				mesh.remove(heOrig1);
				mesh.add(heOrig1);
				mesh.setPair(heNew3, heOrig1);
				mesh.setNext(heNew3, heNew4);
				mesh.setVertex(heNew4, v4);
				mesh.setHalfedge(v4, heNew4);
				mesh.setFace(heNew4, fNew);
				mesh.setNext(heNew4, heNew1);
				mesh.setVertex(heOrig1, v4);
				mesh.addDerivedElement(fNew, heOrig1, heOrig2);
				mesh.add(v3);
				mesh.add(heNew1);
				mesh.add(heNew2);
				mesh.add(heNew3);
				mesh.add(heNew4);
				newhes.add(heNew1);
				newhes.add(heNew2);
				newhes.add(heNew3);
				newhes.add(heNew4);
				he = he.getNextInFace();
				c++;
			} while (he != f.getHalfedge());
			mesh.pairHalfedges(newhes);
			final List<HE_Halfedge> edgesToRemove = new FastList<HE_Halfedge>();
			for (int i = 0; i < newhes.size(); i++) {
				final HE_Halfedge e = newhes.get(i);
				if (e.isEdge()) {
					if (WB_Epsilon.isZeroSq(WB_GeometryOp3D.getSqDistance3D(e.getStartVertex(), e.getEndVertex()))) {
						edgesToRemove.add(e);
					}
				}
			}
			for (int i = 0; i < edgesToRemove.size(); i++) {
				HET_MeshOp.collapseEdge(mesh, edgesToRemove.get(i));
			}
		}

		return isPossible;
	}

}
