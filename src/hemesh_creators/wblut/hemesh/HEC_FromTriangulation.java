/*
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package wblut.hemesh;

import java.util.Collection;
import java.util.List;

import org.eclipse.collections.impl.list.mutable.FastList;
import wblut.geom.WB_Coord;
import wblut.geom.WB_Triangulation2D;
import wblut.geom.WB_Triangulation2DWithPoints;

/**
 * Creates a new mesh from a triangulation.
 *
 * the generic type
 *
 * @author Frederik Vanhoutte (W:Blut)
 */
public class HEC_FromTriangulation extends HEC_Creator {
	/** Source triangles. */
	WB_Triangulation2D tri;
	private List<WB_Coord> points;

	/**
	 *
	 */
	public HEC_FromTriangulation() {
		super();
		override = true;
	}

	public HEC_FromTriangulation setTriangulation(final WB_Triangulation2D tri) {
		this.tri = tri;
		return this;
	}

	public HEC_FromTriangulation setTriangulation(final WB_Triangulation2DWithPoints tri) {
		this.tri = tri;
		this.points = tri.getPoints();
		return this;
	}

	public HEC_FromTriangulation setPoints(final Collection<? extends WB_Coord> points) {
		this.points = new FastList<WB_Coord>();
		this.points.addAll(points);
		return this;
	}

	public HEC_FromTriangulation setPoints(final WB_Coord[] points) {
		this.points = new FastList<WB_Coord>();
		for (WB_Coord p : points) {
			this.points.add(p);
		}
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.hemesh.HE_Creator#create()
	 */
	@Override
	protected HE_Mesh createBase() {

		final HEC_FromFacelist ffl = new HEC_FromFacelist().setVertices(points).setFaces(tri.getTriangles())
				.setDuplicate(false);
		return ffl.createBase();

	}
}
