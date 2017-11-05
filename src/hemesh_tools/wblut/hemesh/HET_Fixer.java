/*
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package wblut.hemesh;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;

import org.eclipse.collections.impl.list.mutable.FastList;
import wblut.core.WB_ProgressReporter.WB_ProgressCounter;
import wblut.core.WB_ProgressReporter.WB_ProgressTracker;
import wblut.geom.WB_AABB;
import wblut.geom.WB_AABBTree;
import wblut.geom.WB_AABBTree.WB_AABBNode;
import wblut.geom.WB_GeometryOp3D;
import wblut.geom.WB_IntersectionResult;
import wblut.geom.WB_Point;
import wblut.geom.WB_RandomOnSphere;
import wblut.geom.WB_Segment;
import wblut.geom.WB_Vector;
import wblut.math.WB_Epsilon;

/**
 * @author FVH
 *
 */
public class HET_Fixer {
	public static final WB_ProgressTracker tracker = WB_ProgressTracker.instance();

	/**
	 *
	 * @param mesh
	 * @param f
	 */
	public static void deleteTwoEdgeFace(final HE_Mesh mesh, final HE_Face f) {
		if (mesh.contains(f)) {
			final HE_Halfedge he = f.getHalfedge();
			final HE_Halfedge hen = he.getNextInFace();
			if (he == he.getNextInFace(2)) {
				final HE_Halfedge hePair = he.getPair();
				final HE_Halfedge henPair = hen.getPair();
				mesh.remove(f);
				mesh.remove(he);
				mesh.setHalfedge(he.getVertex(), he.getNextInVertex());
				mesh.remove(hen);
				mesh.setHalfedge(hen.getVertex(), hen.getNextInVertex());
				mesh.setPair(hePair, henPair);

			}
		}
	}

	/**
	 *
	 */
	public static void deleteTwoEdgeFaces(final HE_Mesh mesh) {
		HE_FaceIterator fItr = mesh.fItr();
		HE_Face f;
		while (fItr.hasNext()) {
			f = fItr.next();
			final HE_Halfedge he = f.getHalfedge();
			final HE_Halfedge hen = he.getNextInFace();
			if (he == hen.getNextInFace()) {
				final HE_Halfedge hePair = he.getPair();
				final HE_Halfedge henPair = hen.getPair();
				mesh.remove(f);
				mesh.remove(he);
				mesh.setHalfedge(he.getVertex(), he.getNextInVertex());
				mesh.remove(hen);
				mesh.setHalfedge(hen.getVertex(), hen.getNextInVertex());
				mesh.setPair(hePair, henPair);

			}
		}
	}

	/**
	 *
	 * @param mesh
	 * @param v
	 */
	public static void deleteTwoEdgeVertex(final HE_Mesh mesh, final HE_Vertex v) {
		if (mesh.contains(v) && v.getVertexDegree() == 2) {
			final HE_Halfedge he0 = v.getHalfedge();
			final HE_Halfedge he1 = he0.getNextInVertex();
			final HE_Halfedge he0n = he0.getNextInFace();
			final HE_Halfedge he1n = he1.getNextInFace();
			final HE_Halfedge he0p = he0.getPair();
			final HE_Halfedge he1p = he1.getPair();
			mesh.setNext(he0p, he1n);
			mesh.setNext(he1p, he0n);
			if (he0.getFace() != null) {
				mesh.setHalfedge(he0.getFace(), he1p);
			}
			if (he1.getFace() != null) {
				mesh.setHalfedge(he1.getFace(), he0p);
			}
			mesh.setHalfedge(he0n.getVertex(), he0n);
			mesh.setHalfedge(he1n.getVertex(), he1n);
			mesh.setPair(he0p, he1p);
			mesh.remove(he0);
			mesh.remove(he1);
			mesh.remove(v);
		}
	}

	/**
	 *
	 */
	public static void deleteTwoEdgeVertices(final HE_Mesh mesh) {
		final HE_VertexIterator vitr = mesh.vItr();
		HE_Vertex v;
		final List<HE_Vertex> toremove = new FastList<HE_Vertex>();
		while (vitr.hasNext()) {
			v = vitr.next();
			if (v.getVertexDegree() == 2) {
				toremove.add(v);
			}
		}
		for (final HE_Vertex vtr : toremove) {
			deleteTwoEdgeVertex(mesh, vtr);
		}
	}

	/**
	 * Collapse all zero-length edges.
	 *
	 */
	public static void collapseDegenerateEdges(final HE_Mesh mesh) {
		final FastList<HE_Halfedge> edgesToRemove = new FastList<HE_Halfedge>();
		final Iterator<HE_Halfedge> eItr = mesh.eItr();
		HE_Halfedge e;
		while (eItr.hasNext()) {
			e = eItr.next();
			if (WB_Epsilon.isZeroSq(WB_GeometryOp3D.getSqDistance3D(e.getVertex(), e.getEndVertex()))) {
				edgesToRemove.add(e);
			}
		}
		for (int i = 0; i < edgesToRemove.size(); i++) {
			HET_MeshOp.collapseEdge(mesh, edgesToRemove.get(i));
		}
	}

	/**
	 *
	 * @param mesh
	 * @param d
	 */
	public static void collapseDegenerateEdges(final HE_Mesh mesh, final double d) {
		final FastList<HE_Halfedge> edgesToRemove = new FastList<HE_Halfedge>();
		final Iterator<HE_Halfedge> eItr = mesh.eItr();
		HE_Halfedge e;
		final double d2 = d * d;
		while (eItr.hasNext()) {
			e = eItr.next();
			if (WB_GeometryOp3D.getSqDistance3D(e.getVertex(), e.getEndVertex()) < d2) {
				edgesToRemove.add(e);
			}
		}
		for (int i = 0; i < edgesToRemove.size(); i++) {
			HET_MeshOp.collapseEdge(mesh, edgesToRemove.get(i));
		}
	}

	/**
	 *
	 *
	 * @return
	 */
	public static boolean fixNonManifoldVerticesOnePass(final HE_Mesh mesh) {
		class VertexInfo {
			FastList<HE_Halfedge> out;

			VertexInfo() {
				out = new FastList<HE_Halfedge>();
			}
		}
		final LongObjectHashMap<VertexInfo> vertexLists = new LongObjectHashMap<VertexInfo>();
		HE_Vertex v;
		VertexInfo vi;
		WB_ProgressCounter counter = new WB_ProgressCounter(mesh.getNumberOfHalfedges(), 10);
		tracker.setCounterStatus("HET_Fixer", "Classifying halfedges per vertex.", counter);
		HE_HalfedgeIterator heItr = mesh.heItr();
		HE_Halfedge he;
		while (heItr.hasNext()) {
			he = heItr.next();
			v = he.getVertex();
			vi = vertexLists.get(v.key());
			if (vi == null) {
				vi = new VertexInfo();
				vertexLists.put(v.key(), vi);
			}
			vi.out.add(he);
			counter.increment();
		}
		final List<HE_Vertex> toUnweld = new FastList<HE_Vertex>();
		counter = new WB_ProgressCounter(mesh.getNumberOfVertices(), 10);
		tracker.setCounterStatus("HET_Fixer", "Checking vertex umbrellas.", counter);
		Iterator<HE_Vertex> vItr = mesh.vItr();
		while (vItr.hasNext()) {
			v = vItr.next();
			final List<HE_Halfedge> outgoing = vertexLists.get(v.key()).out;
			final List<HE_Halfedge> vStar = v.getHalfedgeStar();
			if (outgoing.size() != vStar.size()) {
				toUnweld.add(v);
			}
		}
		vItr = toUnweld.iterator();
		counter = new WB_ProgressCounter(toUnweld.size(), 10);
		tracker.setCounterStatus("HET_Fixer", "Splitting vertex umbrellas. ", counter);

		while (vItr.hasNext()) {
			v = vItr.next();
			final List<HE_Halfedge> vHalfedges = vertexLists.get(v.key()).out;
			final List<HE_Halfedge> vStar = v.getHalfedgeStar();
			final HE_Vertex vc = new HE_Vertex(v);
			mesh.add(vc);
			for (int i = 0; i < vStar.size(); i++) {
				mesh.setVertex(vStar.get(i), vc);
			}
			mesh.setHalfedge(vc, vStar.get(0));
			for (int i = 0; i < vHalfedges.size(); i++) {
				he = vHalfedges.get(i);
				if (he.getVertex() == v) {
					mesh.setHalfedge(v, he);
					break;
				}
			}
			counter.increment();
		}
		return toUnweld.size() > 0;
	}

	public static void fixDegenerateTriangles(final HE_Mesh mesh) {
		HE_FaceIterator fItr = mesh.fItr();
		HE_Face f;
		while (fItr.hasNext()) {
			f = fItr.next();
			if (f.isDegenerate() && f.getFaceDegree() == 3 && mesh.contains(f)) {
				double d = f.getHalfedge().getLength();
				double dmax = d;
				HE_Halfedge he = f.getHalfedge();
				HE_Halfedge longesthe = he;
				if (d > WB_Epsilon.EPSILON) {
					do {
						he = he.getNextInFace();
						d = he.getLength();
						if (WB_Epsilon.isZero(d)) {
							longesthe = he;
							break;
						}

						if (d > dmax) {
							longesthe = he;
							dmax = d;
						}
					} while (he != f.getHalfedge());
				}
				mesh.deleteEdge(longesthe);
			}

		}
	}

	/**
	 *
	 */
	public static void fixNonManifoldVertices(final HE_Mesh mesh) {
		int counter = 0;
		do {
			counter++;
		} while (fixNonManifoldVerticesOnePass(mesh) || counter < 10);// Normally
		// this should
		// run at most
		// 3 or 4
		// times
	}

	/**
	 * Remove all redundant vertices in straight edges.
	 *
	 */
	public static void deleteCollinearVertices(final HE_Mesh mesh) {
		final Iterator<HE_Vertex> vItr = mesh.vItr();
		HE_Vertex v;
		HE_Halfedge he;
		while (vItr.hasNext()) {
			v = vItr.next();
			if (v.getVertexDegree() == 2) {
				he = v.getHalfedge();
				if (WB_Vector.isParallel(he.getHalfedgeTangent(), he.getNextInVertex().getHalfedgeTangent())) {
					mesh.setNext(he.getPrevInFace(), he.getNextInFace());
					mesh.setNext(he.getPair().getPrevInFace(), he.getPair().getNextInFace());
					mesh.setVertex(he.getPair().getNextInFace(), he.getNextInFace().getVertex());
					if (he.getFace() != null) {
						if (he.getFace().getHalfedge() == he) {
							mesh.setHalfedge(he.getFace(), he.getNextInFace());
						}
					}
					if (he.getPair().getFace() != null) {
						if (he.getPair().getFace().getHalfedge() == he.getPair()) {
							mesh.setHalfedge(he.getPair().getFace(), he.getPair().getNextInFace());
						}
					}
					vItr.remove();
					mesh.remove(he);
					mesh.remove(he.getPair());
				}
			}
		}
	}

	/**
	 *
	 */
	public static void deleteDegenerateTriangles(final HE_Mesh mesh) {
		final List<HE_Face> faces = mesh.getFaces();
		HE_Halfedge he;
		for (final HE_Face face : faces) {
			if (!mesh.contains(face)) {
				continue; // face already removed by a previous change
			}
			if (face.isDegenerate()) {
				final int fo = face.getFaceDegree();
				if (fo == 3) {
					HE_Halfedge degeneratehe = null;
					he = face.getHalfedge();
					do {
						if (WB_Epsilon.isZero(he.getLength())) {
							degeneratehe = he;
							break;
						}
						he = he.getNextInFace();
					} while (he != face.getHalfedge());
					if (degeneratehe != null) {
						// System.out.println("Zero length change!");
						HET_MeshOp.collapseHalfedge(mesh, he);
						continue;
					}
					he = face.getHalfedge();
					double d;
					double dmax = 0;
					do {
						d = he.getLength();
						if (d > dmax) {
							degeneratehe = he;
							dmax = d;
						}
						he = he.getNextInFace();
					} while (he != face.getHalfedge());
					// System.out.println("Deleting longest edge: " + he);
					mesh.deleteEdge(degeneratehe);
				}
			}
		}
	}

	public static void clean(final HE_Mesh mesh) {
		mesh.modify(new HEM_Clean());
	}

	/**
	 * Fix loops.
	 */
	public static void fixLoops(final HE_Mesh mesh) {
		for (final HE_Halfedge he : mesh.getHalfedges()) {
			if (he.getPrevInFace() == null) {
				HE_Halfedge hen = he.getNextInFace();
				while (hen.getNextInFace() != he) {
					hen = hen.getNextInFace();
				}
				mesh.setNext(hen, he);
			}
		}
	}

	/**
	 *
	 *
	 * @param tri
	 * @param tree
	 * @return
	 */
	static List<HET_SelfIntersectionResult> checkSelfIntersection(final HE_Face tri, final WB_AABBTree tree) {
		final List<HET_SelfIntersectionResult> selfints = new FastList<HET_SelfIntersectionResult>();
		final HE_RAS.HE_RASEC<HE_Face> candidates = new HE_RAS.HE_RASEC<HE_Face>();
		final WB_AABB aabb = tri.getAABB();
		final List<WB_AABBNode> nodes = WB_GeometryOp3D.getIntersection3D(aabb, tree);
		for (final WB_AABBNode n : nodes) {
			candidates.addAll(n.getFaces());
		}
		for (final HE_Vertex v : tri.getFaceVertices()) {
			candidates.removeAll(v.getFaceStar());
		}
		for (final HE_Face candidate : candidates) {
			if (candidate.getKey() > tri.getKey()) {// Check each face pair only
				// once
				final WB_IntersectionResult ir = WB_GeometryOp3D.getIntersection3D(tri.getHalfedge().getVertex(),
						tri.getHalfedge().getEndVertex(), tri.getHalfedge().getNextInFace().getEndVertex(),
						candidate.getHalfedge().getVertex(), candidate.getHalfedge().getEndVertex(),
						candidate.getHalfedge().getNextInFace().getEndVertex());
				if (ir.intersection && ir.object != null && !WB_Epsilon.isZero(((WB_Segment) ir.object).getLength())) {
					candidate.setInternalLabel(1);
					selfints.add(new HET_SelfIntersectionResult(tri, candidate, (WB_Segment) ir.object));
				}
			}
		}
		return selfints;
	}

	/**
	 *
	 *
	 * @param mesh
	 * @return
	 */
	public static List<HET_SelfIntersectionResult> checkSelfIntersection(final HE_Mesh mesh) {

		mesh.triangulate();
		mesh.resetFaceInternalLabels();
		final WB_AABBTree tree = new WB_AABBTree(mesh, 1);

		HE_Selection sifs = new HE_Selection(mesh);
		List<HET_SelfIntersectionResult> result = checkSelfIntersection(mesh.faces.getObjects(), tree);
		for (HET_SelfIntersectionResult sir : result) {
			sifs.add(sir.f1);
			sifs.add(sir.f2);
		}
		mesh.replaceSelection("intersection", sifs);
		return result;
	}

	/**
	 *
	 *
	 * @param faces
	 * @param tree
	 * @return
	 */
	private static List<HET_SelfIntersectionResult> checkSelfIntersection(final List<HE_Face> faces,
			final WB_AABBTree tree) {

		List<HET_SelfIntersectionResult> selfints = new FastList<HET_SelfIntersectionResult>();
		try {
			int threadCount = Runtime.getRuntime().availableProcessors();
			int dfaces = faces.size() / threadCount;
			if (dfaces < 1024) {
				dfaces = 1024;
				threadCount = (int) Math.ceil(faces.size() / 1024.0);

			}
			final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
			final List<Future<List<HET_SelfIntersectionResult>>> list = new ArrayList<Future<List<HET_SelfIntersectionResult>>>();
			int i = 0;
			for (i = 0; i < threadCount - 1; i++) {
				final Callable<List<HET_SelfIntersectionResult>> runner = new SelfIntersectionChecker(dfaces * i,
						dfaces * (i + 1) - 1, i, faces, tree);

				list.add(executor.submit(runner));
			}
			final Callable<List<HET_SelfIntersectionResult>> runner = new SelfIntersectionChecker(dfaces * i,
					faces.size() - 1, i, faces, tree);
			list.add(executor.submit(runner));

			for (Future<List<HET_SelfIntersectionResult>> future : list) {
				selfints.addAll(future.get());
			}

			executor.shutdown();

		} catch (final InterruptedException ex) {
			ex.printStackTrace();
		} catch (final ExecutionException ex) {
			ex.printStackTrace();
		}
		return selfints;
	}

	/**
	 *
	 */
	static class SelfIntersectionChecker implements Callable<List<HET_SelfIntersectionResult>> {
		int start;
		int end;
		int id;
		int[] triangles;
		List<HE_Face> faces;
		WB_AABBTree tree;

		/**
		 *
		 *
		 * @param s
		 * @param e
		 * @param id
		 * @param faces
		 * @param tree
		 */
		public SelfIntersectionChecker(final int s, final int e, final int id, final List<HE_Face> faces,
				final WB_AABBTree tree) {
			start = s;
			end = e;
			this.id = id;
			this.faces = faces;
			this.tree = tree;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public List<HET_SelfIntersectionResult> call() {
			ArrayList<HET_SelfIntersectionResult> selfints = new ArrayList<HET_SelfIntersectionResult>();
			ListIterator<HE_Face> itr = faces.listIterator(start);
			for (int i = start; i <= end; i++) {
				selfints.addAll(checkSelfIntersection(itr.next(), tree));

			}
			return selfints;
		}
	}

	/**
	 *
	 */
	public static class HET_SelfIntersectionResult {
		/**
		 *
		 */
		HE_Face f1;
		/**
		 *
		 */
		HE_Face f2;
		/**
		 *
		 */
		WB_Segment segment;

		/**
		 *
		 *
		 * @param f1
		 * @param f2
		 * @param seg
		 */
		public HET_SelfIntersectionResult(final HE_Face f1, final HE_Face f2, final WB_Segment seg) {
			this.f1 = f1;
			this.f2 = f2;
			segment = seg;
		}

		/**
		 *
		 *
		 * @return
		 */
		public HE_Face getFace1() {
			return f1;
		}

		/**
		 *
		 *
		 * @return
		 */
		public HE_Face getFace2() {
			return f2;
		}

		/**
		 *
		 *
		 * @return
		 */
		public WB_Segment getSegment() {
			return segment;
		}
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		WB_RandomOnSphere rs = new WB_RandomOnSphere().setRadius(400);
		HEC_ConvexHull creator = new HEC_ConvexHull();

		int num = (int) (Math.random() * 17 + 8);
		WB_Point[] points = new WB_Point[num];
		for (int i = 0; i < num; i++) {
			points[i] = rs.nextPoint();
		}
		creator.setPoints(points);
		creator.setN(num);
		HE_Mesh mesh = new HE_Mesh(creator);

		mesh = new HE_Mesh(new HEC_Dual(mesh).setFixNonPlanarFaces(false));

		HEM_Extrude ext = new HEM_Extrude().setChamfer(25).setRelative(false);
		mesh.modify(ext);
		HE_Selection sel = mesh.getSelection("extruded");
		ext = new HEM_Extrude().setDistance(-10);
		sel.modify(ext);
		System.out.println(checkSelfIntersection(mesh.get()).size());

	}

}