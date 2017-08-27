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

import java.util.List;
import java.util.Map;

import gnu.trove.map.TLongDoubleMap;
import gnu.trove.map.hash.TLongDoubleHashMap;
import javolution.util.FastMap;
import javolution.util.FastTable;
import wblut.core.WB_ProgressCounter;
import wblut.geom.WB_Classification;
import wblut.geom.WB_Coord;
import wblut.geom.WB_GeometryFactory;
import wblut.geom.WB_GeometryOp3D;
import wblut.geom.WB_IntersectionResult;
import wblut.geom.WB_Point;
import wblut.geom.WB_Polygon;
import wblut.geom.WB_Segment;
import wblut.geom.WB_Vector;
import wblut.math.WB_ConstantScalarParameter;
import wblut.math.WB_Epsilon;
import wblut.math.WB_ScalarParameter;

/**
 * Extrudes and scales a face along its face normal.
 *
 * @author Frederik Vanhoutte (W:Blut)
 *
 */
public class HEM_Extrude extends HEM_Modifier {
	/**
	 *
	 */
	private static WB_GeometryFactory gf = new WB_GeometryFactory();
	/** Extrusion distance. */
	private WB_ScalarParameter d;
	/** Threshold angle for hard edges. */
	private double thresholdAngle;
	/** Chamfer factor or distance. */
	private WB_ScalarParameter chamfer;
	/** Hard edge chamfer distance. */
	private WB_ScalarParameter hardEdgeChamfer;
	/** Extrusion mode. */
	private boolean relative;
	/** Fuse coplanar faces. */
	private boolean fuse;
	/** Turn non-extrudable faces into spiked faces?. */
	private boolean peak;
	/** Limit angle for face fusion. */
	private double fuseAngle;
	/** sin(fuseAngle). */
	private double sin2FA;
	/** Vertex normals. */
	private Map<Long, WB_Coord> _faceNormals;
	/** Halfedge normals. */
	private Map<Long, WB_Coord> _halfedgeNormals;
	/** Extrusion widths. */
	private TLongDoubleMap _halfedgeEWs;
	/** Face centers. */
	private Map<Long, WB_Coord> _faceCenters;
	/**
	 *
	 */
	private double[] heights;
	public HE_Selection walls;
	public HE_Selection extruded;

	private List<HE_Face> failedFaces;
	private List<Double> failedHeights;
	boolean isFlat, isSpiky, isStraight;

	/**
	 * Instantiates a new HEM_Extrude.
	 */
	public HEM_Extrude() {
		super();
		d = new WB_ConstantScalarParameter(0.0);
		isFlat = true;
		isSpiky = false;
		isStraight = true;
		thresholdAngle = -1;
		chamfer = new WB_ConstantScalarParameter(0.0);
		hardEdgeChamfer = new WB_ConstantScalarParameter(0.0);
		relative = true;
		fuseAngle = Math.PI / 36;
		sin2FA = Math.sin(fuseAngle);
		sin2FA *= sin2FA;
		heights = null;
	}

	/**
	 * Set extrusion distance.
	 *
	 * @param d
	 *            extrusion distance
	 * @return self
	 */
	public HEM_Extrude setDistance(final double d) {
		this.d = new WB_ConstantScalarParameter(d);
		isFlat = WB_Epsilon.isZero(d);

		return this;
	}

	/**
	 * Sets the distance.
	 *
	 * @param d
	 * @return self
	 */
	public HEM_Extrude setDistance(final WB_ScalarParameter d) {
		this.d = d;
		isFlat = false;

		return this;
	}

	/**
	 * Set chamfer factor.
	 *
	 * @param c
	 * @return self
	 */
	public HEM_Extrude setChamfer(final double c) {
		chamfer = new WB_ConstantScalarParameter(c);
		isSpiky = relative && WB_Epsilon.isZero(1.0 - c);
		isStraight = WB_Epsilon.isZero(c);
		return this;
	}

	public HEM_Extrude setChamfer(final WB_ScalarParameter c) {
		chamfer = c;
		isSpiky = false;
		isStraight = false;
		return this;
	}

	/**
	 * Set hard edge chamfer distance
	 *
	 * @param c
	 * @return self
	 */
	public HEM_Extrude setHardEdgeChamfer(final double c) {
		hardEdgeChamfer = new WB_ConstantScalarParameter(c);
		return this;
	}

	public HEM_Extrude setHardEdgeChamfer(final WB_ScalarParameter c) {
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
	public HEM_Extrude setRelative(final boolean relative) {
		this.relative = relative;
		return this;
	}

	/**
	 * Set fuse option: merges coplanar faces.
	 *
	 * @param b
	 *            true, false
	 * @return self
	 */
	public HEM_Extrude setFuse(final boolean b) {
		fuse = b;
		return this;
	}

	/**
	 * Set peak option.
	 *
	 * @param b
	 *            true, false
	 * @return self
	 */
	public HEM_Extrude setPeak(final boolean b) {
		peak = b;
		return this;
	}

	/**
	 * Set threshold angle for hard edge.
	 *
	 * @param a
	 *            threshold angle
	 * @return self
	 */
	public HEM_Extrude setThresholdAngle(final double a) {
		thresholdAngle = a;
		return this;
	}

	/**
	 * Set threshold angle for fuse.
	 *
	 * @param a
	 *            threshold angle
	 * @return self
	 */
	public HEM_Extrude setFuseAngle(final double a) {
		fuseAngle = a;
		sin2FA = Math.sin(fuseAngle);
		sin2FA *= sin2FA;
		return this;
	}

	/**
	 *
	 *
	 * @param distances
	 * @return
	 */
	public HEM_Extrude setDistances(final double[] distances) {
		this.heights = distances;
		return this;
	}

	/**
	 *
	 *
	 * @param distances
	 * @return
	 */
	public HEM_Extrude setDistances(final float[] distances) {
		heights = new double[distances.length];
		for (int i = 0; i < distances.length; i++) {
			heights[i] = distances[i];
		}
		return this;
	}

	/**
	 *
	 *
	 * @param distances
	 * @return
	 */
	public HEM_Extrude setDistances(final int[] distances) {
		heights = new double[distances.length];
		for (int i = 0; i < distances.length; i++) {
			heights[i] = distances[i];
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
		tracker.setStartStatus(this, "Starting HEM_Extrude.");
		mesh.resetFaceInternalLabels();
		walls = new HE_Selection(mesh);
		extruded = new HE_Selection(mesh);

		if (chamfer == null && d == null && heights == null) {
			tracker.setStopStatus(this, "Exiting HEM_Extrude.");
			return mesh;
		}
		HE_Face f;
		HE_Halfedge he;
		WB_Coord c;
		final List<HE_Face> faces = mesh.getFaces();
		_faceNormals = mesh.getKeyedFaceNormals();
		_faceCenters = mesh.getKeyedFaceCenters();
		final int nf = faces.size();
		WB_ProgressCounter counter = new WB_ProgressCounter(nf, 10);

		tracker.setCounterStatus(this, "Collecting halfedge information per face.", counter);
		_halfedgeNormals = new FastMap<Long, WB_Coord>();
		_halfedgeEWs = new TLongDoubleHashMap(10, 0.5f, -1L, Double.NaN);
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
		if (isStraight) {
			return applyStraight(mesh, mesh.getFaces());
		}
		final List<HE_Face> facelist = mesh.getFaces();
		if (isSpiky) {
			return applyPeaked(mesh, facelist);
		}
		failedFaces = new FastTable<HE_Face>();
		failedHeights = new FastTable<Double>();
		applyFlat(mesh, faces, isFlat && fuse);

		if (heights != null) {
			for (int i = 0; i < failedHeights.size(); i++) {
				heights[facelist.indexOf(failedFaces.get(i))] = failedHeights.get(i);
			}
		}
		if (peak) {
			applyPeaked(mesh, failedFaces);
		}
		WB_Coord n;
		if (!isFlat) {

			if (heights != null) {
				if (heights.length == faces.size()) {
					for (int i = 0; i < faces.size(); i++) {
						f = faces.get(i);
						if (!failedFaces.contains(f)) {
							n = _faceNormals.get(f.key());
							he = f.getHalfedge();
							do {
								he.getVertex().addMulSelf(heights[i], n);
								he = he.getNextInFace();
							} while (he != f.getHalfedge());
						}
					}
				} else {
					throw new IllegalArgumentException(
							"Length of heights array does not correspond to number of extruded faces.");
				}
			} else {

				for (int i = 0; i < nf; i++) {
					f = faces.get(i);

					if (!failedFaces.contains(f)) {
						n = _faceNormals.get(f.key());
						he = f.getHalfedge();
						do {
							final HE_Vertex v = he.getVertex();
							v.addMulSelf(d.evaluate(v.xd(), v.yd(), v.zd()), n);
							he = he.getNextInFace();
						} while (he != f.getHalfedge());
					}
				}
			}
		}
		HET_Texture.cleanUVW(mesh);
		tracker.setStopStatus(this, "Exiting HEM_Extrude.");
		return mesh;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.hemesh.HE_Modifier#apply(wblut.hemesh.HE_Mesh)
	 */
	@Override
	protected HE_Mesh applySelf(final HE_Selection selection) {
		tracker.setStartStatus(this, "Starting HEM_Extrude.");
		selection.parent.resetFaceInternalLabels();
		walls = new HE_Selection(selection.parent);
		extruded = new HE_Selection(selection.parent);
		if (selection.getNumberOfFaces() == 0) {
			tracker.setStopStatus(this, "Exiting HEM_Extrude.");
			return selection.parent;
		}
		_halfedgeNormals = new FastMap<Long, WB_Coord>();
		_halfedgeEWs = new TLongDoubleHashMap(10, 0.5f, -1L, Double.NaN);
		if (chamfer == null && isFlat && heights == null) {
			return selection.parent;
		}
		HE_Face f;
		HE_Halfedge he;
		WB_Coord c;
		final List<HE_Face> selFaces = selection.getFaces();
		_faceNormals = selection.parent.getKeyedFaceNormals();
		_faceCenters = selection.parent.getKeyedFaceCenters();
		final int nf = selFaces.size();
		WB_ProgressCounter counter = new WB_ProgressCounter(nf, 10);

		tracker.setCounterStatus(this, "Collecting halfedge information per face.", counter);
		for (int i = 0; i < nf; i++) {
			f = selFaces.get(i);
			he = f.getHalfedge();
			c = _faceCenters.get(f.getKey());
			do {
				_halfedgeNormals.put(he.key(), he.getHalfedgeNormal());
				_halfedgeEWs.put(he.key(), he.getHalfedgeDihedralAngle() < thresholdAngle
						? hardEdgeChamfer.evaluate(c.xd(), c.yd(), c.zd()) : chamfer.evaluate(c.xd(), c.yd(), c.zd()));
				he = he.getNextInFace();
			} while (he != f.getHalfedge());
			counter.increment();
		}
		if (isStraight) {
			return applyStraight(selection.parent, selFaces);
		}
		if (isSpiky) {
			return applyPeaked(selection.parent, selFaces);
		}
		failedFaces = new FastTable<HE_Face>();
		failedHeights = new FastTable<Double>();
		applyFlat(selection.parent, selFaces, isFlat && fuse);
		if (heights != null) {
			for (int i = 0; i < failedHeights.size(); i++) {
				heights[selFaces.indexOf(failedFaces.get(i))] = failedHeights.get(i);
			}
		}
		if (peak) {
			applyPeaked(selection.parent, failedFaces);
		}
		WB_Coord n;
		if (!isFlat) {
			if (heights != null) {
				if (heights.length == selFaces.size()) {
					for (int i = 0; i < selFaces.size(); i++) {
						f = selFaces.get(i);
						n = _faceNormals.get(f.key());
						he = f.getHalfedge();
						do {
							he.getVertex().addMulSelf(heights[i], n);
							he = he.getNextInFace();
						} while (he != f.getHalfedge());
					}
				} else {
					throw new IllegalArgumentException(
							"Length of heights array does not correspond to number of extruded faces.");
				}
			} else {
				for (int i = 0; i < selFaces.size(); i++) {
					f = selFaces.get(i);
					n = _faceNormals.get(f.key());
					he = f.getHalfedge();
					do {
						final HE_Vertex v = he.getVertex();
						v.addMulSelf(d.evaluate(v.xd(), v.yd(), v.zd()), n);
						he = he.getNextInFace();
					} while (he != f.getHalfedge());
				}
			}
		}
		HET_Texture.cleanUVW(selection.parent);
		tracker.setStopStatus(this, "Exiting HEM_Extrude.");
		return selection.parent;
	}

	/**
	 * Apply straight extrusion.
	 *
	 * @param mesh
	 *            the mesh
	 * @param faces
	 *            the faces
	 * @return mesh
	 */
	private HE_Mesh applyStraight(final HE_Mesh mesh, final List<HE_Face> faces) {
		final int nf = faces.size();
		final boolean[] visited = new boolean[nf];
		WB_Coord fc;
		WB_ProgressCounter counter = new WB_ProgressCounter(nf, 10);

		tracker.setCounterStatus(this, "Creating straight extrusions.", counter);
		if (heights != null) {
			if (heights.length == faces.size()) {
				for (int i = 0; i < nf; i++) {
					// System.out.println(heights[i]);
					applyStraightToOneFaceIgnoreNeighborhood(i, faces, mesh, visited, heights[i]);
					counter.increment();
				}
			} else {
				throw new IllegalArgumentException(
						"Length of heights array does not correspond to number of extruded faces.");
			}
		} else {
			for (int i = 0; i < nf; i++) {
				fc = faces.get(i).getFaceCenter();
				applyStraightToOneFace(i, faces, mesh, visited, d.evaluate(fc.xd(), fc.yd(), fc.zd()));
				counter.increment();
			}
		}
		return mesh;
	}

	/**
	 *
	 *
	 * @param id
	 * @param selfaces
	 * @param mesh
	 * @param visited
	 * @param d
	 * @return
	 */
	private boolean applyStraightToOneFaceIgnoreNeighborhood(final int id, final List<HE_Face> selfaces,
			final HE_Mesh mesh, final boolean[] visited, final double d) {
		if (visited[id]) {
			return false;
		}
		final HE_Face f = selfaces.get(id);
		final WB_Coord n = _faceNormals.get(f.key());
		final List<HE_Face> neighborhood = new FastTable<HE_Face>();
		neighborhood.add(f);
		f.setInternalLabel(1);
		visited[id] = true;

		extruded.addFaces(neighborhood);
		final List<HE_Halfedge> outerHalfedges = new FastTable<HE_Halfedge>();
		final List<HE_Halfedge> halfedges = new FastTable<HE_Halfedge>();
		final List<HE_Vertex> vertices = new FastTable<HE_Vertex>();
		final List<HE_Halfedge> pairHalfedges = new FastTable<HE_Halfedge>();
		final List<HE_Vertex> outerVertices = new FastTable<HE_Vertex>();
		final List<HE_Vertex> extOuterVertices = new FastTable<HE_Vertex>();
		for (int i = 0; i < neighborhood.size(); i++) {
			HE_Halfedge he = neighborhood.get(i).getHalfedge();
			do {
				final HE_Face fp = he.getPair().getFace();
				if (fp == null || !neighborhood.contains(fp)) {
					outerHalfedges.add(he);
				}
				halfedges.add(he);
				if (!vertices.contains(he.getVertex())) {
					vertices.add(he.getVertex());
				}
				he = he.getNextInFace();
			} while (he != neighborhood.get(i).getHalfedge());
		}
		for (int i = 0; i < outerHalfedges.size(); i++) {
			pairHalfedges.add(outerHalfedges.get(i).getPair());
			outerVertices.add(outerHalfedges.get(i).getVertex());
			final HE_Vertex eov = new HE_Vertex(outerHalfedges.get(i).getVertex());
			eov.copyProperties(outerHalfedges.get(i).getVertex());
			if (n != null) {
				eov.addMulSelf(d, n);
			}
			extOuterVertices.add(eov);
		}
		mesh.addVertices(extOuterVertices);
		for (int i = 0; i < vertices.size(); i++) {
			final HE_Vertex v = vertices.get(i);
			if (!outerVertices.contains(v)) {
				v.addMulSelf(d, n);
			}
		}
		for (int i = 0; i < halfedges.size(); i++) {
			final HE_Halfedge he = halfedges.get(i);
			final int ovi = outerVertices.indexOf(he.getVertex());
			if (ovi >= 0) {
				mesh.setVertex(he, extOuterVertices.get(ovi));
				mesh.setHalfedge(extOuterVertices.get(ovi), he);
			}
		}
		final List<HE_Halfedge> newhes = new FastTable<HE_Halfedge>();
		for (int c = 0; c < outerHalfedges.size(); c++) {
			final HE_Face fNew = new HE_Face();
			walls.add(fNew);
			fNew.copyProperties(f);
			fNew.setInternalLabel(2);
			final HE_Halfedge heOrig1 = outerHalfedges.get(c);
			final HE_Halfedge heOrig2 = pairHalfedges.get(c);
			final HE_Halfedge heNew1 = new HE_Halfedge();
			final HE_Halfedge heNew2 = new HE_Halfedge();
			final HE_Halfedge heNew3 = new HE_Halfedge();
			final HE_Halfedge heNew4 = new HE_Halfedge();
			HE_Halfedge hen = heOrig1.getNextInFace();
			int cp = -1;
			do {
				cp = outerHalfedges.indexOf(hen);
				hen = hen.getPair().getNextInFace();
			} while (hen != heOrig1.getNextInFace() && cp == -1);
			final HE_Vertex v1 = outerVertices.get(c);
			final HE_Vertex v2 = outerVertices.get(cp);
			final HE_Vertex v4 = extOuterVertices.get(c);
			final HE_Vertex v3 = extOuterVertices.get(cp);
			mesh.setVertex(heNew1, v1);
			mesh.setHalfedge(v1, heNew1);
			mesh.setFace(heNew1, fNew);
			mesh.setHalfedge(fNew, heNew1);
			mesh.setPair(heOrig2, heNew1);
			mesh.setNext(heNew1, heNew2);
			mesh.setVertex(heNew2, v2);
			mesh.setHalfedge(v2, heNew2);
			mesh.setFace(heNew2, fNew);
			mesh.setNext(heNew2, heNew3);
			mesh.setVertex(heNew3, v3);
			mesh.setHalfedge(v3, heNew3);
			mesh.setFace(heNew3, fNew);
			mesh.setPair(heNew3, heOrig1);
			mesh.setNext(heNew3, heNew4);
			mesh.setVertex(heNew4, v4);
			mesh.setHalfedge(v4, heNew4);
			mesh.setFace(heNew4, fNew);
			mesh.setNext(heNew4, heNew1);
			mesh.setVertex(heOrig1, v4);
			mesh.add(fNew);
			mesh.add(heNew1);
			mesh.add(heNew2);
			mesh.add(heNew3);
			mesh.add(heNew4);
			newhes.add(heNew1);
			newhes.add(heNew2);
			newhes.add(heNew3);
			newhes.add(heNew4);
		}
		mesh.pairHalfedges(newhes);
		return true;
	}

	/**
	 * Apply straight extrusion to one face.
	 *
	 * @param id
	 *            the id
	 * @param selfaces
	 *            the selfaces
	 * @param mesh
	 *            the mesh
	 * @param visited
	 *            the visited
	 * @param d
	 * @return true, if successful
	 */
	private boolean applyStraightToOneFace(final int id, final List<HE_Face> selfaces, final HE_Mesh mesh,
			final boolean[] visited, final double d) {
		if (visited[id]) {
			return false;
		}
		final HE_Face f = selfaces.get(id);
		final WB_Coord n = _faceNormals.get(f.key());
		final List<HE_Face> neighborhood = new FastTable<HE_Face>();
		neighborhood.add(f);
		f.setInternalLabel(1);
		visited[id] = true;
		int no = 0;
		int nn = 1;
		do {
			nn = neighborhood.size();
			for (int i = no; i < nn; i++) {
				final HE_Face fi = neighborhood.get(i);
				final List<HE_Face> faces = fi.getNeighborFaces();
				for (int j = 0; j < faces.size(); j++) {
					final HE_Face fj = faces.get(j);
					if (_faceNormals.get(fi.key()) != null && _faceNormals.get(fj.key()) != null) {
						if (WB_Vector.isParallel(_faceNormals.get(fi.key()), _faceNormals.get(fj.key()))) {
							final int ij = selfaces.indexOf(fj);
							if (ij >= 0) {
								if (!neighborhood.contains(fj)) {
									neighborhood.add(fj);
									fj.setInternalLabel(1);
								}
								visited[ij] = true;
							}
						}
					}
				}
			}
			no = nn;
		} while (neighborhood.size() > nn);
		extruded.addFaces(neighborhood);
		final List<HE_Halfedge> outerHalfedges = new FastTable<HE_Halfedge>();
		final List<HE_Halfedge> halfedges = new FastTable<HE_Halfedge>();
		final List<HE_Vertex> vertices = new FastTable<HE_Vertex>();
		final List<HE_Halfedge> pairHalfedges = new FastTable<HE_Halfedge>();
		final List<HE_Vertex> outerVertices = new FastTable<HE_Vertex>();
		final List<HE_Vertex> extOuterVertices = new FastTable<HE_Vertex>();
		for (int i = 0; i < neighborhood.size(); i++) {
			HE_Halfedge he = neighborhood.get(i).getHalfedge();
			do {
				final HE_Face fp = he.getPair().getFace();
				if (fp == null || !neighborhood.contains(fp)) {
					outerHalfedges.add(he);
				}
				halfedges.add(he);
				if (!vertices.contains(he.getVertex())) {
					vertices.add(he.getVertex());
				}
				he = he.getNextInFace();
			} while (he != neighborhood.get(i).getHalfedge());
		}
		for (int i = 0; i < outerHalfedges.size(); i++) {
			pairHalfedges.add(outerHalfedges.get(i).getPair());
			outerVertices.add(outerHalfedges.get(i).getVertex());
			final HE_Vertex eov = new HE_Vertex(outerHalfedges.get(i).getVertex());
			eov.copyProperties(outerHalfedges.get(i).getVertex());
			if (n != null) {
				eov.addMulSelf(d, n);
			}
			extOuterVertices.add(eov);
		}
		mesh.addVertices(extOuterVertices);
		for (int i = 0; i < vertices.size(); i++) {
			final HE_Vertex v = vertices.get(i);
			if (!outerVertices.contains(v)) {
				v.addMulSelf(d, n);
			}
		}
		for (int i = 0; i < halfedges.size(); i++) {
			final HE_Halfedge he = halfedges.get(i);
			final int ovi = outerVertices.indexOf(he.getVertex());
			if (ovi >= 0) {
				mesh.setVertex(he, extOuterVertices.get(ovi));
				mesh.setHalfedge(extOuterVertices.get(ovi), he);
			}
		}
		final List<HE_Halfedge> newhes = new FastTable<HE_Halfedge>();
		for (int c = 0; c < outerHalfedges.size(); c++) {
			final HE_Face fNew = new HE_Face();
			walls.add(fNew);
			fNew.copyProperties(f);
			fNew.setInternalLabel(2);
			final HE_Halfedge heOrig1 = outerHalfedges.get(c);
			final HE_Halfedge heOrig2 = pairHalfedges.get(c);
			final HE_Halfedge heNew1 = new HE_Halfedge();
			final HE_Halfedge heNew2 = new HE_Halfedge();
			final HE_Halfedge heNew3 = new HE_Halfedge();
			final HE_Halfedge heNew4 = new HE_Halfedge();
			HE_Halfedge hen = heOrig1.getNextInFace();
			int cp = -1;
			do {
				cp = outerHalfedges.indexOf(hen);
				hen = hen.getPair().getNextInFace();
			} while (hen != heOrig1.getNextInFace() && cp == -1);
			final HE_Vertex v1 = outerVertices.get(c);
			final HE_Vertex v2 = outerVertices.get(cp);
			final HE_Vertex v4 = extOuterVertices.get(c);
			final HE_Vertex v3 = extOuterVertices.get(cp);
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
			mesh.setPair(heNew3, heOrig1);
			mesh.setNext(heNew3, heNew4);
			mesh.setVertex(heNew4, v4);
			mesh.setHalfedge(v4, heNew4);
			mesh.setFace(heNew4, fNew);
			mesh.setNext(heNew4, heNew1);
			mesh.setVertex(heOrig1, v4);
			mesh.add(fNew);
			mesh.add(heNew1);
			mesh.add(heNew2);
			mesh.add(heNew3);
			mesh.add(heNew4);
			newhes.add(heNew1);
			newhes.add(heNew2);
			newhes.add(heNew3);
			newhes.add(heNew4);
		}
		mesh.pairHalfedges(newhes);
		return true;
	}

	/**
	 * Apply peaked extrusion.
	 *
	 * @param mesh
	 * @param faces
	 * @return mesh
	 */
	private HE_Mesh applyPeaked(final HE_Mesh mesh, final List<HE_Face> faces) {
		final int nf = faces.size();
		HE_Face f;
		WB_Coord fc;
		WB_ProgressCounter counter = new WB_ProgressCounter(nf, 10);

		tracker.setCounterStatus(this, "Creating peaked extrusions.", counter);
		for (int i = 0; i < nf; i++) {
			f = faces.get(i);
			_faceCenters.put(f.key(), f.getFaceCenter());
		}
		if (heights != null) {
			if (heights.length == faces.size()) {
				for (int i = 0; i < nf; i++) {
					applyPeakToOneFace(i, faces, mesh, heights[i]);
					counter.increment();
				}
			} else {
				throw new IllegalArgumentException(
						"Length of heights array does not correspond to number of extruded faces.");
			}
		} else {
			for (int i = 0; i < nf; i++) {
				fc = faces.get(i).getFaceCenter();
				applyPeakToOneFace(i, faces, mesh, d.evaluate(fc.xd(), fc.yd(), fc.zd()));
				counter.increment();
			}
		}
		return mesh;
	}

	/**
	 * Apply peaked extrusion to one face.
	 *
	 * @param id
	 * @param selFaces
	 * @param mesh
	 * @param d
	 */
	private void applyPeakToOneFace(final int id, final List<HE_Face> selFaces, final HE_Mesh mesh, final double d) {
		final HE_Face f = selFaces.get(id);
		final WB_Vector n = new WB_Vector(_faceNormals.get(f.key()));
		final WB_Point fc = new WB_Point(_faceCenters.get(f.key()));
		walls.add(f);
		f.setInternalLabel(4);
		final HE_Face[] newFaces = HEM_TriSplit.splitFaceTri(mesh, f, fc.addSelf(n.mulSelf(d))).getFacesAsArray();
		for (final HE_Face newFace : newFaces) {
			newFace.copyProperties(f);
		}
		walls.addFaces(newFaces);
	}

	/**
	 * Apply flat extrusion.
	 *
	 * @param mesh
	 * @param faces
	 * @param fuse
	 * @return mesh
	 */
	private HE_Mesh applyFlat(final HE_Mesh mesh, final List<HE_Face> faces, final boolean fuse) {
		final HE_Selection sel = new HE_Selection(mesh);
		sel.addFaces(faces);
		sel.collectEdgesByFace();
		final List<HE_Halfedge> originalEdges = sel.getEdges();
		final int nf = faces.size();
		WB_ProgressCounter counter = new WB_ProgressCounter(nf, 10);

		tracker.setCounterStatus(this, "Creating flat extrusions.", counter);
		WB_Coord fc;
		if (heights != null) {
			if (heights.length == faces.size()) {
				for (int i = 0; i < nf; i++) {
					if (!applyFlatToOneFace(i, faces, mesh)) {
						failedFaces.add(faces.get(i));
						failedHeights.add(heights[i]);
					}
					counter.increment();
				}
			} else {
				throw new IllegalArgumentException(
						"Length of heights array does not correspond to number of extruded faces.");
			}
		} else {
			for (int i = 0; i < nf; i++) {
				if (!applyFlatToOneFace(i, faces, mesh)) {
					failedFaces.add(faces.get(i));
					fc = faces.get(i).getFaceCenter();
					failedHeights.add(d.evaluate(fc.xd(), fc.yd(), fc.zd()));
				}
			}

			counter.increment();
		}
		if (fuse) {
			counter = new WB_ProgressCounter(originalEdges.size(), 10);

			tracker.setCounterStatus(this, "Fusing original edges.", counter);
			for (int i = 0; i < originalEdges.size(); i++) {
				final HE_Halfedge e = originalEdges.get(i);
				final HE_Face f1 = e.getFace();
				final HE_Face f2 = e.getPair().getFace();
				if (f1 != null && f2 != null) {
					if (f1.getInternalLabel() == 2 && f2.getInternalLabel() == 2) {
						if (WB_Vector.cross(f1.getFaceNormal(), f2.getFaceNormal()).getSqLength() < sin2FA) {
							final HE_Face f = mesh.deleteEdge(e);
							if (f != null) {
								f.setInternalLabel(3);
							}
						}
					}
				}
				counter.increment();
			}
		}
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
		final List<HE_Vertex> faceVertices = new FastTable<HE_Vertex>();
		final List<HE_Halfedge> faceHalfedges = new FastTable<HE_Halfedge>();
		final List<WB_Coord> faceHalfedgeNormals = new FastTable<WB_Coord>();
		final List<WB_Coord> faceEdgeCenters = new FastTable<WB_Coord>();
		final List<HE_Vertex> extFaceVertices = new FastTable<HE_Vertex>();
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
			if (!isStraight && f.getFaceType() == WB_Classification.CONVEX) {
				final WB_Point[] vPos = new WB_Point[n];
				for (int i = 0; i < n; i++) {
					final HE_Vertex v = faceVertices.get(i);
					vPos[i] = new WB_Point(v);
				}
				WB_Polygon poly = gf.createSimplePolygon(vPos);
				poly = poly.trimConvexPolygon(d);
				if (poly.getNumberOfShellPoints() == n) {
					final int inew = poly.closestIndex(faceVertices.get(0));
					for (int i = 0; i < n; i++) {
						extFaceVertices.get(i).set(poly.getPoint((inew + i) % n));
					}
				} else if (poly.getNumberOfShellPoints() > 2) {
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
			final List<HE_Halfedge> newhes = new FastTable<HE_Halfedge>();
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
				mesh.setPair(heNew3, heOrig1);
				mesh.setNext(heNew3, heNew4);
				mesh.setVertex(heNew4, v4);
				mesh.setHalfedge(v4, heNew4);
				mesh.setFace(heNew4, fNew);
				mesh.setNext(heNew4, heNew1);
				mesh.setVertex(heOrig1, v4);
				mesh.add(fNew);
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
			final List<HE_Halfedge> edgesToRemove = new FastTable<HE_Halfedge>();
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
