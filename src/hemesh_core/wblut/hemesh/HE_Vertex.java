/*
 * HE_Mesh  Frederik Vanhoutte - www.wblut.com
 * 
 * https://github.com/wblut/HE_Mesh
 * A Processing/Java library for for creating and manipulating polygonal meshes.
 * 
 * Public Domain: http://creativecommons.org/publicdomain/zero/1.0/
 */

package wblut.hemesh;

import java.util.Iterator;
import java.util.List;

import org.eclipse.collections.impl.list.mutable.FastList;
import wblut.geom.WB_Classification;
import wblut.geom.WB_Coord;
import wblut.geom.WB_CoordinateSystem3D;
import wblut.geom.WB_GeometryOp3D;
import wblut.geom.WB_MutableCoord;
import wblut.geom.WB_MutableCoordinateFull3D;
import wblut.geom.WB_Point;
import wblut.geom.WB_Transform;
import wblut.geom.WB_Transform2D;
import wblut.geom.WB_Vector;
import wblut.math.WB_Epsilon;
import wblut.math.WB_HashCode;
import wblut.math.WB_M33;
import wblut.math.WB_Math;

/**
 * Vertex element of half-edge mesh.
 *
 * @author Frederik Vanhoutte (W:Blut)
 *
 */
public class HE_Vertex extends HE_MeshElement implements WB_MutableCoordinateFull3D {
	WB_MutableCoord pos;

	/** Halfedge associated with this vertex. */
	private HE_Halfedge _halfedge;

	private HE_TextureCoordinate uvw = null;

	/**
	 * Instantiates a new HE_Vertex.
	 */
	public HE_Vertex() {
		super();
		pos = new WB_Point();
		uvw = null;

	}

	/**
	 * Instantiates a new HE_Vertex at position x, y, z.
	 *
	 * @param x
	 *            x-coordinate of vertex
	 * @param y
	 *            y-coordinate of vertex
	 * @param z
	 *            z-coordinate of vertex
	 */
	public HE_Vertex(final double x, final double y, final double z) {
		super();
		pos = new WB_Point(x, y, z);
		uvw = null;
	}

	/**
	 * Instantiates a new HE_Vertex at position v.
	 *
	 * @param v
	 *            position of vertex
	 */
	public HE_Vertex(final WB_Coord v) {
		super();
		pos = new WB_Point(v);
		uvw = null;
	}

	/**
	 *
	 *
	 * @return
	 */
	public HE_VertexEdgeCirculator veCrc() {
		return new HE_VertexEdgeCirculator(this);
	}

	/**
	 *
	 *
	 * @return
	 */
	public HE_VertexFaceCirculator vfCrc() {
		return new HE_VertexFaceCirculator(this);
	}

	/**
	 *
	 *
	 * @return
	 */
	public HE_VertexVertexCirculator vvCrc() {
		return new HE_VertexVertexCirculator(this);
	}

	/**
	 *
	 *
	 * @return
	 */
	public HE_VertexHalfedgeInCirculator vheiCrc() {
		return new HE_VertexHalfedgeInCirculator(this);
	}

	/**
	 *
	 *
	 * @return
	 */
	public HE_VertexHalfedgeOutCirculator vheoCrc() {
		return new HE_VertexHalfedgeOutCirculator(this);
	}

	/**
	 *
	 *
	 * @return
	 */
	public HE_VertexEdgeRevCirculator veRevCrc() {
		return new HE_VertexEdgeRevCirculator(this);
	}

	/**
	 *
	 *
	 * @return
	 */
	public HE_VertexFaceRevCirculator vfRevCrc() {
		return new HE_VertexFaceRevCirculator(this);
	}

	/**
	 *
	 *
	 * @return
	 */
	public HE_VertexVertexRevCirculator vvRevCrc() {
		return new HE_VertexVertexRevCirculator(this);
	}

	/**
	 *
	 *
	 * @return
	 */
	public HE_VertexHalfedgeInRevCirculator vheiRevCrc() {
		return new HE_VertexHalfedgeInRevCirculator(this);
	}

	/**
	 *
	 *
	 * @return
	 */
	public HE_VertexHalfedgeOutRevCirculator vheoRevCrc() {
		return new HE_VertexHalfedgeOutRevCirculator(this);
	}

	/**
	 *
	 *
	 * @return
	 */
	public HE_Vertex copy() {
		final HE_Vertex copy = new HE_Vertex(pos);
		copy.copyProperties(this);
		return copy;
	}

	/**
	 * Get halfedge associated with this vertex.
	 *
	 * @return halfedge
	 */
	public HE_Halfedge getHalfedge() {
		return _halfedge;
	}

	/**
	 * Sets the halfedge associated with this vertex.
	 *
	 * @param halfedge
	 *            the new halfedge
	 */
	protected void _setHalfedge(final HE_Halfedge halfedge) {
		_halfedge = halfedge;
	}

	/**
	 * Set position to v.
	 *
	 * @param v
	 *            position
	 */
	public void set(final HE_Vertex v) {
		pos.set(v);
	}

	/**
	 *
	 *
	 * @param d
	 * @return
	 */
	public WB_Point getOffset(final double d) {
		return WB_Point.addMul(pos, d, getVertexNormal());
	}

	public HE_Vertex getNextInFace() {
		return getHalfedge().getNextInFace().getVertex();
	}

	public HE_Vertex getPrevInFace() {
		return getHalfedge().getPrevInFace().getVertex();
	}

	/**
	 * Get vertex type. Returns stored value if update status is true.
	 *
	 * @return HE.VertexType.FLAT: vertex is flat in all faces,
	 *         HE.VertexType.CONVEX: vertex is convex in all faces,
	 *         HE.VertexType.CONCAVE: vertex is concave in all faces,
	 *         HE.VertexType.FLATCONVEX: vertex is convex or flat in all faces,
	 *         HE.VertexType.FLATCONCAVE: vertex is concave or flat in all
	 *         faces, HE.VertexType.SADDLE: vertex is convex and concave in at
	 *         least one face each
	 */
	public WB_Classification getVertexType() {
		return HET_MeshOp.getVertexType(this);
	}

	/**
	 * Clear halfedge.
	 */
	protected void _clearHalfedge() {
		_halfedge = null;
	}

	/**
	 * Get key.
	 *
	 * @return key
	 */
	public long key() {
		return super.getKey();
	}

	/**
	 * Get halfedges in vertex.
	 *
	 * @return halfedges
	 */
	public List<HE_Halfedge> getHalfedgeStar() {
		final List<HE_Halfedge> vhe = new FastList<HE_Halfedge>();
		if (getHalfedge() == null) {
			return vhe;
		}
		HE_Halfedge he = getHalfedge();

		do {
			if (!vhe.contains(he)) {
				vhe.add(he);
			}
			he = he.getNextInVertex();
		} while (he != getHalfedge());
		return vhe;
	}

	/**
	 * Get edges in vertex.
	 *
	 * @return edges
	 */
	public List<HE_Halfedge> getEdgeStar() {
		final List<HE_Halfedge> ve = new FastList<HE_Halfedge>();
		if (getHalfedge() == null) {
			return ve;
		}
		HE_Halfedge he = getHalfedge();
		do {
			if (he.isEdge()) {
				if (!ve.contains(he)) {
					ve.add(he);
				}
			} else {
				if (!ve.contains(he.getPair())) {
					ve.add(he.getPair());
				}
			}
			he = he.getNextInVertex();
		} while (he != getHalfedge());
		return ve;
	}

	/**
	 * Get faces in vertex.
	 *
	 * @return faces
	 */
	public List<HE_Face> getFaceStar() {
		final List<HE_Face> vf = new FastList<HE_Face>();
		if (getHalfedge() == null) {
			return vf;
		}
		HE_Halfedge he = getHalfedge();
		do {
			if (he.getFace() != null) {
				if (!vf.contains(he.getFace())) {
					vf.add(he.getFace());
				}
			}
			he = he.getNextInVertex();
		} while (he != getHalfedge());
		return vf;
	}

	/**
	 * Get neighboring vertices.
	 *
	 * @return neighbors
	 */
	public List<HE_Vertex> getNeighborVertices() {
		final List<HE_Vertex> vv = new FastList<HE_Vertex>();
		if (getHalfedge() == null) {
			return vv;
		}
		HE_Halfedge he = getHalfedge();
		do {
			final HE_Halfedge hen = he.getNextInFace();
			if (hen.getVertex() != this && !vv.contains(hen.getVertex())) {
				vv.add(hen.getVertex());
			}
			he = he.getNextInVertex();
		} while (he != getHalfedge());
		return vv;
	}

	/**
	 *
	 *
	 * @return
	 */
	public List<HE_Vertex> getVertexStar() {
		return getNeighborVertices();
	}

	/**
	 *
	 *
	 * @return
	 */
	public List<HE_Vertex> getNextNeighborVertices() {
		final List<HE_Vertex> result = new FastList<HE_Vertex>();
		if (getHalfedge() == null) {
			return result;
		}
		final List<HE_Vertex> vv = getNeighborVertices();
		for (final HE_Vertex v : vv) {
			result.addAll(v.getNeighborVertices());
		}
		final Iterator<HE_Vertex> vitr = result.iterator();
		HE_Vertex w;
		while (vitr.hasNext()) {
			w = vitr.next();
			if (w == this || vv.contains(w)) {
				vitr.remove();
			}
		}
		return result;
	}

	/**
	 * Gets the neighbors as points.
	 *
	 * @return the neighbors as points
	 */
	public WB_Coord[] getNeighborsAsPoints() {
		final WB_Coord[] vv = new WB_Coord[getVertexDegree()];
		if (getHalfedge() == null) {
			return vv;
		}
		HE_Halfedge he = getHalfedge();
		int i = 0;
		do {
			vv[i] = he.getEndVertex();
			i++;
			he = he.getNextInVertex();
		} while (he != getHalfedge());
		return vv;
	}

	/**
	 * Get number of edges in vertex.
	 *
	 * @return number of edges
	 */
	public int getVertexDegree() {
		int result = 0;
		if (getHalfedge() == null) {
			return 0;
		}
		HE_Halfedge he = getHalfedge();
		do {
			result++;
			he = he.getNextInVertex();
		} while (he != getHalfedge());
		return result;
	}

	/**
	 * Get area of faces bounding vertex.
	 *
	 * @return area
	 */
	public double getVertexArea() {
		return HET_MeshOp.getVertexArea(this);
	}

	/**
	 * Get the barycentric dual area. Triangles only.
	 *
	 * @return
	 */
	public double getBarycentricDualArea() {
		return HET_MeshOp.getBarycentricDualVertexArea(this);
	}

	/**
	 * Get the circumcentric dual area. Triangles only.
	 *
	 * @return
	 */
	public double getCircumcentricDualArea() {
		return HET_MeshOp.getCircumcentricDualVertexArea(this);
	}

	/**
	 * Checks if is boundary.
	 *
	 * @return true, if is boundary
	 */
	public boolean isBoundary() {
		HE_Halfedge he = _halfedge;
		do {
			if (he.getFace() == null) {
				return true;
			}
			he = he.getNextInVertex();
		} while (he != _halfedge);
		return false;
	}

	public boolean isIsolated() {

		return _halfedge == null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_Coord#xd()
	 */
	@Override
	public double xd() {
		return pos.xd();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_Coordinate#yd()
	 */
	@Override
	public double yd() {
		return pos.yd();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_Coordinate#zd()
	 */
	@Override
	public double zd() {
		return pos.zd();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_Coordinate#zd()
	 */
	@Override
	public double wd() {
		return 1;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_Coordinate#getd(int)
	 */
	@Override
	public double getd(final int i) {

		return pos.getd(i);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_Coordinate#xf()
	 */
	@Override
	public float xf() {
		return pos.xf();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_Coordinate#yf()
	 */
	@Override
	public float yf() {
		return pos.yf();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_Coordinate#zf()
	 */
	@Override
	public float zf() {
		return pos.zf();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_Coordinate#zf()
	 */
	@Override
	public float wf() {
		return 1.0f;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_Coordinate#getf(int)
	 */
	@Override
	public float getf(final int i) {
		return pos.getf(i);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinate#setX(double)
	 */
	@Override
	public void setX(final double x) {
		pos.setX(x);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinate#setY(double)
	 */
	@Override
	public void setY(final double y) {
		pos.setY(y);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinate#setZ(double)
	 */
	@Override
	public void setZ(final double z) {
		pos.setZ(z);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinate#setW(double)
	 */
	@Override
	public void setW(final double w) {

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinate#setCoord(int, double)
	 */
	@Override
	public void setCoord(final int i, final double v) {
		pos.setCoord(i, v);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinate#set(wblut.geom.WB_Coordinate)
	 */
	@Override
	public void set(final WB_Coord p) {
		pos.set(p);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinate#set(double, double)
	 */
	@Override
	public void set(final double x, final double y) {
		pos.set(x, y);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinate#set(double, double, double)
	 */
	@Override
	public void set(final double x, final double y, final double z) {
		pos.set(x, y, z);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinate#set(double, double, double, double)
	 */
	@Override
	public void set(final double x, final double y, final double z, final double w) {
		pos.set(x, y, z, w);
	}

	/**
	 *
	 *
	 * @return
	 */
	public WB_CoordinateSystem3D getVertexCS() {
		return HET_MeshOp.getVertexCS(this);
	}

	// Common area-weighted mean normal
	/**
	 *
	 *
	 * @return
	 */
	public WB_Coord getVertexNormal() {
		return HET_MeshOp.getVertexNormal(this);
	}

	/**
	 *
	 *
	 * @return
	 */
	public WB_Coord getVertexNormalAverage() {
		return HET_MeshOp.getVertexNormalAverage(this);
	}

	/**
	 *
	 *
	 * @return
	 */
	public WB_Coord getVertexNormalArea() {
		return HET_MeshOp.getVertexNormalArea(this);
	}

	/**
	 *
	 *
	 * @return
	 */
	public WB_Coord getVertexNormalAngle() {
		return HET_MeshOp.getVertexNormalAngle(this);

	}

	/**
	 *
	 *
	 * @return
	 */
	public WB_Coord getVertexNormalGaussCurvature() {
		return HET_MeshOp.getVertexNormalGaussCurvature(this);

	}

	/**
	 *
	 *
	 * @return
	 */
	public WB_Coord getVertexNormalMeanCurvature() {
		return HET_MeshOp.getVertexNormalMeanCurvature(this);

	}

	/**
	 *
	 *
	 * @return
	 */
	public WB_Coord getVertexNormalSphereInscribed() {
		return HET_MeshOp.getVertexNormalSphereInscribed(this);

	}

	/**
	 * Returns the discrete Gaussian curvature and the mean normal. These
	 * discrete operators are described in "Discrete Differential-Geometry
	 * Operators for Triangulated 2-Manifolds", Mark Meyer, Mathieu Desbrun,
	 * Peter Schr???der, and Alan H. Barr.
	 * http://www.cs.caltech.edu/~mmeyer/Publications/diffGeomOps.pdf
	 * http://www.cs.caltech.edu/~mmeyer/Publications/diffGeomOps.pdf Note: on a
	 * sphere, the Gaussian curvature is very accurate, but not the mean
	 * curvature. Guoliang Xu suggests improvements in his papers
	 * http://lsec.cc.ac.cn/~xuguo/xuguo3.htm
	 *
	 * @param meanCurvatureVector
	 * @return
	 */
	public double getGaussianCurvature(final WB_Vector meanCurvatureVector) {
		return HET_MeshOp.getGaussianCurvature(this, meanCurvatureVector);
	}

	/**
	 * Returns the discrete Gaussian curvature. These discrete operators are
	 * described in "Discrete Differential-Geometry Operators for Triangulated
	 * 2-Manifolds", Mark Meyer, Mathieu Desbrun, Peter Schr???der, and Alan H.
	 * Barr. http://www.cs.caltech.edu/~mmeyer/Publications/diffGeomOps.pdf
	 * http://www.cs.caltech.edu/~mmeyer/Publications/diffGeomOps.pdf Note: on a
	 * sphere, the Gaussian curvature is very accurate, but not the mean
	 * curvature. Guoliang Xu suggests improvements in his papers
	 * http://lsec.cc.ac.cn/~xuguo/xuguo3.htm
	 *
	 *
	 * @return
	 */
	public double getGaussCurvature() {
		return HET_MeshOp.getGaussCurvature(this);
	}

	/**
	 *
	 *
	 * @return
	 */
	public WB_CoordinateSystem3D getCurvatureDirections() {
		return HET_MeshOp.getCurvatureDirections(this);
	}

	/**
	 *
	 *
	 * @param el
	 */
	public void copyProperties(final HE_Vertex el) {
		super.copyProperties(el);

		if (el.getVertexUVW() == null) {
			uvw = null;
		} else {
			uvw = new HE_TextureCoordinate(el.getVertexUVW());
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.hemesh.HE_Element#clear()
	 */
	@Override
	public void clear() {
		_halfedge = null;

	}

	/**
	 *
	 *
	 * @return
	 */
	public double getUmbrellaAngle() {
		return HET_MeshOp.getUmbrellaAngle(this);
	}

	/**
	 *
	 *
	 * @return
	 */
	public double getAngleDefect() {
		return HET_MeshOp.getAngleDefect(this);
	}

	/**
	 *
	 *
	 * @return
	 */
	public double getScalarGaussCurvature() {
		return HET_MeshOp.getScalarGaussCurvature(this);
	}

	/**
	 *
	 *
	 * @return
	 */
	public double getScalarMeanCurvature() {
		return HET_MeshOp.getScalarMeanCurvature(this);
	}

	/**
	 *
	 *
	 * @param f
	 * @return
	 */
	public HE_Halfedge getHalfedge(final HE_Face f) {
		HE_Halfedge he = _halfedge;
		if (he == null) {
			return null;
		}
		if (f == null) {
			do {
				if (he.getFace() == null) {
					return he;
				}
				he = he.getNextInVertex();
			} while (he != _halfedge);
		} else {
			do {
				if (he.getFace() == f) {
					return he;
				}
				he = he.getNextInVertex();
			} while (he != _halfedge);
		}
		return null;
	}

	// TEXTURE COORDINATES

	/**
	 * Clear vertex UVW.
	 */

	public void clearUVW() {
		uvw = null;
	}

	/**
	 * Set vertex UVW.
	 *
	 * @param u
	 * @param v
	 * @param w
	 */
	public void setUVW(final double u, final double v, final double w) {
		uvw = new HE_TextureCoordinate(u, v, w);
	}

	/**
	 * Set vertex UVW.
	 *
	 * @param uvw
	 *            WB_Coord
	 */
	public void setUVW(final WB_Coord uvw) {
		if (uvw == null) {
			return;
		}
		this.uvw = new HE_TextureCoordinate(uvw);
	}

	/**
	 * Set vertex UVW.
	 *
	 * @param uvw
	 *            HE_TextureCoordinate
	 */
	public void setUVW(final HE_TextureCoordinate uvw) {
		if (uvw == null) {
			return;
		}
		this.uvw = new HE_TextureCoordinate(uvw);
	}

	/**
	 * Set UVW in halfedge in this vertex, belonging to face. If no such
	 * halfedge exists, nothing happens.
	 *
	 * @param u
	 * @param v
	 * @param w
	 * @param face
	 */
	public void setUVW(final double u, final double v, final double w, final HE_Face face) {
		HE_Halfedge he = getHalfedge(face);
		if (he != null) {
			he.setUVW(u, v, w);
		}

	}

	/**
	 * Set UVW in halfedge in this vertex, belonging to face. If no such
	 * halfedge exists, nothing happens.
	 *
	 * @param uvw
	 *            WB_Coord
	 * @param face
	 */
	public void setUVW(final WB_Coord uvw, final HE_Face face) {
		HE_Halfedge he = getHalfedge(face);
		if (he != null) {
			he.setUVW(uvw);
		}

	}

	/**
	 * Set UVW in halfedge in this vertex, belonging to face. If no such
	 * halfedge exists, nothing happens.
	 *
	 * @param uvw
	 *            HE_TextureCoordinate
	 * @param face
	 */
	public void setUVW(final HE_TextureCoordinate uvw, final HE_Face face) {
		HE_Halfedge he = getHalfedge(face);
		if (he != null) {
			he.setUVW(uvw);
		}

	}

	/**
	 * Clear UVW in halfedge in this vertex, belonging to face. If no such
	 * halfedge exists, nothing happens.
	 *
	 * @param face
	 */

	public void clearUVW(final HE_Face face) {
		HE_Halfedge he = getHalfedge(face);
		if (he != null) {
			he.clearUVW();
		}

	}

	/**
	 * Check if this vertex has a UVW for this face, either a halfedge UVW or a
	 * vertex UVW.
	 *
	 * @param f
	 * @return
	 */
	public boolean hasUVW(final HE_Face f) {
		final HE_Halfedge he = getHalfedge(f);
		if (he != null && he.hasHalfedgeUVW()) {
			return true;
		} else {
			return uvw != null;
		}
	}

	/**
	 * Check if this vertex has a vertex UVW.
	 *
	 * @return
	 */
	public boolean hasVertexUVW() {
		return uvw != null;
	}

	/**
	 * Check if this vertex has a halfedge UVW for this face.
	 *
	 * @param f
	 * @return
	 */
	public boolean hasHalfedgeUVW(final HE_Face f) {
		final HE_Halfedge he = getHalfedge(f);
		if (he != null && he.hasHalfedgeUVW()) {
			return true;
		}
		return false;
	}

	/**
	 * Get the vertex UVW. If none exists, return zero coordinates.
	 *
	 * @return
	 */
	public HE_TextureCoordinate getVertexUVW() {
		if (uvw == null) {
			return HE_TextureCoordinate.ZERO;
		}
		return uvw;
	}

	/**
	 * Get the halfedge UVW belonging to a face. If none exists, return zero
	 * coordinates.
	 *
	 * @param f
	 * @return
	 */
	public HE_TextureCoordinate getHalfedgeUVW(final HE_Face f) {
		final HE_Halfedge he = getHalfedge(f);
		if (he != null && he.hasHalfedgeUVW()) {
			return he.getUVW();
		} else {
			return HE_TextureCoordinate.ZERO;
		}
	}

	/**
	 * Get the UVW belonging to a face. If approprate halfedge UVW exists, the
	 * vertex UVW is retrieved.* If neither exist, zero coordinates are
	 * returned.
	 *
	 * @param f
	 * @return
	 */

	public HE_TextureCoordinate getUVW(final HE_Face f) {
		final HE_Halfedge he = getHalfedge(f);
		if (he != null) {
			return he.getUVW();
		}
		return uvw == null ? HE_TextureCoordinate.ZERO : uvw;
	}

	/**
	 *
	 */
	public void cleanUVW() {
		if (_halfedge == null) {
			return;
		}
		List<HE_Halfedge> halfedges = getHalfedgeStar();

		if (halfedges.size() == 0) {
			return;
		}
		int i = 0;
		while (!hasVertexUVW() && i < halfedges.size()) {
			if (halfedges.get(i).hasHalfedgeUVW()) {
				setUVW(halfedges.get(i).getHalfedgeUVW());
			}
			i++;
		}
		if (hasVertexUVW()) {
			for (HE_Halfedge he : halfedges) {
				if (he.hasHalfedgeUVW()) {
					if (he.getHalfedgeUVW().equals(getVertexUVW())) {
						he.clearUVW();
					}

				}
			}

		}

	}

	public boolean isNeighbor(final HE_Vertex v) {
		if (getHalfedge() == null) {
			return false;
		}
		HE_Halfedge he = getHalfedge();
		do {
			if (he.getEndVertex() == v) {
				return true;
			}
			he = he.getNextInVertex();
		} while (he != getHalfedge());
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#dot2D(wblut.geom.WB_Coordinate)
	 */
	@Override
	public double dot2D(final WB_Coord p) {
		return WB_GeometryOp3D.dot2D(xd(), yd(), p.xd(), p.yd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#absDot2D(wblut.geom.WB_Coordinate)
	 */
	@Override
	public double absDot2D(final WB_Coord p) {
		return WB_Math.fastAbs(WB_GeometryOp3D.dot2D(xd(), yd(), p.xd(), p.yd()));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#dot(wblut.geom.WB_Coordinate)
	 */
	@Override
	public double dot(final WB_Coord p) {
		return WB_GeometryOp3D.dot(xd(), yd(), zd(), p.xd(), p.yd(), p.zd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#absDot(wblut.geom.WB_Coordinate)
	 */
	@Override
	public double absDot(final WB_Coord p) {
		return WB_Math.fastAbs(WB_GeometryOp3D.dot(xd(), yd(), zd(), p.xd(), p.yd(), p.zd()));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMetric#isZero()
	 */
	@Override
	public boolean isZero() {
		return WB_GeometryOp3D.isZero3D(xd(), yd(), zd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#add(double, double, double)
	 */
	@Override
	public WB_Point add(final double... x) {
		if (x.length == 3) {
			return new WB_Point(this.xd() + x[0], this.yd() + x[1], this.zd() + x[2]);
		} else if (x.length == 2) {
			return new WB_Point(this.xd() + x[0], this.yd() + x[1], this.zd());

		}
		throw new IllegalArgumentException("Array should be length 2 or 3.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#addInto(double, double, double,
	 * wblut.geom.WB_MutableCoordinate)
	 */
	@Override
	public void addInto(final WB_MutableCoord result, final double... x) {
		if (x.length == 3) {
			result.set(xd() + x[0], yd() + x[1], zd() + x[2]);
		} else if (x.length == 2) {
			result.set(xd() + x[0], yd() + x[1], zd());
		} else {

			throw new IllegalArgumentException("Array should be length 2 or 3.");
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#add(wblut.geom.WB_Coordinate)
	 */
	@Override
	public WB_Point add(final WB_Coord p) {
		return new WB_Point(xd() + p.xd(), yd() + p.yd(), zd() + p.zd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#addInto(wblut.geom.WB_Coordinate,
	 * wblut.geom.WB_MutableCoordinate)
	 */
	@Override
	public void addInto(final WB_MutableCoord result, final WB_Coord p) {
		result.set(xd() + p.xd(), yd() + p.yd(), zd() + p.zd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#sub(double[])
	 */
	@Override
	public WB_Point sub(final double... x) {
		if (x.length == 3) {
			return new WB_Point(this.xd() - x[0], this.yd() - x[1], this.zd() - x[2]);
		} else if (x.length == 2) {
			return new WB_Point(this.xd() - x[0], this.yd() - x[1], this.zd());

		}
		throw new IllegalArgumentException("Array should be length 2 or 3.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#subInto(wblut.geom.WB_MutableCoord,
	 * double[])
	 */
	@Override
	public void subInto(final WB_MutableCoord result, final double... x) {
		if (x.length == 3) {
			result.set(xd() - x[0], yd() - x[1], zd() - x[2]);
		} else if (x.length == 2) {
			result.set(xd() - x[0], yd() - x[1], zd());
		} else {

			throw new IllegalArgumentException("Array should be length 2 or 3.");
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#sub(wblut.geom.WB_Coordinate)
	 */
	@Override
	public WB_Point sub(final WB_Coord p) {
		return new WB_Point(this.xd() - p.xd(), this.yd() - p.yd(), this.zd() - p.zd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#subInto(wblut.geom.WB_Coordinate,
	 * wblut.geom.WB_MutableCoordinate)
	 */
	@Override
	public void subInto(final WB_MutableCoord result, final WB_Coord p) {
		result.set(this.xd() - p.xd(), this.yd() - p.yd(), this.zd() - p.zd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#mul(double)
	 */
	@Override
	public WB_Point mul(final double f) {
		return new WB_Point(xd() * f, yd() * f, zd() * f);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#mulInto(double,
	 * wblut.geom.WB_MutableCoordinate)
	 */
	@Override
	public void mulInto(final WB_MutableCoord result, final double f) {
		scaleInto(result, f);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#div(double)
	 */
	@Override
	public WB_Point div(final double f) {
		return mul(1.0 / f);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#divInto(double,
	 * wblut.geom.WB_MutableCoordinate)
	 */
	@Override
	public void divInto(final WB_MutableCoord result, final double f) {
		mulInto(result, 1.0 / f);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#addMul(double, double, double, double)
	 */
	@Override
	public WB_Point addMul(final double f, final double... x) {
		if (x.length == 3) {
			return new WB_Point(this.xd() + f * x[0], this.yd() + f * x[1], this.zd() + f * x[2]);
		} else if (x.length == 2) {
			return new WB_Point(this.xd() + f * x[0], this.yd() + f * x[1], this.zd());
		}
		throw new IllegalArgumentException("Array should be length 2 or 3.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#addMulInto(double, double, double,
	 * double, wblut.geom.WB_MutableCoordinate)
	 */
	@Override
	public void addMulInto(final WB_MutableCoord result, final double f, final double... x) {
		if (x.length == 3) {
			result.set(xd() + f * x[0], yd() + f * x[1], zd() + f * x[2]);

		} else if (x.length == 2) {
			result.set(xd() + f * x[0], yd() + f * x[1], zd());
		} else {

			throw new IllegalArgumentException("Array should be length 2 or 3.");
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#addMul(double,
	 * wblut.geom.WB_Coordinate)
	 */
	@Override
	public WB_Point addMul(final double f, final WB_Coord p) {
		return new WB_Point(xd() + f * p.xd(), yd() + f * p.yd(), zd() + f * p.zd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#addMulInto(double,
	 * wblut.geom.WB_Coordinate, wblut.geom.WB_MutableCoordinate)
	 */
	@Override
	public void addMulInto(final WB_MutableCoord result, final double f, final WB_Coord p) {
		result.set(xd() + f * p.xd(), yd() + f * p.yd(), zd() + f * p.zd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#mulAddMul(double, double,
	 * wblut.geom.WB_Coordinate)
	 */
	@Override
	public WB_Point mulAddMul(final double f, final double g, final WB_Coord p) {
		return new WB_Point(f * xd() + g * p.xd(), f * yd() + g * p.yd(), f * zd() + g * p.zd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#mulAddMulInto(double, double,
	 * wblut.geom.WB_Coordinate, wblut.geom.WB_MutableCoordinate)
	 */
	@Override
	public void mulAddMulInto(final WB_MutableCoord result, final double f, final double g, final WB_Coord p) {
		result.set(f * xd() + g * p.xd(), f * yd() + g * p.yd(), f * zd() + g * p.zd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#mulAddMul(double, double, double[])
	 */
	@Override
	public WB_Point mulAddMul(final double f, final double g, final double... x) {
		if (x.length == 3) {
			return new WB_Point(f * this.xd() + g * x[0], f * this.yd() + g * x[1], f * this.zd() + g * x[2]);
		} else if (x.length == 2) {
			return new WB_Point(f * this.xd() + g * x[0], f * this.yd() + g * x[1], this.zd());
		}
		throw new IllegalArgumentException("Array should be length 2 or 3.");

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateMath#mulAddMulInto(wblut.geom.WB_MutableCoord,
	 * double, double, double[])
	 */
	@Override
	public void mulAddMulInto(final WB_MutableCoord result, final double f, final double g, final double... x) {
		if (x.length == 3) {
			result.set(f * this.xd() + g * x[0], f * this.yd() + g * x[1], f * this.zd() + g * x[2]);

		} else if (x.length == 2) {
			result.set(f * this.xd() + g * x[0], f * this.yd() + g * x[1], this.zd());

		} else {
			throw new IllegalArgumentException("Array should be length 2 or 3.");
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#cross(wblut.geom.WB_Coordinate)
	 */
	@Override
	public WB_Point cross(final WB_Coord p) {
		return new WB_Point(yd() * p.zd() - zd() * p.yd(), zd() * p.xd() - xd() * p.zd(),
				xd() * p.yd() - yd() * p.xd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#crossInto(wblut.geom.WB_Coordinate,
	 * wblut.geom.WB_MutableCoordinate)
	 */
	@Override
	public void crossInto(final WB_MutableCoord result, final WB_Coord p) {
		result.set(yd() * p.zd() - zd() * p.yd(), zd() * p.xd() - xd() * p.zd(), xd() * p.yd() - yd() * p.xd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#tensor(wblut.geom.WB_Coordinate)
	 */
	@Override
	public WB_M33 tensor(final WB_Coord v) {
		return new WB_M33(WB_GeometryOp3D.tensor3D(xd(), yd(), zd(), v.xd(), v.yd(), v.zd()));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMath#scalarTriple(wblut.geom.WB_Coordinate,
	 * wblut.geom.WB_Coordinate)
	 */
	@Override
	public double scalarTriple(final WB_Coord v, final WB_Coord w) {
		return WB_GeometryOp3D.scalarTriple(xd(), yd(), zd(), v.xd(), v.yd(), v.zd(), w.xd(), w.yd(), w.zd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_MutableCoordinateMath#addSelf(wblut.geom.WB_Coordinate)
	 */
	@Override
	public HE_Vertex addSelf(final WB_Coord p) {
		set(xd() + p.xd(), yd() + p.yd(), zd() + p.zd());
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinateMath#subSelf(double, double, double)
	 */
	@Override
	public HE_Vertex subSelf(final double... x) {
		if (x.length == 3) {
			set(xd() - x[0], yd() - x[1], zd() - x[2]);
			return this;
		} else if (x.length == 2) {
			set(xd() - x[0], yd() - x[1], zd());
			return this;
		}
		throw new IllegalArgumentException("Array should be length 2 or 3.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_MutableCoordinateMath#subSelf(wblut.geom.WB_Coordinate)
	 */
	@Override
	public HE_Vertex subSelf(final WB_Coord v) {
		set(xd() - v.xd(), yd() - v.yd(), zd() - v.zd());
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinateMath#mulSelf(double)
	 */
	@Override
	public HE_Vertex mulSelf(final double f) {
		set(f * xd(), f * yd(), f * zd());
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinateMath#divSelf(double)
	 */
	@Override
	public HE_Vertex divSelf(final double f) {
		return mulSelf(1.0 / f);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinateMath#addMulSelf(double, double,
	 * double, double)
	 */
	@Override
	public HE_Vertex addMulSelf(final double f, final double... x) {
		if (x.length == 3) {
			set(xd() + f * x[0], yd() + f * x[1], zd() + f * x[2]);
			return this;
		} else if (x.length == 2) {
			set(xd() + f * x[0], yd() + f * x[1], zd());
			return this;

		}

		throw new IllegalArgumentException("Array should be length 2 or 3.");

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinateMath#addMulSelf(double,
	 * wblut.geom.WB_Coordinate)
	 */
	@Override
	public HE_Vertex addMulSelf(final double f, final WB_Coord p) {
		set(xd() + f * p.xd(), yd() + f * p.yd(), zd() + f * p.zd());
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinateMath#addSelf(double, double, double)
	 */
	@Override
	public HE_Vertex addSelf(final double... x) {
		if (x.length == 3) {
			set(xd() + x[0], yd() + x[1], zd() + x[2]);
			return this;
		} else if (x.length == 2) {
			set(xd() + x[0], yd() + x[1], zd());
			return this;

		}

		throw new IllegalArgumentException("Array should be length 2 or 3.");

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinateMath#mulAddMulSelf(double, double,
	 * double[])
	 */
	@Override
	public HE_Vertex mulAddMulSelf(final double f, final double g, final double... x) {
		if (x.length == 3) {
			set(f * this.xd() + g * x[0], f * this.yd() + g * x[1], f * this.zd() + g * x[2]);
			return this;
		} else if (x.length == 2) {
			set(f * this.xd() + g * x[0], f * this.yd() + g * x[1], this.zd());
			return this;
		}
		throw new IllegalArgumentException("Array should be length 2 or 3.");

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinateMath#mulAddMulSelf(double, double,
	 * wblut.geom.WB_Coordinate)
	 */
	@Override
	public HE_Vertex mulAddMulSelf(final double f, final double g, final WB_Coord p) {
		set(f * xd() + g * p.xd(), f * yd() + g * p.yd(), f * zd() + g * p.zd());
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_MutableCoordinateMath#crossSelf(wblut.geom.WB_Coordinate)
	 */
	@Override
	public HE_Vertex crossSelf(final WB_Coord p) {
		set(yd() * p.zd() - this.zd() * p.yd(), this.zd() * p.xd() - this.xd() * p.zd(),
				this.xd() * p.yd() - yd() * p.xd());
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinateMath#normalizeSelf()
	 */
	@Override
	public double normalizeSelf() {
		final double d = getLength();
		if (WB_Epsilon.isZero(d)) {
			set(0, 0, 0);
		} else {
			set(xd() / d, yd() / d, zd() / d);
		}
		return d;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinateMath#trimSelf(double)
	 */
	@Override
	public HE_Vertex trimSelf(final double d) {
		if (getSqLength() > d * d) {
			normalizeSelf();
			mulSelf(d);
		}
		return this;
	}

	@Override
	public WB_Point apply2D(final WB_Transform2D T) {
		return T.applyAsPoint2D(this);
	}

	@Override
	public WB_Point applyAsPoint2D(final WB_Transform2D T) {
		return T.applyAsPoint2D(this);
	}

	@Override
	public WB_Vector applyAsVector2D(final WB_Transform2D T) {
		return T.applyAsVector2D(this);
	}

	@Override
	public WB_Vector applyAsNormal2D(final WB_Transform2D T) {
		return T.applyAsNormal2D(this);
	}

	@Override
	public void apply2DInto(final WB_MutableCoord result, final WB_Transform2D T) {
		T.applyAsPoint2DInto(this, result);
	}

	@Override
	public void applyAsPoint2DInto(final WB_MutableCoord result, final WB_Transform2D T) {
		T.applyAsPoint2DInto(this, result);
	}

	@Override
	public void applyAsVector2DInto(final WB_MutableCoord result, final WB_Transform2D T) {
		T.applyAsVector2DInto(this, result);
	}

	@Override
	public void applyAsNormal2DInto(final WB_MutableCoord result, final WB_Transform2D T) {
		T.applyAsNormal2DInto(this, result);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform2D#translate2D(double, double)
	 */
	@Override
	public WB_Coord translate2D(final double px, final double py) {
		return new WB_Point(this.xd() + px, this.yd() + py, 0);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform2D#translate2DInto(wblut.geom.
	 * WB_MutableCoord, double, double)
	 */
	@Override
	public void translate2DInto(final WB_MutableCoord result, final double px, final double py) {
		result.set(this.xd() + px, this.yd() + py, 0);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform2D#translate2D(wblut.geom.WB_Coord)
	 */
	@Override
	public WB_Coord translate2D(final WB_Coord p) {
		return new WB_Point(this.xd() + p.xd(), this.yd() + p.yd(), 0);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform2D#translate2DInto(wblut.geom.
	 * WB_MutableCoord, wblut.geom.WB_Coord)
	 */
	@Override
	public void translate2DInto(final WB_MutableCoord result, final WB_Coord p) {
		result.set(this.xd() + p.xd(), this.yd() + p.yd(), 0);
	}

	@Override

	public WB_Point rotateAboutPoint2D(final double angle, final double px, final double py) {
		final WB_Transform2D raa = new WB_Transform2D();
		raa.addRotateAboutPoint(angle, new WB_Point(px, py));
		WB_Point result = new WB_Point(this);
		raa.applyAsPoint2DSelf(result);
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateTransform2D#rotateAboutPoint2DInto(wblut.geom.
	 * WB_MutableCoord, double, double, double)
	 */
	@Override
	public void rotateAboutPoint2DInto(final WB_MutableCoord result, final double angle, final double px,
			final double py) {
		final WB_Transform2D raa = new WB_Transform2D();
		raa.addRotateAboutPoint(angle, new WB_Point(px, py));
		raa.applyAsPoint2DInto(this, result);
	}

	@Override
	public WB_Point rotateAboutPoint2D(final double angle, final WB_Coord p) {
		final WB_Transform2D raa = new WB_Transform2D();
		raa.addRotateAboutPoint(angle, p);
		WB_Point result = new WB_Point(this);
		raa.applyAsPoint2DSelf(result);
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateTransform2D#rotateAboutPoint2DInto(wblut.geom.
	 * WB_MutableCoord, double, wblut.geom.WB_Coord)
	 */
	@Override
	public void rotateAboutPoint2DInto(final WB_MutableCoord result, final double angle, final WB_Coord p) {
		final WB_Transform2D raa = new WB_Transform2D();
		raa.addRotateAboutPoint(angle, p);

		raa.applyAsPoint2DInto(this, result);

	}

	@Override
	public WB_Point rotateAboutOrigin2D(final double angle) {
		final WB_Transform2D raa = new WB_Transform2D();
		raa.addRotateAboutOrigin(angle);
		WB_Point result = new WB_Point(this);
		raa.applyAsPoint2DSelf(result);
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateTransform2D#rotateAboutOrigin2DInto(wblut.geom.
	 * WB_MutableCoord, double)
	 */
	@Override
	public void rotateAboutOrigin2DInto(final WB_MutableCoord result, final double angle) {
		final WB_Transform2D raa = new WB_Transform2D();
		raa.addRotateAboutOrigin(angle);

		raa.applyAsPoint2DInto(this, result);

	}

	@Override
	public WB_Point scale2D(final double f) {
		return mul(f);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateTransform#scaleInto(wblut.geom.WB_MutableCoord,
	 * double)
	 */
	@Override
	public void scale2DInto(final WB_MutableCoord result, final double f) {
		result.set(xd() * f, yd() * f, 0);
	}

	@Override
	public WB_Point scale2D(final double fx, final double fy) {
		return new WB_Point(xd() * fx, yd() * fy);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateTransform#scaleInto(wblut.geom.WB_MutableCoord,
	 * double, double, double)
	 */
	@Override
	public void scale2DInto(final WB_MutableCoord result, final double fx, final double fy) {
		result.set(xd() * fx, yd() * fy, 0);
	}

	@Override
	public HE_Vertex apply2DSelf(final WB_Transform2D T) {
		T.applyAsPoint2DSelf(this);
		return this;
	}

	@Override
	public HE_Vertex applyAsPoint2DSelf(final WB_Transform2D T) {
		T.applyAsPoint2DSelf(this);
		return this;
	}

	@Override
	public HE_Vertex applyAsVector2DSelf(final WB_Transform2D T) {
		T.applyAsVector2DSelf(this);
		return this;
	}

	@Override
	public HE_Vertex applyAsNormal2DSelf(final WB_Transform2D T) {
		T.applyAsNormal2DSelf(this);
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinateTransform3D#translate2DSelf(double,
	 * double)
	 */
	@Override
	public WB_Coord translate2DSelf(final double px, final double py) {
		set(xd() + px, yd() + py);
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_MutableCoordinateTransform3D#translate2DSelf(wblut.geom.
	 * WB_Coord)
	 */
	@Override
	public WB_Coord translate2DSelf(final WB_Coord p) {
		set(xd() + p.xd(), yd() + p.yd());
		return this;
	}

	@Override
	public HE_Vertex rotateAboutPoint2DSelf(final double angle, final double px, final double py) {
		final WB_Transform2D raa = new WB_Transform2D();
		raa.addRotateAboutPoint(angle, new WB_Vector(px, py));
		raa.applyAsPoint2DSelf(this);
		return this;
	}

	@Override
	/**
	 *
	 * @param angle
	 * @param p
	 * @return
	 */
	public HE_Vertex rotateAboutPoint2DSelf(final double angle, final WB_Coord p) {
		final WB_Transform2D raa = new WB_Transform2D();
		raa.addRotateAboutPoint(angle, p);
		raa.applyAsPoint2DSelf(this);
		return this;
	}

	@Override
	public HE_Vertex rotateAboutOrigin2DSelf(final double angle) {
		final WB_Transform2D raa = new WB_Transform2D();
		raa.addRotateAboutOrigin(angle);
		raa.applyAsPoint2DSelf(this);
		return this;
	}

	@Override
	public HE_Vertex scale2DSelf(final double f) {
		mulSelf(f);
		return this;
	}

	@Override
	public HE_Vertex scale2DSelf(final double fx, final double fy) {
		set(xd() * fx, yd() * fy);
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform#apply(wblut.geom.WB_Transform)
	 */
	@Override
	public WB_Point apply(final WB_Transform T) {
		return T.applyAsPoint(this);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateTransform#applyAsPoint(wblut.geom.WB_Transform)
	 */
	@Override
	public WB_Point applyAsPoint(final WB_Transform T) {
		return T.applyAsPoint(this);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateTransform#applyAsNormal(wblut.geom.WB_Transform)
	 */
	@Override
	public WB_Vector applyAsNormal(final WB_Transform T) {
		return T.applyAsNormal(this);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateTransform#applyAsVector(wblut.geom.WB_Transform)
	 */
	@Override
	public WB_Vector applyAsVector(final WB_Transform T) {
		return T.applyAsVector(this);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform#applyInto(wblut.geom.WB_Transform,
	 * wblut.geom.WB_MutableCoordinate)
	 */
	@Override
	public void applyInto(final WB_MutableCoord result, final WB_Transform T) {
		T.applyAsPointInto(result, this);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform#applyAsPointInto(wblut.geom.
	 * WB_Transform , wblut.geom.WB_MutableCoordinate)
	 */
	@Override
	public void applyAsPointInto(final WB_MutableCoord result, final WB_Transform T) {
		T.applyAsPointInto(this, result);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform#applyAsNormalInto(wblut.geom.
	 * WB_Transform , wblut.geom.WB_MutableCoordinate)
	 */
	@Override
	public void applyAsNormalInto(final WB_MutableCoord result, final WB_Transform T) {
		T.applyAsNormalInto(this, result);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform#applyAsVectorInto(wblut.geom.
	 * WB_Transform , wblut.geom.WB_MutableCoordinate)
	 */
	@Override
	public void applyAsVectorInto(final WB_MutableCoord result, final WB_Transform T) {
		T.applyAsVectorInto(this, result);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform3D#translate(double, double)
	 */
	@Override
	public WB_Coord translate(final double px, final double py, final double pz) {
		return new WB_Point(this.xd() + px, this.yd() + py, this.zd() + pz);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform3D#translateInto(wblut.geom.
	 * WB_MutableCoord, double, double)
	 */
	@Override
	public WB_Coord translateInto(final WB_MutableCoord result, final double px, final double py, final double pz) {
		result.set(xd() + px, yd() + py, zd() + pz);
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform3D#translate(wblut.geom.WB_Coord)
	 */
	@Override
	public WB_Coord translate(final WB_Coord p) {
		return new WB_Point(xd() + p.xd(), yd() + p.yd(), zd() + p.zd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform3D#translateInto(wblut.geom.
	 * WB_MutableCoord, wblut.geom.WB_Coord)
	 */
	@Override
	public WB_Coord translateInto(final WB_MutableCoord result, final WB_Coord p) {
		result.set(xd() + p.xd(), yd() + p.yd(), zd() + p.zd());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform#rotateAbout2PointAxis(double,
	 * double, double, double, double, double, double)
	 */
	@Override
	public WB_Point rotateAboutAxis2P(final double angle, final double p1x, final double p1y, final double p1z,
			final double p2x, final double p2y, final double p2z) {
		final WB_Point result = new WB_Point(this);
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutAxis(angle, new WB_Vector(p1x, p1y, p1z), new WB_Vector(p2x - p1x, p2y - p1y, p2z - p1z));
		raa.applyAsPointSelf(result);
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateTransform3D#rotateAboutAxis2PInto(wblut.geom.
	 * WB_MutableCoord, double, double, double, double, double, double, double)
	 */
	@Override
	public void rotateAboutAxis2PInto(final WB_MutableCoord result, final double angle, final double p1x,
			final double p1y, final double p1z, final double p2x, final double p2y, final double p2z) {
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutAxis(angle, new WB_Vector(p1x, p1y, p1z), new WB_Vector(p2x - p1x, p2y - p1y, p2z - p1z));
		raa.applyAsPointInto(result, this);

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform#rotateAbout2PointAxis(double,
	 * wblut.geom.WB_Coordinate, wblut.geom.WB_Coordinate)
	 */
	@Override
	/**
	 *
	 * @param angle
	 * @param p1
	 * @param p2
	 * @return
	 */
	public WB_Point rotateAboutAxis2P(final double angle, final WB_Coord p1, final WB_Coord p2) {
		final WB_Point result = new WB_Point(this);
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutAxis(angle, p1, new WB_Vector(p1, p2));
		raa.applyAsPointSelf(result);
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateTransform3D#rotateAboutAxis2PInto(wblut.geom.
	 * WB_MutableCoord, double, wblut.geom.WB_Coord, wblut.geom.WB_Coord)
	 */
	@Override
	public void rotateAboutAxis2PInto(final WB_MutableCoord result, final double angle, final WB_Coord p1,
			final WB_Coord p2) {

		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutAxis(angle, p1, new WB_Vector(p1, p2));
		raa.applyAsPointInto(this, result);

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_MutableCoordinateTransform#rotateAbout2PointAxisSelf(double
	 * , double, double, double, double, double, double)
	 */
	@Override
	public WB_Point rotateAboutAxis(final double angle, final double px, final double py, final double pz,
			final double ax, final double ay, final double az) {
		final WB_Point result = new WB_Point(this);
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutAxis(angle, new WB_Vector(px, py, pz), new WB_Vector(ax, ay, az));
		raa.applyAsPointSelf(result);
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform3D#rotateAboutAxisInto(wblut.geom.
	 * WB_MutableCoord, double, double, double, double, double, double, double)
	 */
	@Override
	public void rotateAboutAxisInto(final WB_MutableCoord result, final double angle, final double px, final double py,
			final double pz, final double ax, final double ay, final double az) {

		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutAxis(angle, new WB_Vector(px, py, pz), new WB_Vector(ax, ay, az));
		raa.applyAsPointInto(this, result);

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform#rotateAboutAxis(double,
	 * wblut.geom.WB_Coordinate, wblut.geom.WB_Coordinate)
	 */
	@Override
	public WB_Point rotateAboutAxis(final double angle, final WB_Coord p, final WB_Coord a) {
		final WB_Point result = new WB_Point(this);
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutAxis(angle, p, a);
		raa.applyAsPointSelf(result);
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform3D#rotateAboutAxisInto(wblut.geom.
	 * WB_MutableCoord, double, wblut.geom.WB_Coord, wblut.geom.WB_Coord)
	 */
	@Override
	public void rotateAboutAxisInto(final WB_MutableCoord result, final double angle, final WB_Coord p,
			final WB_Coord a) {
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutAxis(angle, p, a);
		raa.applyAsPointInto(this, result);

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_MutableCoordinateTransform#rotateAbout2PointAxisSelf(double
	 * , double, double, double, double, double, double)
	 */
	@Override
	public WB_Point rotateAboutOrigin(final double angle, final double x, final double y, final double z) {
		final WB_Point result = new WB_Point(this);
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutOrigin(angle, new WB_Vector(x, y, z));
		raa.applyAsPointSelf(result);
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateTransform3D#rotateAboutOriginInto(wblut.geom.
	 * WB_MutableCoord, double, double, double, double)
	 */
	@Override
	public void rotateAboutOriginInto(final WB_MutableCoord result, final double angle, final double x, final double y,
			final double z) {
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutOrigin(angle, new WB_Vector(x, y, z));
		raa.applyAsPointInto(this, result);

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform#rotateAboutAxis(double,
	 * wblut.geom.WB_Coordinate, wblut.geom.WB_Coordinate)
	 */
	@Override
	public WB_Point rotateAboutOrigin(final double angle, final WB_Coord a) {
		final WB_Point result = new WB_Point(this);
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutOrigin(angle, a);
		raa.applyAsPointSelf(result);
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateTransform3D#rotateAboutOriginInto(wblut.geom.
	 * WB_MutableCoord, double, wblut.geom.WB_Coord)
	 */
	@Override
	public void rotateAboutOriginInto(final WB_MutableCoord result, final double angle, final WB_Coord v) {
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutOrigin(angle, v);
		raa.applyAsPointInto(this, result);

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform#scale(double)
	 */
	@Override
	/**
	 *
	 * @param f
	 * @return
	 */
	public WB_Point scale(final double f) {
		return mul(f);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateTransform#scaleInto(wblut.geom.WB_MutableCoord,
	 * double)
	 */
	@Override
	public void scaleInto(final WB_MutableCoord result, final double f) {
		result.set(xd() * f, yd() * f, zd() * f);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform#scale(double, double, double)
	 */
	@Override
	/**
	 *
	 * @param fx
	 * @param fy
	 * @param fz
	 * @return
	 */
	public WB_Point scale(final double fx, final double fy, final double fz) {
		return new WB_Point(xd() * fx, yd() * fy, zd() * fz);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateTransform#scaleInto(wblut.geom.WB_MutableCoord,
	 * double, double, double)
	 */
	@Override
	public void scaleInto(final WB_MutableCoord result, final double fx, final double fy, final double fz) {
		result.set(xd() * fx, yd() * fy, zd() * fz);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_MutableCoordinateMath#applySelf(wblut.geom.WB_Transform)
	 */
	@Override
	public HE_Vertex applySelf(final WB_Transform T) {
		return applyAsPointSelf(T);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinateMath#applyAsPointSelf(wblut.geom.
	 * WB_Transform )
	 */
	@Override
	public HE_Vertex applyAsPointSelf(final WB_Transform T) {
		T.applyAsPointSelf(this);
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinateMath#applyAsVectorSelf(wblut.geom.
	 * WB_Transform )
	 */
	@Override
	public HE_Vertex applyAsVectorSelf(final WB_Transform T) {
		T.applyAsVectorSelf(this);
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinateMath#applyAsNormalSelf(wblut.geom.
	 * WB_Transform )
	 */
	@Override
	public HE_Vertex applyAsNormalSelf(final WB_Transform T) {
		T.applyAsNormalSelf(this);
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinateTransform3D#translateSelf(double,
	 * double, double)
	 */
	@Override
	public WB_Coord translateSelf(final double px, final double py, final double pz) {
		set(xd() + px, yd() + py, zd() + pz);
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinateTransform3D#translateSelf(wblut.geom.
	 * WB_Coord)
	 */
	@Override
	public WB_Coord translateSelf(final WB_Coord p) {
		set(xd() + p.xd(), yd() + p.yd(), zd() + p.zd());
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_MutableCoordinateTransform#rotateAbout2PointAxisSelf(double
	 * , double, double, double, double, double, double)
	 */
	@Override
	public HE_Vertex rotateAboutAxis2PSelf(final double angle, final double p1x, final double p1y, final double p1z,
			final double p2x, final double p2y, final double p2z) {
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutAxis(angle, new WB_Vector(p1x, p1y, p1z), new WB_Vector(p2x - p1x, p2y - p1y, p2z - p1z));
		raa.applyAsPointSelf(this);
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_MutableCoordinateTransform#rotateAbout2PointAxisSelf(double
	 * , wblut.geom.WB_Coordinate, wblut.geom.WB_Coordinate)
	 */
	@Override
	public HE_Vertex rotateAboutAxis2PSelf(final double angle, final WB_Coord p1, final WB_Coord p2) {
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutAxis(angle, p1, new WB_Vector(p1, p2));
		raa.applyAsPointSelf(this);
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinateTransform#rotateAboutAxisSelf(double,
	 * wblut.geom.WB_Coordinate, wblut.geom.WB_Coordinate)
	 */
	@Override
	public HE_Vertex rotateAboutAxisSelf(final double angle, final WB_Coord p, final WB_Coord a) {
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutAxis(angle, p, a);
		raa.applyAsPointSelf(this);
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_MutableCoordinateTransform#rotateAbout2PointAxisSelf(double
	 * , double, double, double, double, double, double)
	 */
	@Override
	public HE_Vertex rotateAboutAxisSelf(final double angle, final double px, final double py, final double pz,
			final double ax, final double ay, final double az) {
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutAxis(angle, new WB_Vector(px, py, pz), new WB_Vector(ax, ay, az));
		raa.applyAsPointSelf(this);
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_MutableCoordinateTransform#rotateAbout2PointAxisSelf(double
	 * , double, double, double, double, double, double)
	 */
	@Override
	public HE_Vertex rotateAboutOriginSelf(final double angle, final double x, final double y, final double z) {

		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutOrigin(angle, new WB_Vector(x, y, z));
		raa.applyAsPointSelf(this);
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateTransform#rotateAboutAxis(double,
	 * wblut.geom.WB_Coordinate, wblut.geom.WB_Coordinate)
	 */
	@Override
	public HE_Vertex rotateAboutOriginSelf(final double angle, final WB_Coord a) {

		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutOrigin(angle, a);
		raa.applyAsPointSelf(this);
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinateTransform#scaleSelf(double)
	 */
	@Override
	public HE_Vertex scaleSelf(final double f) {
		mulSelf(f);
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_MutableCoordinateTransform#scaleSelf(double, double,
	 * double)
	 */
	@Override
	public HE_Vertex scaleSelf(final double fx, final double fy, final double fz) {
		set(xd() * fx, yd() * fy, zd() * fz);
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateMetric#getDistance2D(wblut.geom.WB_Coordinate)
	 */
	@Override
	public double getDistance2D(final WB_Coord p) {
		return WB_GeometryOp3D.getDistance2D(xd(), yd(), p.xd(), p.yd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateMetric#getSqDistance2D(wblut.geom.WB_Coordinate)
	 */
	@Override
	public double getSqDistance2D(final WB_Coord p) {
		return WB_GeometryOp3D.getSqDistance2D(xd(), yd(), p.xd(), p.yd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMetric#getLength2D()
	 */
	@Override
	public double getLength2D() {
		return WB_GeometryOp3D.getLength2D(xd(), yd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMetric#getSqLength2D()
	 */
	@Override
	public double getSqLength2D() {
		return WB_GeometryOp3D.getSqLength2D(xd(), yd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMetric#heading2D()
	 */
	@Override
	public double getHeading2D() {
		return Math.atan2(yd(), xd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMetric#getOrthoNormal2D()
	 */
	@Override
	public WB_Vector getOrthoNormal2D() {
		final WB_Vector a = new WB_Vector(-yd(), xd(), 0);
		a.normalizeSelf();
		return a;
	}

	/**
	 *
	 *
	 * @param p
	 * @param q
	 * @return
	 */
	@Override
	public boolean isCollinear2D(final WB_Coord p, final WB_Coord q) {
		return WB_GeometryOp3D.isCollinear2D(this, p, q);
	}

	/**
	 *
	 *
	 * @param p
	 * @return
	 */
	@Override
	public boolean isParallel2D(final WB_Coord p) {
		return WB_GeometryOp3D.isParallel2D(this, p);
	}

	/**
	 *
	 *
	 * @param p
	 * @param t
	 * @return
	 */
	@Override
	public boolean isParallel2D(final WB_Coord p, final double t) {
		return WB_GeometryOp3D.isParallel2D(this, p, t);
	}

	/**
	 *
	 *
	 * @param p
	 * @return
	 */
	@Override
	public boolean isParallelNorm2D(final WB_Coord p) {
		return WB_GeometryOp3D.isParallelNorm2D(this, p);
	}

	/**
	 *
	 *
	 * @param p
	 * @param t
	 * @return
	 */
	@Override
	public boolean isParallelNorm2D(final WB_Coord p, final double t) {
		return WB_GeometryOp3D.isParallelNorm2D(this, p, t);
	}

	/**
	 *
	 *
	 * @param p
	 * @return
	 */
	@Override
	public boolean isOrthogonal2D(final WB_Coord p) {
		return WB_GeometryOp3D.isOrthogonal2D(this, p);
	}

	/**
	 *
	 *
	 * @param p
	 * @param t
	 * @return
	 */
	@Override
	public boolean isOrthogonal2D(final WB_Coord p, final double t) {
		return WB_GeometryOp3D.isOrthogonal2D(this, p, t);
	}

	/**
	 *
	 *
	 * @param p
	 * @return
	 */
	@Override
	public boolean isOrthogonalNorm2D(final WB_Coord p) {
		return WB_GeometryOp3D.isOrthogonalNorm2D(this, p);
	}

	/**
	 *
	 *
	 * @param p
	 * @param t
	 * @return
	 */
	@Override
	public boolean isOrthogonalNorm2D(final WB_Coord p, final double t) {
		return WB_GeometryOp3D.isOrthogonalNorm2D(this, p, t);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateMetric#getDistance3D(wblut.geom.WB_Coordinate)
	 */

	@Override
	/**
	 *
	 * @param p
	 * @return
	 */
	public double getDistance3D(final WB_Coord p) {
		return getDistance(p);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateMetric#getDistance3D(wblut.geom.WB_Coordinate)
	 */
	@Override
	public double getDistance(final WB_Coord p) {
		return WB_GeometryOp3D.getDistance3D(xd(), yd(), zd(), p.xd(), p.yd(), p.zd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateMetric#getSqDistance3D(wblut.geom.WB_Coordinate)
	 */

	@Override
	/**
	 *
	 * @param p
	 * @return
	 */
	public double getSqDistance3D(final WB_Coord p) {
		return getSqDistance(p);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateMetric#getSqDistance3D(wblut.geom.WB_Coordinate)
	 */
	@Override
	public double getSqDistance(final WB_Coord p) {
		return WB_GeometryOp3D.getSqDistance3D(xd(), yd(), zd(), p.xd(), p.yd(), p.zd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMetric#getLength3D()
	 */

	@Override
	public double getLength3D() {
		return getLength();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMetric#getLength3D()
	 */
	@Override
	public double getLength() {
		return WB_GeometryOp3D.getLength3D(xd(), yd(), zd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMetric#getSqLength3D()
	 */

	@Override
	public double getSqLength3D() {
		return getSqLength();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMetric#getSqLength3D()
	 */
	@Override
	public double getSqLength() {
		return WB_GeometryOp3D.getSqLength3D(xd(), yd(), zd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMetric#getAngle(wblut.geom.WB_Coordinate)
	 */
	@Override
	public double getAngle(final WB_Coord p) {
		return WB_GeometryOp3D.getAngleBetween(xd(), yd(), zd(), p.xd(), p.yd(), p.zd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * wblut.geom.WB_CoordinateMetric#getAngleNorm(wblut.geom.WB_Coordinate)
	 */
	@Override
	public double getAngleNorm(final WB_Coord p) {
		return WB_GeometryOp3D.getAngleBetweenNorm(xd(), yd(), zd(), p.xd(), p.yd(), p.zd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMetric#getOrthoNormal3D()
	 */
	@Override
	public WB_Vector getOrthoNormal3D() {
		return getOrthoNormal();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.WB_CoordinateMetric#getOrthoNormal3D()
	 */
	@Override
	public WB_Vector getOrthoNormal() {
		if (Math.abs(zd()) > WB_Epsilon.EPSILON) {
			final WB_Vector a = new WB_Vector(1, 0, -xd() / zd());
			a.normalizeSelf();
			return a;
		} else {
			return new WB_Vector(0, 0, 1);
		}
	}

	/**
	 *
	 *
	 * @param p
	 * @param q
	 * @return
	 */
	@Override
	public boolean isCollinear(final WB_Coord p, final WB_Coord q) {
		return WB_GeometryOp3D.isCollinear(this, p, q);
	}

	/**
	 *
	 *
	 * @param p
	 * @return
	 */
	@Override
	public boolean isParallel(final WB_Coord p) {
		return WB_GeometryOp3D.isParallel(this, p);
	}

	/**
	 *
	 *
	 * @param p
	 * @param t
	 * @return
	 */
	@Override
	public boolean isParallel(final WB_Coord p, final double t) {
		return WB_GeometryOp3D.isParallel(this, p, t);
	}

	/**
	 *
	 *
	 * @param p
	 * @return
	 */
	@Override
	public boolean isParallelNorm(final WB_Coord p) {
		return WB_GeometryOp3D.isParallelNorm(this, p);
	}

	/**
	 *
	 *
	 * @param p
	 * @param t
	 * @return
	 */
	@Override
	public boolean isParallelNorm(final WB_Coord p, final double t) {
		return WB_GeometryOp3D.isParallelNorm(this, p, t);
	}

	/**
	 *
	 *
	 * @param p
	 * @return
	 */
	@Override
	public boolean isOrthogonal(final WB_Coord p) {
		return WB_GeometryOp3D.isOrthogonal(this, p);
	}

	/**
	 *
	 *
	 * @param p
	 * @param t
	 * @return
	 */
	@Override
	public boolean isOrthogonal(final WB_Coord p, final double t) {
		return WB_GeometryOp3D.isOrthogonal(this, p, t);
	}

	/**
	 *
	 *
	 * @param p
	 * @return
	 */
	@Override
	public boolean isOrthogonalNorm(final WB_Coord p) {
		return WB_GeometryOp3D.isOrthogonalNorm(this, p);
	}

	/**
	 *
	 *
	 * @param p
	 * @param t
	 * @return
	 */
	@Override
	public boolean isOrthogonalNorm(final WB_Coord p, final double t) {
		return WB_GeometryOp3D.isOrthogonalNorm(this, p, t);
	}

	/**
	 *
	 */
	public void invert() {
		mulSelf(-1);
	}

	/**
	 *
	 *
	 * @return
	 */
	public double[] coords() {
		return new double[] { xd(), yd(), zd() };
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(final WB_Coord p) {
		int cmp = Double.compare(xd(), p.xd());
		if (cmp != 0) {
			return cmp;
		}
		cmp = Double.compare(yd(), p.yd());
		if (cmp != 0) {
			return cmp;
		}
		cmp = Double.compare(zd(), p.zd());
		if (cmp != 0) {
			return cmp;
		}
		return Double.compare(wd(), p.wd());
	}

	/**
	 *
	 *
	 * @param p
	 * @return
	 */

	public int compareToY1st(final WB_Coord p) {
		int cmp = Double.compare(yd(), p.yd());
		if (cmp != 0) {
			return cmp;
		}
		cmp = Double.compare(xd(), p.xd());
		if (cmp != 0) {
			return cmp;
		}
		cmp = Double.compare(zd(), p.zd());
		if (cmp != 0) {
			return cmp;
		}
		return Double.compare(wd(), p.wd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object o) {
		if (o == null) {
			return false;
		}
		if (o == this) {
			return true;
		}
		if (!(o instanceof HE_Vertex)) {
			return false;
		}
		final HE_Vertex p = (HE_Vertex) o;
		if (!WB_Epsilon.isEqualAbs(xd(), p.xd())) {
			return false;
		}
		if (!WB_Epsilon.isEqualAbs(yd(), p.yd())) {
			return false;
		}
		if (!WB_Epsilon.isEqualAbs(zd(), p.zd())) {
			return false;
		}
		if (!WB_Epsilon.isEqualAbs(wd(), p.wd())) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return WB_HashCode.calculateHashCode(xd(), yd(), zd());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.Point3D#toString()
	 */
	@Override
	public String toString() {
		return "HE_Vertex key: " + key() + " [x=" + xd() + ", y=" + yd() + ", z=" + zd() + "]" + " (" + getUserLabel()
				+ "," + getInternalLabel() + ")";
	}

	@Override
	public void clearPrecomputed() {

	}

}
