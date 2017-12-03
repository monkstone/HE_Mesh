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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap;

import wblut.core.WB_ProgressReporter.WB_ProgressCounter;
import wblut.geom.WB_AABB;
import wblut.geom.WB_Classification;
import wblut.geom.WB_Coord;
import wblut.geom.WB_CoordCollection;
import wblut.geom.WB_Frame;
import wblut.geom.WB_GeometryOp3D;
import wblut.geom.WB_KDTree;
import wblut.geom.WB_KDTree.WB_KDEntry;
import wblut.geom.WB_Mesh;
import wblut.geom.WB_MeshCreator;
import wblut.geom.WB_Plane;
import wblut.geom.WB_Point;
import wblut.geom.WB_Polygon;
import wblut.geom.WB_Transform;
import wblut.geom.WB_TriangleGenerator;
import wblut.geom.WB_Vector;
import wblut.hemesh.HE_RAS.HE_RASEC;
import wblut.math.WB_Epsilon;
import wblut.math.WB_MTRandom;

/**
 * Half-edge mesh data structure.
 *
 * @author Frederik Vanhoutte (W:Blut)
 *
 */
public class HE_Mesh extends HE_MeshStructure implements WB_TriangleGenerator {
	Future<HE_Mesh> future;
	ExecutorService executor;
	LinkedList<Callable<HE_Mesh>> tasks;
	Map<String, HE_Selection> selections;
	boolean finished;
	int[] triangles;

	/**
	 * Instantiates a new HE_Mesh.
	 *
	 */
	public HE_Mesh() {
		super();
		selections = new UnifiedMap<String, HE_Selection>();
		tasks = new LinkedList<Callable<HE_Mesh>>();
		future = null;
		executor = null;
		finished = true;
		triangles = null;
	}

	public void createThreaded(final HEC_Creator creator) {
		tasks.add(new CreatorThread(creator));
	}

	public void modifyThreaded(final HEM_Modifier modifier) {
		tasks.add(new ModifierThread(modifier, this));
	}

	public void subdivideThreaded(final HES_Subdividor subdividor) {
		tasks.add(new SubdividorThread(subdividor, this));
	}

	public void subdivideThreaded(final HES_Subdividor subdividor, final int rep) {
		for (int i = 0; i < rep; i++) {
			tasks.add(new SubdividorThread(subdividor, this));
		}
	}

	public void simplifyThreaded(final HES_Simplifier simplifier) {
		tasks.add(new SimplifierThread(simplifier, this));
	}

	public void update() {
		if (future == null) {
			if (tasks.size() > 0) {
				if (executor == null) {
					executor = Executors.newFixedThreadPool(1);
				}
				future = executor.submit(tasks.removeFirst());
				finished = false;
			} else {
				if (executor != null) {
					executor.shutdown();
				}
				executor = null;

			}
		} else if (future.isDone()) {
			try {
				HE_Mesh result = future.get();
				if (result != this) {// HEM_Modify returns this if modification
										// of copy failed.
					setNoCopy(result);
				}
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			future = null;
			finished = true;
		} else if (future.isCancelled()) {
			future = null;
			finished = true;
		}

	}

	/**
	 * Constructor.
	 *
	 * @param creator
	 *            HE_Creator that generates this mesh
	 */
	public HE_Mesh(final HEC_Creator creator) {
		this();
		setNoCopy(creator.create());
		triangles = null;
	}

	/**
	 *
	 *
	 * @param mesh
	 */
	public HE_Mesh(final WB_Mesh mesh) {
		this(new HEC_FromWBMesh(mesh));
		triangles = null;
	}

	/**
	 *
	 *
	 * @param mesh
	 */
	public HE_Mesh(final WB_MeshCreator mesh) {
		this(new HEC_FromWBMesh(mesh.create()));
		triangles = null;
	}

	/**
	 *
	 *
	 * @param mesh
	 */
	public HE_Mesh(final HE_Mesh mesh) {
		this();
		set(mesh);
		triangles = null;
	}

	/**
	 * Deep copy of mesh.
	 *
	 * @return copy as new HE_Mesh
	 */
	public HE_Mesh copy() {
		return new HE_Mesh(new HEC_Copy(this));
	}

	/**
	 * Deep copy of mesh.
	 *
	 * @return copy as new HE_Mesh
	 */
	public HE_Mesh get() {
		return new HE_Mesh(new HEC_Copy(this));
	}

	/**
	 * Replace mesh with deep copy of target.
	 *
	 * @param target
	 *            HE_Mesh to be duplicated
	 */
	public void set(final HE_Mesh target) {

		final HE_Mesh result = target.copy();
		replaceVertices(result);
		replaceFaces(result);
		replaceHalfedges(result);
		selections = target.selections;

	}

	/**
	 * Replace mesh with shallow copy of target.
	 *
	 * @param target
	 *            HE_Mesh to be duplicated
	 */
	void setNoCopy(final HE_Mesh target) {
		synchronized (this) {
			replaceVertices(target);
			replaceFaces(target);
			replaceHalfedges(target);
			selections = target.selections;
			for (String name : getSelectionNames()) {
				getSelection(name).parent = this;
			}
		}

	}

	/**
	 *
	 *
	 * @param mesh
	 */
	private final void replaceFaces(final HE_Mesh mesh) {
		clearFaces();
		addFaces(mesh);
	}

	/**
	 *
	 *
	 * @param mesh
	 */
	private final void replaceVertices(final HE_Mesh mesh) {
		clearVertices();
		addVertices(mesh);
	}

	/**
	 *
	 *
	 * @param mesh
	 */
	private final void replaceHalfedges(final HE_Mesh mesh) {
		clearHalfedges();
		HE_HalfedgeIterator heItr = mesh.heItr();
		while (heItr.hasNext()) {
			add(heItr.next());

		}

	}

	/**
	 * Modify the mesh.
	 *
	 * @param modifier
	 *            HE_Modifier to apply
	 * @return self
	 */
	public HE_Mesh modify(final HEM_Modifier modifier) {
		if (finished) {
			modifier.apply(this);
			clearPrecomputed();
		} else {
			modifyThreaded(modifier);

		}
		return this;
	}

	/**
	 * Subdivide the mesh.
	 *
	 * @param subdividor
	 *            HE_Subdividor to apply
	 * @return self
	 */
	public HE_Mesh subdivide(final HES_Subdividor subdividor) {
		if (finished) {
			subdividor.apply(this);
			clearPrecomputed();
		} else {
			subdivideThreaded(subdividor);
		}
		return this;
	}

	/**
	 * Subdivide the mesh a number of times.
	 *
	 * @param subdividor
	 *            HE_Subdividor to apply
	 * @param rep
	 *            subdivision iterations. WARNING: higher values will lead to
	 *            unmanageable number of faces.
	 * @return self
	 */
	public HE_Mesh subdivide(final HES_Subdividor subdividor, final int rep) {
		if (finished) {
			for (int i = 0; i < rep; i++) {
				subdividor.apply(this);
				clearPrecomputed();
			}
		} else {
			for (int i = 0; i < rep; i++) {
				subdivideThreaded(subdividor);
			}
		}
		return this;
	}

	/**
	 * Smooth.
	 */
	public void smooth() {
		if (finished) {
			subdivide(new HES_CatmullClark());
		} else {
			subdivideThreaded(new HES_CatmullClark());
		}
	}

	/**
	 *
	 *
	 * @param rep
	 */
	public void smooth(final int rep) {
		if (finished) {
			subdivide(new HES_CatmullClark(), rep);
		} else {
			for (int i = 0; i < rep; i++) {
				subdivideThreaded(new HES_CatmullClark());
			}
		}
	}

	/**
	 * Simplify.
	 *
	 * @param simplifier
	 *            the simplifier
	 * @return the h e_ mesh
	 */
	public HE_Mesh simplify(final HES_Simplifier simplifier) {
		if (finished) {
			simplifier.apply(this);
			clearPrecomputed();
		} else {
			simplifyThreaded(simplifier);
		}
		return this;
	}

	/**
	 * Add all mesh elements to this mesh. No copies are made. Tries to join
	 * geometry.
	 *
	 * @param mesh
	 *            mesh to add
	 */
	public void fuse(final HE_Mesh mesh) {

		addVertices(mesh.getVerticesAsArray());
		addFaces(mesh.getFacesAsArray());
		addHalfedges(mesh.getHalfedgesAsArray());
		setNoCopy(new HE_Mesh(new HEC_FromPolygons().setPolygons(this.getPolygonList())));
	}

	/**
	 *
	 *
	 * @return
	 */
	public WB_Mesh toFacelistMesh() {
		return gf.createMesh(getVerticesAsCoord(), getFacesAsInt());
	}

	/**
	 * Gets the frame.
	 *
	 * @return the frame
	 */
	public WB_Frame getFrame() {
		final WB_Frame frame = new WB_Frame(getVerticesAsCoord());
		final LongIntHashMap map = new LongIntHashMap();
		Map<Long, Integer> indexMap = getVertexKeyToIndexMap();
		for (Entry<Long, Integer> entry : indexMap.entrySet()) {
			map.put(entry.getKey(), entry.getValue());
		}
		final Iterator<HE_Halfedge> eItr = eItr();
		HE_Halfedge e;
		while (eItr.hasNext()) {
			e = eItr.next();
			frame.addStrut(map.get(e.getVertex().key()), map.get(e.getEndVertex().key()));
		}
		return frame;
	}

	public HE_Mesh apply(final WB_Transform T) {
		return new HEC_Transform(this, T).create();
	}

	/**
	 *
	 * @param T
	 * @return
	 */
	public HE_Mesh applySelf(final WB_Transform T) {

		return modify(new HEM_Transform(T));
	}

	/**
	 * Apply transform to entire mesh.
	 *
	 * @param T
	 *            WB_Transform to apply
	 *
	 * @return self
	 */
	public HE_Mesh transformSelf(final WB_Transform T) {

		return modify(new HEM_Transform(T));
	}

	/**
	 * Create transformed copy of mesh.
	 *
	 * @param T
	 *            WB_Transform to apply
	 *
	 * @return copy
	 */
	public HE_Mesh transform(final WB_Transform T) {
		return copy().modify(new HEM_Transform(T));
	}

	/**
	 * Translate entire mesh.
	 *
	 * @param x
	 *
	 * @param y
	 *
	 * @param z
	 *
	 * @return self
	 */
	public HE_Mesh moveSelf(final double x, final double y, final double z) {

		final Iterator<HE_Vertex> vItr = vItr();
		while (vItr.hasNext()) {
			vItr.next().addSelf(x, y, z);
		}
		clearPrecomputed();
		return this;
	}

	/**
	 * Create translated copy of mesh.
	 *
	 * @param x
	 *
	 * @param y
	 *
	 * @param z
	 *
	 * @return copy
	 */
	public HE_Mesh move(final double x, final double y, final double z) {
		HE_Mesh result = copy();
		final Iterator<HE_Vertex> vItr = result.vItr();
		while (vItr.hasNext()) {
			vItr.next().addSelf(x, y, z);
		}
		result.clearPrecomputed();
		return result;
	}

	/**
	 * Translate entire mesh.
	 *
	 * @param v
	 *            the v
	 * @return self
	 */
	public HE_Mesh moveSelf(final WB_Coord v) {

		return moveSelf(v.xd(), v.yd(), v.zd());
	}

	/**
	 * Created translated copy of mesh.
	 *
	 * @param v
	 *
	 * @return copy
	 */
	public HE_Mesh move(final WB_Coord v) {
		return move(v.xd(), v.yd(), v.zd());
	}

	/**
	 * Translate entire mesh to given position.
	 *
	 * @param x
	 *
	 * @param y
	 *
	 * @param z
	 *
	 * @return self
	 */
	public HE_Mesh moveToSelf(final double x, final double y, final double z) {

		WB_Point center = getCenter();
		final Iterator<HE_Vertex> vItr = vItr();
		while (vItr.hasNext()) {
			vItr.next().addSelf(x - center.xd(), y - center.yd(), z - center.zd());
		}
		clearPrecomputed();
		return this;
	}

	/**
	 * Create copy of mesh at given position.
	 *
	 * @param x
	 *
	 * @param y
	 *
	 * @param z
	 *
	 * @return copy
	 */
	public HE_Mesh moveTo(final double x, final double y, final double z) {
		HE_Mesh result = copy();
		WB_Point center = result.getCenter();
		final Iterator<HE_Vertex> vItr = result.vItr();
		while (vItr.hasNext()) {
			vItr.next().addSelf(x - center.xd(), y - center.yd(), z - center.zd());
		}
		result.clearPrecomputed();
		return result;
	}

	/**
	 * Translate entire mesh to given position.
	 *
	 * @param v
	 *
	 * @return self
	 */
	public HE_Mesh moveToSelf(final WB_Coord v) {

		return moveToSelf(v.xd(), v.yd(), v.zd());
	}

	/**
	 * create copy of mesh at given position.
	 *
	 * @param v
	 *
	 * @return copy
	 */
	public HE_Mesh moveTo(final WB_Coord v) {
		return moveTo(v.xd(), v.yd(), v.zd());
	}

	/**
	 * Rotate entire mesh around an arbitrary axis defined by 2 points.
	 *
	 * @param angle
	 *            angle
	 * @param p1x
	 *            x-coordinate of first point on axis
	 * @param p1y
	 *            y-coordinate of first point on axis
	 * @param p1z
	 *            z-coordinate of first point on axis
	 * @param p2x
	 *            x-coordinate of second point on axis
	 * @param p2y
	 *            y-coordinate of second point on axis
	 * @param p2z
	 *            z-coordinate of second point on axis
	 * @return self
	 */
	public HE_Mesh rotateAboutAxis2PSelf(final double angle, final double p1x, final double p1y, final double p1z,
			final double p2x, final double p2y, final double p2z) {

		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = vItr();
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutAxis(angle, new WB_Point(p1x, p1y, p1z), new WB_Vector(p2x - p1x, p2y - p1y, p2z - p1z));
		while (vItr.hasNext()) {
			v = vItr.next();
			raa.applyAsPointSelf(v);
		}
		clearPrecomputed();
		return this;
	}

	/**
	 * Create rotated copy of mesh around an arbitrary axis defined by 2 points.
	 *
	 * @param angle
	 *            angle
	 * @param p1x
	 *            x-coordinate of first point on axis
	 * @param p1y
	 *            y-coordinate of first point on axis
	 * @param p1z
	 *            z-coordinate of first point on axis
	 * @param p2x
	 *            x-coordinate of second point on axis
	 * @param p2y
	 *            y-coordinate of second point on axis
	 * @param p2z
	 *            z-coordinate of second point on axis
	 * @return copy
	 */
	public HE_Mesh rotateAboutAxis2P(final double angle, final double p1x, final double p1y, final double p1z,
			final double p2x, final double p2y, final double p2z) {
		HE_Mesh result = copy();
		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = result.vItr();
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutAxis(angle, new WB_Point(p1x, p1y, p1z), new WB_Vector(p2x - p1x, p2y - p1y, p2z - p1z));
		while (vItr.hasNext()) {
			v = vItr.next();
			raa.applyAsPointSelf(v);
		}
		result.clearPrecomputed();
		return result;
	}

	/**
	 * Rotate entire mesh around an arbitrary axis defined by 2 points..
	 *
	 * @param angle
	 *            angle
	 * @param p1
	 *            first point on axis
	 * @param p2
	 *            second point on axis
	 * @return self
	 */
	public HE_Mesh rotateAboutAxis2PSelf(final double angle, final WB_Coord p1, final WB_Coord p2) {

		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = vItr();
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutAxis(angle, p1, new WB_Vector(p1, p2));
		while (vItr.hasNext()) {
			v = vItr.next();
			raa.applyAsPointSelf(v);
		}
		clearPrecomputed();
		return this;
	}

	/**
	 * Create copy of mesh rotated around an arbitrary axis defined by 2 points.
	 *
	 * @param angle
	 *            angle
	 * @param p1
	 *            first point on axis
	 * @param p2
	 *            second point on axis
	 * @return copy
	 */
	public HE_Mesh rotateAboutAxis2P(final double angle, final WB_Coord p1, final WB_Coord p2) {
		HE_Mesh result = copy();
		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = result.vItr();
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutAxis(angle, p1, new WB_Vector(p1, p2));
		while (vItr.hasNext()) {
			v = vItr.next();
			raa.applyAsPointSelf(v);
		}
		result.clearPrecomputed();
		return result;
	}

	/**
	 * Rotate entire mesh around an arbitrary axis defined by a point and a
	 * direction.
	 *
	 * @param angle
	 *            angle
	 * @param p
	 *            rotation point
	 * @param a
	 *            axis
	 * @return self
	 */
	public HE_Mesh rotateAboutAxisSelf(final double angle, final WB_Coord p, final WB_Coord a) {

		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = vItr();
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutAxis(angle, p, a);
		while (vItr.hasNext()) {
			v = vItr.next();
			raa.applyAsPointSelf(v);
		}
		clearPrecomputed();
		return this;
	}

	/**
	 * Create copy of mesh rotated around an arbitrary axis defined by a point
	 * and a direction.
	 *
	 * @param angle
	 *            angle
	 * @param p
	 *            rotation point
	 * @param a
	 *            axis
	 * @return copy
	 */
	public HE_Mesh rotateAboutAxis(final double angle, final WB_Coord p, final WB_Coord a) {
		HE_Mesh result = copy();
		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = result.vItr();
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutAxis(angle, p, a);
		while (vItr.hasNext()) {
			v = vItr.next();
			raa.applyAsPointSelf(v);
		}
		result.clearPrecomputed();
		return result;

	}

	/**
	 * Rotate entire mesh around an arbitrary axis defined by a point and a
	 * direction.
	 *
	 * @param angle
	 * @param px
	 * @param py
	 * @param pz
	 * @param ax
	 * @param ay
	 * @param az
	 * @return self
	 */
	public HE_Mesh rotateAboutAxisSelf(final double angle, final double px, final double py, final double pz,
			final double ax, final double ay, final double az) {

		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = vItr();
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutAxis(angle, new WB_Point(px, py, pz), new WB_Vector(ax, ay, az));
		while (vItr.hasNext()) {
			v = vItr.next();
			raa.applyAsPointSelf(v);
		}
		clearPrecomputed();
		return this;
	}

	/**
	 * Rotate entire mesh around an arbitrary axis defined by a point and a
	 * direction.
	 *
	 * @param angle
	 * @param px
	 * @param py
	 * @param pz
	 * @param ax
	 * @param ay
	 * @param az
	 * @return copy
	 */
	public HE_Mesh rotateAboutAxis(final double angle, final double px, final double py, final double pz,
			final double ax, final double ay, final double az) {
		HE_Mesh result = copy();
		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = result.vItr();
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutAxis(angle, new WB_Point(px, py, pz), new WB_Vector(ax, ay, az));
		while (vItr.hasNext()) {
			v = vItr.next();
			raa.applyAsPointSelf(v);
		}
		result.clearPrecomputed();
		return result;
	}

	/**
	 * Rotate entire mesh around an arbitrary axis in origin.
	 *
	 * @param angle
	 *            angle
	 * @param a
	 *            axis
	 * @return self
	 */
	public HE_Mesh rotateAboutOriginSelf(final double angle, final WB_Coord a) {

		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = vItr();
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutOrigin(angle, a);
		while (vItr.hasNext()) {
			v = vItr.next();
			raa.applyAsPointSelf(v);
		}
		clearPrecomputed();
		return this;
	}

	/**
	 * Create copy of mesh rotate around an arbitrary axis in origin.
	 *
	 * @param angle
	 *            angle
	 * @param a
	 *            axis
	 * @return copy
	 */
	public HE_Mesh rotateAboutOrigin(final double angle, final WB_Coord a) {
		HE_Mesh result = copy();
		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = result.vItr();
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutOrigin(angle, a);
		while (vItr.hasNext()) {
			v = vItr.next();
			raa.applyAsPointSelf(v);
		}
		result.clearPrecomputed();
		return result;
	}

	/**
	 * Rotate entire mesh around an arbitrary axis in origin.
	 *
	 *
	 * @param angle
	 * @param ax
	 * @param ay
	 * @param az
	 * @return
	 */
	public HE_Mesh rotateAboutOriginSelf(final double angle, final double ax, final double ay, final double az) {

		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = vItr();
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutOrigin(angle, new WB_Vector(ax, ay, az));
		while (vItr.hasNext()) {
			v = vItr.next();
			raa.applyAsPointSelf(v);
		}
		clearPrecomputed();
		return this;
	}

	/**
	 * Create copy of mesh rotated around an arbitrary axis in origin.
	 *
	 *
	 * @param angle
	 * @param ax
	 * @param ay
	 * @param az
	 * @return copy
	 */
	public HE_Mesh rotateAboutOrigin(final double angle, final double ax, final double ay, final double az) {
		HE_Mesh result = copy();
		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = result.vItr();
		final WB_Transform raa = new WB_Transform();
		raa.addRotateAboutOrigin(angle, new WB_Vector(ax, ay, az));
		while (vItr.hasNext()) {
			v = vItr.next();
			raa.applyAsPointSelf(v);
		}
		result.clearPrecomputed();
		return result;
	}

	/**
	 * Rotate entire mesh around an arbitrary axis in center.
	 *
	 * @param angle
	 *            angle
	 * @param a
	 *            axis
	 * @return self
	 */
	public HE_Mesh rotateAboutCenterSelf(final double angle, final WB_Coord a) {

		return rotateAboutAxisSelf(angle, getCenter(), a);
	}

	/**
	 * Create copy of mesh rotated around an arbitrary axis in center.
	 *
	 * @param angle
	 *            angle
	 * @param a
	 *            axis
	 * @return self
	 */
	public HE_Mesh rotateAboutCenter(final double angle, final WB_Coord a) {

		return rotateAboutAxis(angle, getCenter(), a);
	}

	/**
	 * Rotate entire mesh around an arbitrary axis in center.
	 *
	 * @param angle
	 * @param ax
	 * @param ay
	 * @param az
	 * @return
	 */
	public HE_Mesh rotateAboutCenterSelf(final double angle, final double ax, final double ay, final double az) {

		return rotateAboutAxisSelf(angle, getCenter(), new WB_Vector(ax, ay, az));
	}

	/**
	 * Create copy of mesh rotated around an arbitrary axis in center.
	 *
	 * @param angle
	 * @param ax
	 * @param ay
	 * @param az
	 * @return copy
	 */
	public HE_Mesh rotateAboutCenter(final double angle, final double ax, final double ay, final double az) {
		return rotateAboutAxis(angle, getCenter(), new WB_Vector(ax, ay, az));
	}

	/**
	 * Scale entire mesh around center point.
	 *
	 * @param scaleFactorx
	 *            x-coordinate of scale factor
	 * @param scaleFactory
	 *            y-coordinate of scale factor
	 * @param scaleFactorz
	 *            z-coordinate of scale factor
	 * @param c
	 *            center
	 * @return self
	 */
	public HE_Mesh scaleSelf(final double scaleFactorx, final double scaleFactory, final double scaleFactorz,
			final WB_Coord c) {

		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = vItr();
		while (vItr.hasNext()) {
			v = vItr.next();
			v.set(c.xd() + scaleFactorx * (v.xd() - c.xd()), c.yd() + scaleFactory * (v.yd() - c.yd()),
					c.zd() + scaleFactorz * (v.zd() - c.zd()));
		}
		clearPrecomputed();
		return this;
	}

	/**
	 * Create copy of mesh scaled around center point.
	 *
	 * @param scaleFactorx
	 *            x-coordinate of scale factor
	 * @param scaleFactory
	 *            y-coordinate of scale factor
	 * @param scaleFactorz
	 *            z-coordinate of scale factor
	 * @param c
	 *            center
	 * @return copy
	 */
	public HE_Mesh scale(final double scaleFactorx, final double scaleFactory, final double scaleFactorz,
			final WB_Coord c) {
		HE_Mesh result = copy();
		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = result.vItr();
		while (vItr.hasNext()) {
			v = vItr.next();
			v.set(c.xd() + scaleFactorx * (v.xd() - c.xd()), c.yd() + scaleFactory * (v.yd() - c.yd()),
					c.zd() + scaleFactorz * (v.zd() - c.zd()));
		}
		result.clearPrecomputed();
		return result;
	}

	/**
	 * Scale entire mesh around center point.
	 *
	 * @param scaleFactor
	 *            scale
	 * @param c
	 *            center
	 * @return self
	 */
	public HE_Mesh scaleSelf(final double scaleFactor, final WB_Coord c) {

		return scaleSelf(scaleFactor, scaleFactor, scaleFactor, c);
	}

	/**
	 * Create copy of mesh scaled around center point.
	 *
	 * @param scaleFactor
	 *            scale
	 * @param c
	 *            center
	 * @return copy
	 */
	public HE_Mesh scale(final double scaleFactor, final WB_Coord c) {
		return scale(scaleFactor, scaleFactor, scaleFactor, c);
	}

	/**
	 * Scale entire mesh around bodycenter.
	 *
	 * @param scaleFactorx
	 *            x-coordinate of scale factor
	 * @param scaleFactory
	 *            y-coordinate of scale factor
	 * @param scaleFactorz
	 *            z-coordinate of scale factor
	 * @return self
	 */
	public HE_Mesh scaleSelf(final double scaleFactorx, final double scaleFactory, final double scaleFactorz) {

		WB_Point center = getCenter();
		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = vItr();
		while (vItr.hasNext()) {
			v = vItr.next();
			v.set(center.xd() + scaleFactorx * (v.xd() - center.xd()),
					center.yd() + scaleFactory * (v.yd() - center.yd()),
					center.zd() + scaleFactorz * (v.zd() - center.zd()));
		}
		clearPrecomputed();
		return this;
	}

	/**
	 * Create copy of mesh scaled around bodycenter.
	 *
	 * @param scaleFactorx
	 *            x-coordinate of scale factor
	 * @param scaleFactory
	 *            y-coordinate of scale factor
	 * @param scaleFactorz
	 *            z-coordinate of scale factor
	 * @return copy
	 */
	public HE_Mesh scale(final double scaleFactorx, final double scaleFactory, final double scaleFactorz) {
		HE_Mesh result = copy();
		WB_Point center = result.getCenter();
		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = result.vItr();
		while (vItr.hasNext()) {
			v = vItr.next();
			v.set(center.xd() + scaleFactorx * (v.xd() - center.xd()),
					center.yd() + scaleFactory * (v.yd() - center.yd()),
					center.zd() + scaleFactorz * (v.zd() - center.zd()));
		}
		result.clearPrecomputed();
		return result;
	}

	/**
	 * Scale entire mesh around bodycenter.
	 *
	 * @param scaleFactor
	 *            scale
	 * @return self
	 */
	public HE_Mesh scaleSelf(final double scaleFactor) {

		return scaleSelf(scaleFactor, scaleFactor, scaleFactor);
	}

	/**
	 * Create copy of mesh scaled around bodycenter.
	 *
	 * @param scaleFactor
	 *            scale
	 * @return copy
	 */
	public HE_Mesh scale(final double scaleFactor) {
		return scale(scaleFactor, scaleFactor, scaleFactor);
	}

	/**
	 * Fit in aabb.
	 *
	 * @param AABB
	 *
	 */
	public void fitInAABB(final WB_AABB AABB) {
		final WB_AABB self = getAABB();
		moveSelf(new WB_Vector(self.getMin(), AABB.getMin()));
		scaleSelf(AABB.getWidth() / self.getWidth(), AABB.getHeight() / self.getHeight(),
				AABB.getDepth() / self.getDepth(), new WB_Point(AABB.getMin()));
	}

	public void fitInAABB(final WB_AABB from, final WB_AABB to) {

		moveSelf(new WB_Vector(from.getMin(), to.getMin()));
		scaleSelf(to.getWidth() / from.getWidth(), to.getHeight() / from.getHeight(), to.getDepth() / from.getDepth(),
				new WB_Point(to.getMin()));
	}

	/**
	 * Fit in aabb constrained.
	 *
	 * @param AABB
	 *
	 * @return
	 */
	public double fitInAABBConstrained(final WB_AABB AABB) {
		final WB_AABB self = getAABB();
		moveSelf(new WB_Vector(self.getCenter(), AABB.getCenter()));
		double f = Math.min(AABB.getWidth() / self.getWidth(), AABB.getHeight() / self.getHeight());
		f = Math.min(f, AABB.getDepth() / self.getDepth());
		scaleSelf(f, new WB_Point(AABB.getCenter()));
		return f;
	}

	public double fitInAABBConstrained(final WB_AABB from, final WB_AABB to) {

		moveSelf(new WB_Vector(from.getCenter(), to.getCenter()));
		double f = Math.min(to.getWidth() / from.getWidth(), to.getHeight() / from.getHeight());
		f = Math.min(f, to.getDepth() / from.getDepth());
		scaleSelf(f, new WB_Point(to.getCenter()));
		return f;
	}

	/**
	 * Get the center (average of all vertex positions).
	 *
	 * @return the center
	 */

	public WB_Point getCenter() {
		final WB_Point c = new WB_Point(0, 0, 0);
		final Iterator<HE_Vertex> vItr = vItr();
		while (vItr.hasNext()) {
			c.addSelf(vItr.next());
		}
		c.divSelf(getNumberOfVertices());
		return c;
	}

	/**
	 * Delete face and remove all references.
	 *
	 * @param faces
	 *            faces to delete
	 */
	public void deleteFaces(final HE_Selection faces) {
		HE_Face f;
		final Iterator<HE_Face> fItr = faces.fItr();
		while (fItr.hasNext()) {
			f = fItr.next();
			remove(f);
		}
		cleanUnusedElementsByFace();
		capHalfedges();
	}

	/**
	 * Delete face and remove all references. Its halfedges remain and form a
	 * valid boundary loop.
	 *
	 * @param f
	 *            face to delete
	 */
	public void deleteFace(final HE_Face f) {
		HE_Halfedge he = f.getHalfedge();
		do {
			clearFace(he);
			he = he.getNextInFace();
		} while (he != f.getHalfedge());
		remove(f);
	}

	/**
	 * Delete face and remove all references. Its halfedges are removed, the
	 * boundary loop is unpaired.
	 *
	 * @param f
	 */

	public void cutFace(final HE_Face f) {
		HE_Halfedge he = f.getHalfedge();
		do {
			setHalfedge(he.getVertex(), he.getNextInVertex());

			he = he.getNextInFace();
		} while (he != f.getHalfedge());

		do {
			clearFace(he);
			clearPair(he);
			remove(he);
			he = he.getNextInFace();
		} while (he != f.getHalfedge());
		remove(f);
	}

	/**
	 * Delete edge. Adjacent faces are fused.
	 *
	 * @param e
	 *            edge to delete
	 * @return fused face (or null)
	 */
	public HE_Face deleteEdge(final HE_Halfedge e) {

		HE_Face f = null;
		final HE_Halfedge he1 = e.isEdge() ? e : e.getPair();
		final HE_Halfedge he2 = he1.getPair();
		final HE_Halfedge he1n = he1.getNextInFace();
		final HE_Halfedge he2n = he2.getNextInFace();
		final HE_Halfedge he1p = he1.getPrevInFace();
		final HE_Halfedge he2p = he2.getPrevInFace();
		HE_Vertex v = he1.getVertex();
		if (v.getHalfedge() == he1) {
			setHalfedge(v, he1.getNextInVertex());
		}
		v = he2.getVertex();
		if (v.getHalfedge() == he2) {
			setHalfedge(v, he2.getNextInVertex());
		}
		setNext(he1p, he2n);
		setNext(he2p, he1n);
		if (he1.getFace() != null && he2.getFace() != null) {
			f = new HE_Face();
			f.copyProperties(e.getPair().getFace());
			addDerivedElement(f, e.getPair().getFace());
			setHalfedge(f, he1p);
			HE_Halfedge he = he1p;
			do {
				setFace(he, f);
				he = he.getNextInFace();
			} while (he != he1p);
		}
		if (he1.getFace() != null) {
			remove(he1.getFace());
		}
		if (he2.getFace() != null) {
			remove(he2.getFace());
		}
		remove(he1);
		remove(he2);
		return f;
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public int[] getTriangles() {
		if (triangles == null) {
			final HE_Mesh trimesh = this.copy();
			trimesh.triangulate();
			triangles = new int[trimesh.getNumberOfFaces()];
			final Iterator<HE_Face> fItr = trimesh.fItr();
			HE_Face f;
			int id = 0;
			while (fItr.hasNext()) {
				f = fItr.next();
				triangles[id++] = getIndex(f.getHalfedge().getVertex());
			}
		}
		return triangles;
	}

	/**
	 * Triangulate all concave faces.
	 *
	 * @return
	 */
	public HE_Selection triangulateConcaveFaces() {
		return HET_MeshOp.triangulateConcaveFaces(this);
	}

	/**
	 *
	 *
	 * @param sel
	 * @return
	 */
	public HE_Selection triangulateConcaveFaces(final List<HE_Face> sel) {
		return HET_MeshOp.triangulateConcaveFaces(this, sel);
	}

	/**
	 * Triangulate face if concave.
	 *
	 * @param key
	 *            key of face
	 * @return
	 */
	public HE_Selection triangulateConcaveFace(final long key) {
		return HET_MeshOp.triangulateConcaveFace(this, key);
	}

	/**
	 * Triangulate face if concave.
	 *
	 * @param face
	 *            key of face
	 * @return
	 */
	public HE_Selection triangulateConcaveFace(final HE_Face face) {
		return HET_MeshOp.triangulateConcaveFace(this, face);
	}

	/**
	 * Check consistency of datastructure.
	 *
	 * @return true or false
	 */
	public boolean validate() {
		return HET_Diagnosis.validate(this);
	}

	/**
	 * Fuse all coplanar faces connected to face. New face can be concave.
	 *
	 * @param face
	 *            starting face
	 * @param a
	 *            the a
	 * @return new face
	 */
	public HE_Face fuseCoplanarFace(final HE_Face face, final double a) {

		List<HE_Face> neighbors;
		FastList<HE_Face> facesToCheck = new FastList<HE_Face>();
		final FastList<HE_Face> newFacesToCheck = new FastList<HE_Face>();
		facesToCheck.add(face);
		final HE_Selection sel = HE_Selection.getSelection(this);
		sel.add(face);
		HE_Face f;
		HE_Face fn;
		int ni = -1;
		int nf = 0;
		double sa = Math.sin(a);
		sa *= sa;
		while (ni < nf) {
			newFacesToCheck.clear();
			for (int i = 0; i < facesToCheck.size(); i++) {
				f = facesToCheck.get(i);
				neighbors = f.getNeighborFaces();
				for (int j = 0; j < neighbors.size(); j++) {
					fn = neighbors.get(j);
					if (!sel.contains(fn)) {
						if (WB_Vector.isParallel(f.getFaceNormal(), fn.getFaceNormal(), sa)) {
							sel.add(fn);
							newFacesToCheck.add(fn);
						}
					}
				}
			}
			facesToCheck = newFacesToCheck;
			ni = nf;
			nf = sel.getNumberOfFaces();
		}
		if (sel.getNumberOfFaces() == 1) {
			return face;
		}
		final List<HE_Halfedge> halfedges = sel.getOuterHalfedgesInside();
		final HE_Face newFace = new HE_Face();
		add(newFace);
		newFace.copyProperties(sel.getFaceWithIndex(0));
		setHalfedge(newFace, halfedges.get(0));
		for (int i = 0; i < halfedges.size(); i++) {
			final HE_Halfedge hei = halfedges.get(i);
			final HE_Halfedge hep = halfedges.get(i).getPair();
			for (int j = 0; j < halfedges.size(); j++) {
				final HE_Halfedge hej = halfedges.get(j);
				if (i != j && hep.getVertex() == hej.getVertex()) {
					setNext(hei, hej);
				}
			}
			setFace(hei, newFace);
			setHalfedge(hei.getVertex(), hei);
		}
		removeFaces(sel.getFacesAsArray());
		cleanUnusedElementsByFace();
		capHalfedges();
		return newFace;
	}

	/**
	 * Fuse all planar faces. Can lead to concave faces.
	 *
	 */
	public void fuseCoplanarFaces() {
		fuseCoplanarFaces(0);
	}

	/**
	 * Fuse all planar faces. Can lead to concave faces.
	 *
	 * @param a
	 *            the a
	 */
	public void fuseCoplanarFaces(final double a) {
		int ni;
		int no;
		do {
			ni = getNumberOfFaces();
			final List<HE_Face> faces = this.getFaces();
			for (int i = 0; i < faces.size(); i++) {
				final HE_Face f = faces.get(i);
				if (contains(f)) {
					fuseCoplanarFace(f, a);
				}
			}
			no = getNumberOfFaces();
		} while (no < ni);
	}

	/**
	 * Return a KD-tree containing all face centers.
	 *
	 * @return WB_KDTree
	 */
	public WB_KDTree<WB_Coord, Long> getFaceTree() {
		final WB_KDTree<WB_Coord, Long> tree = new WB_KDTree<WB_Coord, Long>();
		HE_Face f;
		final Iterator<HE_Face> fItr = fItr();
		while (fItr.hasNext()) {
			f = fItr.next();
			tree.add(f.getFaceCenter(), f.key());
		}
		return tree;
	}

	/**
	 * Return a KD-tree containing all vertices.
	 *
	 * @return WB_KDTree
	 */
	public WB_KDTree<WB_Coord, Long> getVertexTree() {
		final WB_KDTree<WB_Coord, Long> tree = new WB_KDTree<WB_Coord, Long>();
		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = vItr();
		while (vItr.hasNext()) {
			v = vItr.next();
			tree.add(v, v.key());
		}
		return tree;
	}

	/**
	 * Return the closest vertex on the mesh.
	 *
	 * @param p
	 *            query point
	 * @param vertexTree
	 *            KD-tree from mesh (from vertexTree())
	 * @return HE_Vertex closest vertex
	 */
	public HE_Vertex getClosestVertex(final WB_Coord p, final WB_KDTree<WB_Coord, Long> vertexTree) {
		final WB_KDEntry<WB_Coord, Long>[] closestVertex = vertexTree.getNearestNeighbors(p, 1);
		if (closestVertex.length == 0) {
			return null;
		}
		return getVertexWithKey(closestVertex[0].value);
	}

	/**
	 * Return the closest point on the mesh.
	 *
	 * @param p
	 *            query point
	 * @param vertexTree
	 *            KD-tree from mesh (from vertexTree())
	 * @return WB_Coordinate closest point
	 */
	public WB_Coord getClosestPoint(final WB_Coord p, final WB_KDTree<WB_Coord, Long> vertexTree) {
		final WB_KDEntry<WB_Coord, Long>[] closestVertex = vertexTree.getNearestNeighbors(p, 1);
		final HE_Vertex v = getVertexWithKey(closestVertex[0].value);
		if (v == null) {
			return null;
		}
		final List<HE_Face> faces = v.getFaceStar();
		double d;
		double dmin = Double.POSITIVE_INFINITY;
		WB_Coord result = new WB_Point();
		for (int i = 0; i < faces.size(); i++) {
			final WB_Polygon poly = faces.get(i).toPolygon();
			final WB_Coord tmp = WB_GeometryOp3D.getClosestPoint3D(p, poly);
			d = WB_GeometryOp3D.getSqDistance3D(tmp, p);
			if (d < dmin) {
				dmin = d;
				result = tmp;
			}
		}
		return result;
	}

	/**
	 * Split the closest face in the query point.
	 *
	 * @param p
	 *            query point
	 * @param vertexTree
	 *            KD-tree from mesh (from vertexTree())
	 */
	public void addPointInClosestFace(final WB_Coord p, final WB_KDTree<WB_Coord, Long> vertexTree) {
		final WB_KDEntry<WB_Coord, Long>[] closestVertex = vertexTree.getNearestNeighbors(p, 1);
		final HE_Vertex v = getVertexWithKey(closestVertex[0].value);
		final List<HE_Face> faces = v.getFaceStar();
		double d;
		double dmin = Double.POSITIVE_INFINITY;
		HE_Face face = new HE_Face();
		for (int i = 0; i < faces.size(); i++) {
			final WB_Polygon poly = faces.get(i).toPolygon();
			final WB_Coord tmp = WB_GeometryOp3D.getClosestPoint3D(p, poly);
			d = WB_GeometryOp3D.getSqDistance3D(tmp, p);
			if (d < dmin) {
				dmin = d;
				face = faces.get(i);
				;
			}
		}
		final HE_Vertex nv = HEM_TriSplit.splitFaceTri(this, face, p).vItr().next();
		vertexTree.add(nv, nv.key());
	}

	/**
	 *
	 *
	 * @return
	 */
	public double getArea() {
		final Iterator<HE_Face> fItr = fItr();
		double A = 0.0;
		while (fItr.hasNext()) {
			A += fItr.next().getFaceArea();
		}
		return A;
	}

	/**
	 * Triangulate face.
	 *
	 * @param key
	 *            key of face
	 * @return
	 */
	public HE_Selection triangulate(final long key) {
		return triangulate(getFaceWithKey(key));
	}

	/**
	 *
	 *
	 * @param v
	 * @return
	 */
	public HE_Selection triangulateFaceStar(final HE_Vertex v) {
		return HET_MeshOp.triangulateFaceStar(this, v);
	}

	/**
	 *
	 *
	 * @param vertexkey
	 * @return
	 */
	public HE_Selection triangulateFaceStar(final long vertexkey) {
		return HET_MeshOp.triangulateFaceStar(this, vertexkey);
	}

	/**
	 *
	 *
	 * @param face
	 * @return
	 */
	public HE_Selection triangulate(final HE_Face face) {
		return HET_MeshOp.triangulate(this, face);
	}

	/**
	 * Triangulate all faces.
	 *
	 * @return
	 */
	public HE_Selection triangulate() {
		return HET_MeshOp.triangulate(this);
	}

	/**
	 * Triangulate.
	 *
	 * @param sel
	 *            the sel
	 * @return
	 */
	public HE_Selection triangulate(final HE_Selection sel) {
		return HET_MeshOp.triangulate(sel);
	}

	/**
	 * Clean.
	 */
	public void clean() {
		modify(new HEM_Clean());
	}

	public WB_Coord getFaceNormal(final int id) {
		return getFaceWithIndex(id).getFaceNormal();
	}

	public WB_Coord getFaceCenter(final int id) {
		return getFaceWithIndex(id).getFaceCenter();
	}

	public WB_Coord getVertexNormal(final int i) {
		return getVertexWithIndex(i).getVertexNormal();
	}

	public WB_Coord getVertex(final int i) {
		return getVertexWithIndex(i);
	}

	@Override
	public WB_CoordCollection getPoints() {
		final List<WB_Coord> result = new FastList<WB_Coord>();
		result.addAll(vertices.getObjects());
		return WB_CoordCollection.getCollection(result);
	}

	/**
	 *
	 */
	class CreatorThread implements Callable<HE_Mesh> {
		HEC_Creator creator;

		CreatorThread(final HEC_Creator creator) {
			this.creator = creator;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public HE_Mesh call() {
			HE_Mesh result = creator.create();
			return result;
		}
	}

	/**
	 *
	 */
	class ModifierThread implements Callable<HE_Mesh> {
		HEM_Modifier machine;
		HE_Mesh mesh;

		ModifierThread(final HEM_Modifier machine, final HE_Mesh mesh) {
			this.machine = machine;
			this.mesh = mesh;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public HE_Mesh call() {
			try {
				return machine.applySelf(mesh.get());
			} catch (Exception e) {
				return mesh;
			}
		}
	}

	/**
	 *
	 */
	class SubdividorThread implements Callable<HE_Mesh> {
		HES_Subdividor machine;
		HE_Mesh mesh;

		SubdividorThread(final HES_Subdividor machine, final HE_Mesh mesh) {
			this.machine = machine;
			this.mesh = mesh;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public HE_Mesh call() {
			try {
				return machine.applySelf(mesh.get());
			} catch (Exception e) {
				return mesh;
			}
		}
	}

	/**
	 *
	 */
	class SimplifierThread implements Callable<HE_Mesh> {
		HES_Simplifier machine;
		HE_Mesh mesh;

		SimplifierThread(final HES_Simplifier machine, final HE_Mesh mesh) {
			this.machine = machine;
			this.mesh = mesh;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public HE_Mesh call() {
			try {
				return machine.applySelf(mesh.get());
			} catch (Exception e) {
				return mesh;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.geom.Point3D#toString()
	 */
	@Override
	public String toString() {
		String s = "HE_Mesh key: " + getKey() + ". (" + getNumberOfVertices() + ", " + getNumberOfFaces() + ")";

		return s;
	}

	public boolean isFinished() {
		return finished;

	}

	public boolean isSurface() {
		return this.getBoundaryHalfedges().size() > 0;
	}

	/**
	 * Select all mesh elements.
	 *
	 * @return current selection
	 */
	public HE_Selection selectAll(final String name) {
		HE_Selection sel = HE_Selection.getSelection(this);
		sel.addFaces(this);
		sel.addHalfedges(this);
		sel.addVertices(this);
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 */
	public HE_Selection selectAllEdges(final String name) {
		HE_Selection sel = HE_Selection.getSelection(this);
		sel.addEdges(this);
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 */
	public HE_Selection selectAllFaces(final String name) {
		HE_Selection sel = HE_Selection.getSelection(this);
		sel.addFaces(this);
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 */
	public HE_Selection selectAllHalfedges(final String name) {
		HE_Selection sel = HE_Selection.getSelection(this);
		sel.addHalfedges(this);
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 */
	public HE_Selection selectAllInnerBoundaryHalfedges(final String name) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final Iterator<HE_Halfedge> heItr = this.heItr();
		HE_Halfedge he;
		while (heItr.hasNext()) {
			he = heItr.next();
			if (he.getPair().getFace() == null) {
				sel.add(he);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 */
	public HE_Selection selectAllOuterBoundaryHalfedges(final String name) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final Iterator<HE_Halfedge> heItr = this.heItr();
		HE_Halfedge he;
		while (heItr.hasNext()) {
			he = heItr.next();
			if (he.getFace() == null) {
				sel.add(he);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 */
	public HE_Selection selectAllVertices(final String name) {
		HE_Selection sel = HE_Selection.getSelection(this);
		sel.addVertices(this);
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param P
	 */
	public HE_Selection selectBackEdges(final String name, final WB_Plane P) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final HE_EdgeIterator eitr = this.eItr();
		HE_Halfedge e;
		while (eitr.hasNext()) {
			e = eitr.next();
			if (HET_MeshOp.classifyEdgeToPlane3D(e, P) == WB_Classification.BACK) {
				sel.add(e);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param P
	 */
	public HE_Selection selectBackFaces(final String name, final WB_Plane P) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final HE_FaceIterator fitr = this.fItr();
		HE_Face f;
		while (fitr.hasNext()) {
			f = fitr.next();
			if (HET_MeshOp.classifyFaceToPlane3D(f, P) == WB_Classification.BACK) {
				sel.add(f);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param P
	 */
	public HE_Selection selectBackVertices(final String name, final WB_Plane P) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final HE_VertexIterator vitr = this.vItr();
		HE_Vertex v;
		while (vitr.hasNext()) {
			v = vitr.next();
			if (WB_GeometryOp3D.classifyPointToPlane3D(v, P) == WB_Classification.BACK) {
				sel.add(v);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 */
	public HE_Selection selectBoundaryEdges(final String name) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final HE_EdgeIterator eItr = this.eItr();
		HE_Halfedge e;
		while (eItr.hasNext()) {
			e = eItr.next();
			if (e.isInnerBoundary()) {
				sel.add(e);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 */
	public HE_Selection selectBoundaryFaces(final String name) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final Iterator<HE_Halfedge> heItr = this.heItr();
		HE_Halfedge he;
		while (heItr.hasNext()) {
			he = heItr.next();
			if (he.getFace() == null) {
				sel.add(he.getPair().getFace());
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 */
	public HE_Selection selectBoundaryVertices(final String name) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final Iterator<HE_Halfedge> heItr = this.heItr();
		HE_Halfedge he;
		while (heItr.hasNext()) {
			he = heItr.next();
			if (he.getFace() == null) {
				sel.add(he.getVertex());
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param P
	 */
	public HE_Selection selectCrossingEdges(final String name, final WB_Plane P) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final HE_EdgeIterator eitr = this.eItr();
		HE_Halfedge e;
		while (eitr.hasNext()) {
			e = eitr.next();
			if (HET_MeshOp.classifyEdgeToPlane3D(e, P) == WB_Classification.CROSSING) {
				sel.add(e);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param P
	 */
	public HE_Selection selectCrossingFaces(final String name, final WB_Plane P) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final HE_FaceIterator fitr = this.fItr();
		HE_Face f;
		while (fitr.hasNext()) {
			f = fitr.next();
			if (HET_MeshOp.classifyFaceToPlane3D(f, P) == WB_Classification.CROSSING) {
				sel.add(f);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	public HE_Selection selectEdgesWithLabel(final String name, final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Halfedge e;
		final Iterator<HE_Halfedge> eItr = this.eItr();
		while (eItr.hasNext()) {
			e = eItr.next();
			if (e.getUserLabel() == label) {
				sel.add(e);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	public HE_Selection selectEdgesWithOtherInternalLabel(final String name, final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Halfedge e;
		final Iterator<HE_Halfedge> eItr = this.eItr();
		while (eItr.hasNext()) {
			e = eItr.next();
			if (e.getInternalLabel() != label) {
				sel.add(e);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	public HE_Selection selectEdgesWithOtherLabel(final String name, final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Halfedge e;
		final Iterator<HE_Halfedge> eItr = this.eItr();
		while (eItr.hasNext()) {
			e = eItr.next();
			if (e.getUserLabel() != label) {
				sel.add(e);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	public HE_Selection selectEdgeWithInternalLabel(final String name, final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Halfedge e;
		final Iterator<HE_Halfedge> eItr = this.eItr();
		while (eItr.hasNext()) {
			e = eItr.next();
			if (e.getInternalLabel() == label) {
				sel.add(e);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	public HE_Selection selectFacesWithInternalLabel(final String name, final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Face f;
		final Iterator<HE_Face> fItr = this.fItr();
		while (fItr.hasNext()) {
			f = fItr.next();
			if (f.getInternalLabel() == label) {
				sel.add(f);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	public HE_Selection selectFacesWithLabel(final String name, final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Face f;
		final Iterator<HE_Face> fItr = this.fItr();
		while (fItr.hasNext()) {
			f = fItr.next();
			if (f.getUserLabel() == label) {
				sel.add(f);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param v
	 */
	public HE_Selection selectFacesWithNormal(final String name, final WB_Coord v) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final WB_Vector w = new WB_Vector(v);
		w.normalizeSelf();
		HE_Face f;
		final Iterator<HE_Face> fItr = this.fItr();
		while (fItr.hasNext()) {
			f = fItr.next();
			if (WB_Vector.dot(f.getFaceNormal(), v) > 1.0 - WB_Epsilon.EPSILON) {
				sel.add(f);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param n
	 * @param ta
	 */
	public HE_Selection selectFacesWithNormal(final String name, final WB_Coord n, final double ta) {
		HE_Selection sel = HE_Selection.getSelection(this);
		final WB_Vector nn = new WB_Vector(n);
		nn.normalizeSelf();
		final double cta = Math.cos(ta);
		HE_FaceIterator fItr = sel.parent.fItr();
		HE_Face f;
		while (fItr.hasNext()) {
			f = fItr.next();
			if (nn.dot(f.getFaceNormal()) > cta) {
				sel.add(f);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	public HE_Selection selectFacesWithOtherInternalLabel(final String name, final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Face f;
		final Iterator<HE_Face> fItr = this.fItr();
		while (fItr.hasNext()) {
			f = fItr.next();
			if (f.getInternalLabel() != label) {
				sel.add(f);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	public HE_Selection selectFacesWithOtherLabel(final String name, final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Face f;
		final Iterator<HE_Face> fItr = this.fItr();
		while (fItr.hasNext()) {
			f = fItr.next();
			if (f.getUserLabel() != label) {
				sel.add(f);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param P
	 */
	public HE_Selection selectFrontEdges(final String name, final WB_Plane P) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final HE_EdgeIterator eitr = this.eItr();
		HE_Halfedge e;
		while (eitr.hasNext()) {
			e = eitr.next();
			if (HET_MeshOp.classifyEdgeToPlane3D(e, P) == WB_Classification.FRONT) {
				sel.add(e);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param P
	 */
	public HE_Selection selectFrontFaces(final String name, final WB_Plane P) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final HE_FaceIterator fitr = this.fItr();
		HE_Face f;
		while (fitr.hasNext()) {
			f = fitr.next();
			if (HET_MeshOp.classifyFaceToPlane3D(f, P) == WB_Classification.FRONT) {
				sel.add(f);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param P
	 */
	public HE_Selection selectFrontVertices(final String name, final WB_Plane P) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final HE_VertexIterator vitr = this.vItr();
		HE_Vertex v;
		while (vitr.hasNext()) {
			v = vitr.next();
			if (WB_GeometryOp3D.classifyPointToPlane3D(v, P) == WB_Classification.FRONT) {
				sel.add(v);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	public HE_Selection selectHalfedgesWithLabel(final String name, final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Halfedge he;
		final Iterator<HE_Halfedge> heItr = this.heItr();
		while (heItr.hasNext()) {
			he = heItr.next();
			if (he.getUserLabel() == label) {
				sel.add(he);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	public HE_Selection selectHalfedgesWithOtherInternalLabel(final String name, final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Halfedge he;
		final Iterator<HE_Halfedge> heItr = this.heItr();
		while (heItr.hasNext()) {
			he = heItr.next();
			if (he.getInternalLabel() != label) {
				sel.add(he);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	public HE_Selection selectHalfedgesWithOtherLabel(final String name, final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Halfedge he;
		final Iterator<HE_Halfedge> heItr = this.heItr();
		while (heItr.hasNext()) {
			he = heItr.next();
			if (he.getUserLabel() != label) {
				sel.add(he);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	public HE_Selection selectHalfedgeWithInternalLabel(final String name, final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Halfedge he;
		final Iterator<HE_Halfedge> heItr = this.heItr();
		while (heItr.hasNext()) {
			he = heItr.next();
			if (he.getInternalLabel() == label) {
				sel.add(he);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param P
	 */
	public HE_Selection selectOnVertices(final String name, final WB_Plane P) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final HE_VertexIterator vitr = this.vItr();
		HE_Vertex v;
		while (vitr.hasNext()) {
			v = vitr.next();
			if (WB_GeometryOp3D.classifyPointToPlane3D(v, P) == WB_Classification.ON) {
				sel.add(v);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param r
	 */
	public HE_Selection selectRandomEdges(final String name, final double r) {
		HE_Selection sel = HE_Selection.getSelection(this);
		HE_EdgeIterator eItr = this.eItr();
		HE_Halfedge e;
		while (eItr.hasNext()) {
			e = eItr.next();
			if (e != null) {
				if (Math.random() < r) {
					sel.add(e);
				}
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param r
	 * @param seed
	 */
	public HE_Selection selectRandomEdges(final String name, final double r, final long seed) {
		final WB_MTRandom random = new WB_MTRandom(seed);
		HE_Selection sel = HE_Selection.getSelection(this);
		HE_EdgeIterator eItr = this.eItr();
		HE_Halfedge e;
		while (eItr.hasNext()) {
			e = eItr.next();
			if (e != null) {
				if (random.nextFloat() < r) {
					sel.add(e);
				}
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param r
	 */
	public HE_Selection selectRandomFaces(final String name, final double r) {
		HE_Selection sel = HE_Selection.getSelection(this);
		HE_FaceIterator fItr = this.fItr();
		HE_Face f;
		while (fItr.hasNext()) {
			f = fItr.next();
			if (f != null) {
				if (Math.random() < r) {
					sel.add(f);
				}
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param r
	 * @param seed
	 */
	public HE_Selection selectRandomFaces(final String name, final double r, final long seed) {
		final WB_MTRandom random = new WB_MTRandom(seed);
		HE_Selection sel = HE_Selection.getSelection(this);
		HE_FaceIterator fItr = this.fItr();
		HE_Face f;
		while (fItr.hasNext()) {
			f = fItr.next();
			if (f != null) {
				if (random.nextFloat() < r) {
					sel.add(f);
				}
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param r
	 */
	public HE_Selection selectRandomVertices(final String name, final double r) {
		HE_Selection sel = HE_Selection.getSelection(this);
		HE_VertexIterator vItr = this.vItr();
		HE_Vertex v;
		while (vItr.hasNext()) {
			v = vItr.next();
			if (v != null) {
				if (Math.random() < r) {
					sel.add(v);
				}
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param r
	 * @param seed
	 */

	public HE_Selection selectRandomVertices(final String name, final double r, final long seed) {
		final WB_MTRandom random = new WB_MTRandom(seed);
		HE_Selection sel = HE_Selection.getSelection(this);
		HE_VertexIterator vItr = this.vItr();
		HE_Vertex v;
		while (vItr.hasNext()) {
			v = vItr.next();
			if (v != null) {
				if (random.nextFloat() < r) {
					sel.add(v);
				}
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	public HE_Selection selectVerticesWithInternalLabel(final String name, final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = this.vItr();
		while (vItr.hasNext()) {
			v = vItr.next();
			if (v.getInternalLabel() == label) {
				sel.add(v);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	public HE_Selection selectVerticesWithLabel(final String name, final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = this.vItr();
		while (vItr.hasNext()) {
			v = vItr.next();
			if (v.getUserLabel() == label) {
				sel.add(v);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	public HE_Selection selectVerticesWithOtherInternalLabel(final String name, final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = this.vItr();
		while (vItr.hasNext()) {
			v = vItr.next();
			if (v.getInternalLabel() != label) {
				sel.add(v);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	public HE_Selection selectVerticesWithOtherLabel(final String name, final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = this.vItr();
		while (vItr.hasNext()) {
			v = vItr.next();
			if (v.getUserLabel() != label) {
				sel.add(v);
			}
		}
		selections.put(name, sel);
		return sel;
	}

	/**
	 *
	 * @param name
	 * @return
	 */
	public HE_Selection getSelection(final String name) {
		HE_Selection sel = selections.get(name);
		if (sel == null) {
			tracker.setDuringStatus(this, "Selection " + name + " not found.");
		}

		return sel;
	}

	/**
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	public HE_Selection copySelection(final String from, final String to) {
		HE_Selection sel = selections.get(from);
		if (sel == null) {
			tracker.setDuringStatus(this, "Selection " + from + " not found.");
		}

		HE_Selection copy = sel.get();
		addSelection(to, copy);
		return copy;
	}

	/**
	 *
	 * @param name
	 * @param sel
	 */
	public void addSelection(final String name, final HE_Selection sel) {
		if (sel.parent == this && sel != null) {
			HE_Selection prevsel = selections.get(name);
			if (prevsel == null) {
				tracker.setDuringStatus(this, "Adding to selection " + name + ".");
				selections.put(name, sel);
			} else {
				tracker.setDuringStatus(this, "Adding selection " + name + ".");
				prevsel.add(sel);
			}

		} else {
			tracker.setDuringStatus(this,
					"Selection " + name + " not added: selection is null or parent mesh is not the same.");
		}
	}

	void addSelection(final String name, final HE_Machine machine, final HE_Selection sel) {

		if (sel.parent == this && sel != null) {
			sel.createdBy = machine.getName();
			HE_Selection prevsel = selections.get(name);
			if (prevsel == null) {
				tracker.setDuringStatus(this, "Adding to selection " + name + ".");
				selections.put(name, sel);
			} else {
				tracker.setDuringStatus(this, "Adding selection " + name + ".");
				prevsel.add(sel);
			}

		} else {
			tracker.setDuringStatus(this,
					"Selection " + name + " not added: selection is null or parent mesh is not the same.");
		}
	}

	/**
	 *
	 * @param name
	 * @param sel
	 * @return
	 */
	public HE_Selection replaceSelection(final String name, final HE_Selection sel) {
		if (sel.parent == this && sel != null) {
			HE_Selection prevsel = selections.get(name);
			if (prevsel == null) {
				tracker.setDuringStatus(this, "Adding selection " + name + ".");
				selections.put(name, sel);
			} else {
				tracker.setDuringStatus(this, "Replacing selection " + name + ".");
				removeSelection(name);
				selections.put(name, sel);
			}
			return prevsel;

		} else {
			tracker.setDuringStatus(this,
					"Selection " + name + " not added: selection is null or parent mesh is not the same.");
		}
		return null;
	}

	HE_Selection replaceSelection(final String name, final HE_Machine machine, final HE_Selection sel) {

		if (sel.parent == this && sel != null) {
			sel.createdBy = machine.getName();
			HE_Selection prevsel = selections.get(name);
			if (prevsel == null) {
				tracker.setDuringStatus(this, "Adding selection " + name + ".");
				selections.put(name, sel);
			} else {
				tracker.setDuringStatus(this, "Replacing selection " + name + ".");
				removeSelection(name);
				selections.put(name, sel);
			}
			return prevsel;

		} else {
			tracker.setDuringStatus(this,
					"Selection " + name + " not added: selection is null or parent mesh is not the same.");
		}
		return null;
	}

	/**
	 *
	 * @param name
	 * @return
	 */
	public HE_Selection removeSelection(final String name) {
		HE_Selection prevsel = selections.remove(name);
		if (prevsel == null) {
			tracker.setDuringStatus(this, "Selection " + name + " not found.");
		} else {
			tracker.setDuringStatus(this, "Removed selection " + name + ".");
		}
		return prevsel;
	}

	/**
	 *
	 */
	public void cleanSelections() {
		for (HE_Selection sel : selections.values()) {
			sel.cleanSelection();
		}
	}

	/**
	 *
	 */
	public void clearSelections() {

	}

	/**
	 *
	 * @return
	 */
	public Set<String> getSelectionNames() {

		return selections.keySet();

	}

	/**
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	public boolean renameSelection(final String from, final String to) {
		HE_Selection sel = removeSelection(from);
		if (sel == null) {
			tracker.setDuringStatus(this, "Selection " + from + " not found.");
			return false;
		}
		replaceSelection(to, sel);
		tracker.setDuringStatus(this, "Renamed selection " + from + " to " + to + ".");
		return true;

	}

	/**
	 * Adds a face to the mesh. The face is also added to any selection that
	 * contains one of the elements it derives from.
	 *
	 * @param f
	 *            new face
	 * @param el
	 *            elements the face derives from
	 */
	public final void addDerivedElement(final HE_Face f, final HE_Element... el) {
		add(f);
		for (HE_Selection sel : selections.values()) {
			boolean contains = false;
			for (int i = 0; i < el.length; i++) {
				contains |= sel.contains(el[i]);
				if (contains) {
					break;
				}
			}
			if (contains) {
				sel.add(f);
			}
		}
	}

	/**
	 * Adds a halfedge to the mesh. The halfedge is also added to any selection
	 * that contains one of the elements it derives from.
	 *
	 * @param he
	 *            new halfedge
	 * @param el
	 *            elements the halfedge derives from
	 */
	public final void addDerivedElement(final HE_Halfedge he, final HE_Element... el) {
		add(he);
		for (HE_Selection sel : selections.values()) {
			boolean contains = false;
			for (int i = 0; i < el.length; i++) {
				contains |= sel.contains(el[i]);
				if (contains) {
					break;
				}
			}
			if (contains) {
				sel.add(he);
			}
		}

	}

	/**
	 * Adds a vertex to the mesh. The vertex is also added to any selection that
	 * contains one of the elements it derives from.
	 *
	 * @param v
	 *            new vertex
	 * @param el
	 *            elements the vertex derives from
	 */
	public final void addDerivedElement(final HE_Vertex v, final HE_Element... el) {
		add(v);
		for (HE_Selection sel : selections.values()) {
			boolean contains = false;
			for (int i = 0; i < el.length; i++) {
				contains |= sel.contains(el[i]);
				if (contains) {
					break;
				}
			}
			if (contains) {
				sel.add(v);
			}
		}
	}

	/**
	 * Removes face.
	 *
	 * @param f
	 *            face to remove
	 */
	@Override
	public void remove(final HE_Face f) {

		faces.remove(f);
		for (HE_Selection sel : selections.values()) {

			sel.remove(f);

		}
	}

	/**
	 * Removes halfedge.
	 *
	 * @param he
	 *            halfedge to remove
	 */
	@Override
	public void remove(final HE_Halfedge he) {
		edges.remove(he);
		halfedges.remove(he);
		unpairedHalfedges.remove(he);
		for (HE_Selection sel : selections.values()) {

			sel.remove(he);

		}
	}

	/**
	 * Removes vertex.
	 *
	 * @param v
	 *            vertex to remove
	 */
	@Override
	public void remove(final HE_Vertex v) {
		vertices.remove(v);
		for (HE_Selection sel : selections.values()) {

			sel.remove(v);

		}
	}

	@Override
	public void clearPrecomputed() {
		triangles = null;
		clearPrecomputedFaces();
		clearPrecomputedVertices();
		clearPrecomputedHalfedges();
	}

	/**
	 * Clear entire structure.
	 */
	@Override
	public void clear() {
		selections = new UnifiedMap<String, HE_Selection>();
		clearVertices();
		clearHalfedges();
		clearFaces();
	}

	/**
	 * Clear faces.
	 */
	@Override
	public void clearFaces() {
		faces = new HE_RASEC<HE_Face>();
		for (HE_Selection sel : selections.values()) {

			sel.clearFaces();

		}
	}

	/**
	 * Clear halfedges.
	 */
	@Override
	public void clearHalfedges() {
		halfedges = new HE_RASEC<HE_Halfedge>();
		edges = new HE_RASEC<HE_Halfedge>();
		unpairedHalfedges = new HE_RASEC<HE_Halfedge>();
		for (HE_Selection sel : selections.values()) {

			sel.clearHalfedges();

		}
	}

	/**
	 * Clear vertices.
	 */
	@Override
	public void clearVertices() {
		vertices = new HE_RASEC<HE_Vertex>();
		for (HE_Selection sel : selections.values()) {

			sel.clearVertices();

		}
	}

	/**
	 * Cap all remaining unpaired halfedges. Only use after pairHalfedges();
	 */
	@Override
	public void capHalfedges() {

		tracker.setStartStatus(this, "Capping unpaired halfedges.");
		final List<HE_Halfedge> unpairedHalfedges = getUnpairedHalfedges();
		final int nuh = unpairedHalfedges.size();
		final HE_Halfedge[] newHalfedges = new HE_Halfedge[nuh];
		HE_Halfedge he1, he2;
		WB_ProgressCounter counter = new WB_ProgressCounter(nuh, 10);
		tracker.setCounterStatus(this, "Capping unpaired halfedges.", counter);
		for (int i = 0; i < nuh; i++) {
			he1 = unpairedHalfedges.get(i);
			he2 = new HE_Halfedge();
			setVertex(he2, he1.getNextInFace().getVertex());
			setPair(he1, he2);
			newHalfedges[i] = he2;
			addDerivedElement(he2);
			counter.increment();
		}
		counter = new WB_ProgressCounter(nuh, 10);
		tracker.setCounterStatus(this, "Cycling new halfedges.", counter);
		for (int i = 0; i < nuh; i++) {
			he1 = newHalfedges[i];
			if (he1.getNextInFace() == null) {
				for (int j = 0; j < nuh; j++) {
					he2 = newHalfedges[j];
					if (!he2.isVisited()) {
						if (he2.getVertex() == he1.getPair().getVertex()) {
							setNext(he1, he2);
							he2.setVisited();
							break;
						}
					}
				}
			}
			counter.increment();
		}
		tracker.setStopStatus(this, "Processed unpaired halfedges.");
	}

	@Override
	public void setPair(final HE_Halfedge he1, final HE_Halfedge he2) {
		removeNoSelectionCheck(he1);
		removeNoSelectionCheck(he2);
		he1._setPair(he2);
		he2._setPair(he1);
		addDerivedElement(he1, he2);
		addDerivedElement(he2, he1);
	}

	public void setPairNoSelectionCheck(final HE_Halfedge he1, final HE_Halfedge he2) {
		removeNoSelectionCheck(he1);
		removeNoSelectionCheck(he2);
		he1._setPair(he2);
		he2._setPair(he1);

	}

	/**
	 * Add all mesh elements to this mesh. No copies are made.
	 *
	 * @param mesh
	 *            mesh to add
	 */
	@Override
	public void add(final HE_Mesh mesh) {
		addVertices(mesh);
		addFaces(mesh);
		addHalfedges(mesh);

		Set<String> selections = mesh.getSelectionNames();
		for (String name : selections) {
			HE_Selection sourceSel = mesh.getSelection(name);
			HE_Selection currentSel = getSelection(name);
			HE_Selection sel = sourceSel.get();
			sel.parent = this;
			if (currentSel == null) {
				addSelection(name, sel);
			} else {
				currentSel.add(sel);
			}

		}

	}

	public double getMeanEdgeLength() {
		double sum = 0;
		HE_EdgeIterator eItr = this.eItr();
		while (eItr.hasNext()) {
			sum += eItr.next().getLength();
		}

		return sum / this.getNumberOfEdges();
	}

	public double getAngleDefect() {

		return HET_MeshOp.getAngleDefect(this);
	}

	/**
	 * Select all mesh elements.
	 *
	 * @return current selection
	 */
	HE_Selection selectAll() {
		HE_Selection sel = HE_Selection.getSelection(this);
		sel.addFaces(this);
		sel.addHalfedges(this);
		sel.addVertices(this);

		return sel;
	}

	/**
	 *
	 * @param name
	 */
	HE_Selection selectAllEdges() {
		HE_Selection sel = HE_Selection.getSelection(this);
		sel.addEdges(this);

		return sel;
	}

	/**
	 *
	 * @param name
	 */
	HE_Selection selectAllFaces() {
		HE_Selection sel = HE_Selection.getSelection(this);
		sel.addFaces(this);

		return sel;
	}

	/**
	 *
	 * @param name
	 */
	HE_Selection selectAllHalfedges() {
		HE_Selection sel = HE_Selection.getSelection(this);
		sel.addHalfedges(this);

		return sel;
	}

	/**
	 *
	 * @param name
	 */
	HE_Selection selectAllInnerBoundaryHalfedges() {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final Iterator<HE_Halfedge> heItr = this.heItr();
		HE_Halfedge he;
		while (heItr.hasNext()) {
			he = heItr.next();
			if (he.getPair().getFace() == null) {
				sel.add(he);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 */
	HE_Selection selectAllOuterBoundaryHalfedges() {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final Iterator<HE_Halfedge> heItr = this.heItr();
		HE_Halfedge he;
		while (heItr.hasNext()) {
			he = heItr.next();
			if (he.getFace() == null) {
				sel.add(he);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 */
	HE_Selection selectAllVertices() {
		HE_Selection sel = HE_Selection.getSelection(this);
		sel.addVertices(this);

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param P
	 */
	HE_Selection selectBackEdges(final WB_Plane P) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final HE_EdgeIterator eitr = this.eItr();
		HE_Halfedge e;
		while (eitr.hasNext()) {
			e = eitr.next();
			if (HET_MeshOp.classifyEdgeToPlane3D(e, P) == WB_Classification.BACK) {
				sel.add(e);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param P
	 */
	HE_Selection selectBackFaces(final WB_Plane P) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final HE_FaceIterator fitr = this.fItr();
		HE_Face f;
		while (fitr.hasNext()) {
			f = fitr.next();
			if (HET_MeshOp.classifyFaceToPlane3D(f, P) == WB_Classification.BACK) {
				sel.add(f);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param P
	 */
	HE_Selection selectBackVertices(final WB_Plane P) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final HE_VertexIterator vitr = this.vItr();
		HE_Vertex v;
		while (vitr.hasNext()) {
			v = vitr.next();
			if (WB_GeometryOp3D.classifyPointToPlane3D(v, P) == WB_Classification.BACK) {
				sel.add(v);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 */
	HE_Selection selectBoundaryEdges() {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final HE_EdgeIterator eItr = this.eItr();
		HE_Halfedge e;
		while (eItr.hasNext()) {
			e = eItr.next();
			if (e.isInnerBoundary()) {
				sel.add(e);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 */
	HE_Selection selectBoundaryFaces() {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final Iterator<HE_Halfedge> heItr = this.heItr();
		HE_Halfedge he;
		while (heItr.hasNext()) {
			he = heItr.next();
			if (he.getFace() == null) {
				sel.add(he.getPair().getFace());
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 */
	HE_Selection selectBoundaryVertices() {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final Iterator<HE_Halfedge> heItr = this.heItr();
		HE_Halfedge he;
		while (heItr.hasNext()) {
			he = heItr.next();
			if (he.getFace() == null) {
				sel.add(he.getVertex());
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param P
	 */
	HE_Selection selectCrossingEdges(final WB_Plane P) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final HE_EdgeIterator eitr = this.eItr();
		HE_Halfedge e;
		while (eitr.hasNext()) {
			e = eitr.next();
			if (HET_MeshOp.classifyEdgeToPlane3D(e, P) == WB_Classification.CROSSING) {
				sel.add(e);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param P
	 */
	HE_Selection selectCrossingFaces(final WB_Plane P) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final HE_FaceIterator fitr = this.fItr();
		HE_Face f;
		while (fitr.hasNext()) {
			f = fitr.next();
			if (HET_MeshOp.classifyFaceToPlane3D(f, P) == WB_Classification.CROSSING) {
				sel.add(f);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	HE_Selection selectEdgesWithLabel(final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Halfedge e;
		final Iterator<HE_Halfedge> eItr = this.eItr();
		while (eItr.hasNext()) {
			e = eItr.next();
			if (e.getUserLabel() == label) {
				sel.add(e);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	HE_Selection selectEdgesWithOtherInternalLabel(final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Halfedge e;
		final Iterator<HE_Halfedge> eItr = this.eItr();
		while (eItr.hasNext()) {
			e = eItr.next();
			if (e.getInternalLabel() != label) {
				sel.add(e);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	HE_Selection selectEdgesWithOtherLabel(final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Halfedge e;
		final Iterator<HE_Halfedge> eItr = this.eItr();
		while (eItr.hasNext()) {
			e = eItr.next();
			if (e.getUserLabel() != label) {
				sel.add(e);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	HE_Selection selectEdgeWithInternalLabel(final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Halfedge e;
		final Iterator<HE_Halfedge> eItr = this.eItr();
		while (eItr.hasNext()) {
			e = eItr.next();
			if (e.getInternalLabel() == label) {
				sel.add(e);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	HE_Selection selectFacesWithInternalLabel(final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Face f;
		final Iterator<HE_Face> fItr = this.fItr();
		while (fItr.hasNext()) {
			f = fItr.next();
			if (f.getInternalLabel() == label) {
				sel.add(f);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	HE_Selection selectFacesWithLabel(final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Face f;
		final Iterator<HE_Face> fItr = this.fItr();
		while (fItr.hasNext()) {
			f = fItr.next();
			if (f.getUserLabel() == label) {
				sel.add(f);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param v
	 */
	HE_Selection selectFacesWithNormal(final WB_Coord v) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final WB_Vector w = new WB_Vector(v);
		w.normalizeSelf();
		HE_Face f;
		final Iterator<HE_Face> fItr = this.fItr();
		while (fItr.hasNext()) {
			f = fItr.next();
			if (WB_Vector.dot(f.getFaceNormal(), v) > 1.0 - WB_Epsilon.EPSILON) {
				sel.add(f);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param n
	 * @param ta
	 */
	HE_Selection selectFacesWithNormal(final WB_Coord n, final double ta) {
		HE_Selection sel = HE_Selection.getSelection(this);
		final WB_Vector nn = new WB_Vector(n);
		nn.normalizeSelf();
		final double cta = Math.cos(ta);
		HE_FaceIterator fItr = sel.parent.fItr();
		HE_Face f;
		while (fItr.hasNext()) {
			f = fItr.next();
			if (nn.dot(f.getFaceNormal()) > cta) {
				sel.add(f);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	HE_Selection selectFacesWithOtherInternalLabel(final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Face f;
		final Iterator<HE_Face> fItr = this.fItr();
		while (fItr.hasNext()) {
			f = fItr.next();
			if (f.getInternalLabel() != label) {
				sel.add(f);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	HE_Selection selectFacesWithOtherLabel(final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Face f;
		final Iterator<HE_Face> fItr = this.fItr();
		while (fItr.hasNext()) {
			f = fItr.next();
			if (f.getUserLabel() != label) {
				sel.add(f);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param P
	 */
	HE_Selection selectFrontEdges(final WB_Plane P) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final HE_EdgeIterator eitr = this.eItr();
		HE_Halfedge e;
		while (eitr.hasNext()) {
			e = eitr.next();
			if (HET_MeshOp.classifyEdgeToPlane3D(e, P) == WB_Classification.FRONT) {
				sel.add(e);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param P
	 */
	HE_Selection selectFrontFaces(final WB_Plane P) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final HE_FaceIterator fitr = this.fItr();
		HE_Face f;
		while (fitr.hasNext()) {
			f = fitr.next();
			if (HET_MeshOp.classifyFaceToPlane3D(f, P) == WB_Classification.FRONT) {
				sel.add(f);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param P
	 */
	HE_Selection selectFrontVertices(final WB_Plane P) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final HE_VertexIterator vitr = this.vItr();
		HE_Vertex v;
		while (vitr.hasNext()) {
			v = vitr.next();
			if (WB_GeometryOp3D.classifyPointToPlane3D(v, P) == WB_Classification.FRONT) {
				sel.add(v);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	HE_Selection selectHalfedgesWithLabel(final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Halfedge he;
		final Iterator<HE_Halfedge> heItr = this.heItr();
		while (heItr.hasNext()) {
			he = heItr.next();
			if (he.getUserLabel() == label) {
				sel.add(he);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	HE_Selection selectHalfedgesWithOtherInternalLabel(final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Halfedge he;
		final Iterator<HE_Halfedge> heItr = this.heItr();
		while (heItr.hasNext()) {
			he = heItr.next();
			if (he.getInternalLabel() != label) {
				sel.add(he);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	HE_Selection selectHalfedgesWithOtherLabel(final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Halfedge he;
		final Iterator<HE_Halfedge> heItr = this.heItr();
		while (heItr.hasNext()) {
			he = heItr.next();
			if (he.getUserLabel() != label) {
				sel.add(he);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	HE_Selection selectHalfedgeWithInternalLabel(final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Halfedge he;
		final Iterator<HE_Halfedge> heItr = this.heItr();
		while (heItr.hasNext()) {
			he = heItr.next();
			if (he.getInternalLabel() == label) {
				sel.add(he);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param P
	 */
	HE_Selection selectOnVertices(final WB_Plane P) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		final HE_VertexIterator vitr = this.vItr();
		HE_Vertex v;
		while (vitr.hasNext()) {
			v = vitr.next();
			if (WB_GeometryOp3D.classifyPointToPlane3D(v, P) == WB_Classification.ON) {
				sel.add(v);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param r
	 */
	HE_Selection selectRandomEdges(final double r) {
		HE_Selection sel = HE_Selection.getSelection(this);
		HE_EdgeIterator eItr = this.eItr();
		HE_Halfedge e;
		while (eItr.hasNext()) {
			e = eItr.next();
			if (e != null) {
				if (Math.random() < r) {
					sel.add(e);
				}
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param r
	 * @param seed
	 */
	HE_Selection selectRandomEdges(final double r, final long seed) {
		final WB_MTRandom random = new WB_MTRandom(seed);
		HE_Selection sel = HE_Selection.getSelection(this);
		HE_EdgeIterator eItr = this.eItr();
		HE_Halfedge e;
		while (eItr.hasNext()) {
			e = eItr.next();
			if (e != null) {
				if (random.nextFloat() < r) {
					sel.add(e);
				}
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param r
	 */
	HE_Selection selectRandomFaces(final double r) {
		HE_Selection sel = HE_Selection.getSelection(this);
		HE_FaceIterator fItr = this.fItr();
		HE_Face f;
		while (fItr.hasNext()) {
			f = fItr.next();
			if (f != null) {
				if (Math.random() < r) {
					sel.add(f);
				}
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param r
	 * @param seed
	 */
	HE_Selection selectRandomFaces(final double r, final long seed) {
		final WB_MTRandom random = new WB_MTRandom(seed);
		HE_Selection sel = HE_Selection.getSelection(this);
		HE_FaceIterator fItr = this.fItr();
		HE_Face f;
		while (fItr.hasNext()) {
			f = fItr.next();
			if (f != null) {
				if (random.nextFloat() < r) {
					sel.add(f);
				}
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param r
	 */
	HE_Selection selectRandomVertices(final double r) {
		HE_Selection sel = HE_Selection.getSelection(this);
		HE_VertexIterator vItr = this.vItr();
		HE_Vertex v;
		while (vItr.hasNext()) {
			v = vItr.next();
			if (v != null) {
				if (Math.random() < r) {
					sel.add(v);
				}
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param r
	 * @param seed
	 */

	HE_Selection selectRandomVertices(final double r, final long seed) {
		final WB_MTRandom random = new WB_MTRandom(seed);
		HE_Selection sel = HE_Selection.getSelection(this);
		HE_VertexIterator vItr = this.vItr();
		HE_Vertex v;
		while (vItr.hasNext()) {
			v = vItr.next();
			if (v != null) {
				if (random.nextFloat() < r) {
					sel.add(v);
				}
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	HE_Selection selectVerticesWithInternalLabel(final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = this.vItr();
		while (vItr.hasNext()) {
			v = vItr.next();
			if (v.getInternalLabel() == label) {
				sel.add(v);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	HE_Selection selectVerticesWithLabel(final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = this.vItr();
		while (vItr.hasNext()) {
			v = vItr.next();
			if (v.getUserLabel() == label) {
				sel.add(v);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	HE_Selection selectVerticesWithOtherInternalLabel(final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = this.vItr();
		while (vItr.hasNext()) {
			v = vItr.next();
			if (v.getInternalLabel() != label) {
				sel.add(v);
			}
		}

		return sel;
	}

	/**
	 *
	 * @param name
	 * @param label
	 */
	HE_Selection selectVerticesWithOtherLabel(final int label) {
		final HE_Selection sel = HE_Selection.getSelection(this);
		HE_Vertex v;
		final Iterator<HE_Vertex> vItr = this.vItr();
		while (vItr.hasNext()) {
			v = vItr.next();
			if (v.getUserLabel() != label) {
				sel.add(v);
			}
		}
		return sel;
	}

}