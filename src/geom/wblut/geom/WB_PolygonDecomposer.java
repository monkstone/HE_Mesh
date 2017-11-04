/*
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package wblut.geom;

import java.util.List;

import org.eclipse.collections.impl.list.mutable.FastList;

/**
 * Mark Bayazit's algorithm for concave polygon decomposition
 * https://mpen.ca/406/bayazit
 *
 *
 */
public class WB_PolygonDecomposer {
	private static WB_GeometryFactory gf = new WB_GeometryFactory();

	/**
	 * Decompose the given polygon into a reasonably small number of convex
	 * polygons. Additional points can be introduced by the algorithm. There is
	 * no guarantee that the number of polygons is optimal.
	 *
	 * @param polygon
	 *            input polygon
	 * @return List<WB_Polygon>
	 */
	public static List<WB_Polygon> decomposePolygon2D(WB_Polygon polygon) {
		if (!polygon.isSimple()) {
			polygon = gf.createSimplePolygon(polygon);
		}
		List<WB_Polygon> polys = new FastList<WB_Polygon>();
		if (polygon == null) {
			return polys;
		}
		int size = polygon.getNumberOfShellPoints();
		if (size < 4) {
			polys.add(polygon);
			return polys;
		}
		decomposePolygon(polygon.getPoints(), polys);
		return polys;
	}

	private static void decomposePolygon(final List<WB_Point> pointlist, final List<WB_Polygon> accumulator) {
		int n = pointlist.size();
		WB_Point upperIntersection = gf.createPoint();
		WB_Point lowerIntersection = gf.createPoint();
		double upperDistance = Double.MAX_VALUE;
		double lowerDistance = Double.MAX_VALUE;
		double closestDistance = Double.MAX_VALUE;
		int upperIndex = 0;
		int lowerIndex = 0;
		int closestIndex = 0;

		List<WB_Point> lower = new FastList<WB_Point>();
		List<WB_Point> upper = new FastList<WB_Point>();

		for (int i = 0; i < n; i++) {
			WB_Point iVertex = pointlist.get(i);
			WB_Point iVertexPrev = pointlist.get(i == 0 ? n - 1 : i - 1);
			WB_Point iVertexNext = pointlist.get(i + 1 == n ? 0 : i + 1);
			if (WB_GeometryOp3D.isReflex2D(iVertexPrev, iVertex, iVertexNext)) {
				for (int j = 0; j < n; j++) {
					WB_Point jVertex = pointlist.get(j);
					WB_Point jVertexPrev = pointlist.get(j == 0 ? n - 1 : j - 1);
					WB_Point jVertexNext = pointlist.get(j + 1 == n ? 0 : j + 1);
					WB_Point intersection = gf.createPoint();
					if (WB_GeometryOp3D.isLeftStrict2D(iVertexPrev, iVertex, jVertex)
							&& WB_GeometryOp3D.isRight2D(iVertexPrev, iVertex, jVertexPrev)) {
						if (WB_GeometryOp3D.getLineIntersectionInto2D(iVertexPrev, iVertex, jVertex, jVertexPrev,
								intersection)) {
							if (WB_GeometryOp3D.isRightStrict2D(iVertexNext, iVertex, intersection)) {
								double dist = iVertex.getSqDistance2D(intersection);
								if (dist < lowerDistance) {
									lowerDistance = dist;
									lowerIntersection.set(intersection);
									lowerIndex = j;
								}
							}
						}
					}
					if (WB_GeometryOp3D.isLeftStrict2D(iVertexNext, iVertex, jVertexNext)
							&& WB_GeometryOp3D.isRight2D(iVertexNext, iVertex, jVertex)) {
						if (WB_GeometryOp3D.getLineIntersectionInto2D(iVertexNext, iVertex, jVertex, jVertexNext,
								intersection)) {
							if (WB_GeometryOp3D.isLeftStrict2D(iVertexPrev, iVertex, intersection)) {
								double dist = iVertex.getSqDistance2D(intersection);
								if (dist < upperDistance) {
									upperDistance = dist;
									upperIntersection.set(intersection);
									upperIndex = j;
								}
							}
						}
					}
				}
				if (lowerIndex == (upperIndex + 1) % n) {
					WB_Point midpoint = upperIntersection.add(lowerIntersection).mulSelf(0.5);
					if (i < upperIndex) {
						lower.addAll(pointlist.subList(i, upperIndex + 1));
						lower.add(midpoint);
						upper.add(midpoint);
						if (lowerIndex != 0) {
							upper.addAll(pointlist.subList(lowerIndex, n));
						}
						upper.addAll(pointlist.subList(0, i + 1));
					} else {
						if (i != 0) {
							lower.addAll(pointlist.subList(i, n));
						}
						lower.addAll(pointlist.subList(0, upperIndex + 1));
						lower.add(midpoint);
						upper.add(midpoint);
						upper.addAll(pointlist.subList(lowerIndex, i + 1));
					}
				} else {
					if (lowerIndex > upperIndex) {
						upperIndex += n;
					}
					closestIndex = lowerIndex;
					for (int j = lowerIndex; j <= upperIndex; j++) {
						int jmod = j % n;
						WB_Point q = pointlist.get(jmod);

						if (q == iVertex || q == iVertexPrev || q == iVertexNext) {
							continue;
						}
						if (isVisible(pointlist, i, jmod)) {
							double dist = iVertex.getSqDistance2D(q);
							if (dist < closestDistance) {
								closestDistance = dist;
								closestIndex = jmod;
							}
						}
					}
					if (i < closestIndex) {
						lower.addAll(pointlist.subList(i, closestIndex + 1));
						if (closestIndex != 0) {
							upper.addAll(pointlist.subList(closestIndex, n));
						}
						upper.addAll(pointlist.subList(0, i + 1));
					} else {
						if (i != 0) {
							lower.addAll(pointlist.subList(i, n));
						}
						lower.addAll(pointlist.subList(0, closestIndex + 1));
						upper.addAll(pointlist.subList(closestIndex, i + 1));
					}
				}
				if (lower.size() < upper.size()) {
					decomposePolygon(lower, accumulator);
					decomposePolygon(upper, accumulator);
				} else {
					decomposePolygon(upper, accumulator);
					decomposePolygon(lower, accumulator);
				}
				return;
			}
		}
		if (pointlist.size() < 3) {
			return;
		}
		accumulator.add(gf.createSimplePolygon(pointlist));
	}

	private static boolean isVisible(final List<WB_Point> pointlist, final int i, final int j) {
		int n = pointlist.size();
		WB_Point iVertex, jVertex;
		iVertex = pointlist.get(i);
		jVertex = pointlist.get(j);
		WB_Point iVertexPrev, iVertexNext, jVertexPrev, jVertexNext;
		iVertexPrev = pointlist.get(i == 0 ? n - 1 : i - 1);
		iVertexNext = pointlist.get(i + 1 == n ? 0 : i + 1);
		jVertexPrev = pointlist.get(j == 0 ? n - 1 : j - 1);
		jVertexNext = pointlist.get(j + 1 == n ? 0 : j + 1);

		if (WB_GeometryOp3D.isReflex2D(iVertexPrev, iVertex, iVertexNext)) {
			if (WB_GeometryOp3D.isLeft2D(iVertex, iVertexPrev, jVertex)
					&& WB_GeometryOp3D.isRight2D(iVertex, iVertexNext, jVertex)) {
				return false;
			}
		} else {
			if (WB_GeometryOp3D.isRight2D(iVertex, iVertexNext, jVertex)
					|| WB_GeometryOp3D.isLeft2D(iVertex, iVertexPrev, jVertex)) {
				return false;
			}
		}
		if (WB_GeometryOp3D.isReflex2D(jVertexPrev, jVertex, jVertexNext)) {
			if (WB_GeometryOp3D.isLeft2D(jVertex, jVertexPrev, iVertex)
					&& WB_GeometryOp3D.isRight2D(jVertex, jVertexNext, iVertex)) {
				return false;
			}
		} else {
			if (WB_GeometryOp3D.isRight2D(jVertex, jVertexNext, iVertex)
					|| WB_GeometryOp3D.isLeft2D(jVertex, jVertexPrev, iVertex)) {
				return false;
			}
		}
		for (int k = 0; k < n; k++) {
			int knext = k + 1 == n ? 0 : k + 1;
			if (k == i || k == j || knext == i || knext == j) {
				continue;
			}
			WB_Point kVertex = pointlist.get(k);
			WB_Point kVertexNext = pointlist.get(knext);

			WB_Coord in = WB_GeometryOp3D.getSegmentIntersection2D(iVertex, jVertex, kVertex, kVertexNext);
			if (in != null) {
				return false;
			}
		}

		return true;
	}

}
