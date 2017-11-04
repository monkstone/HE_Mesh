/*
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package wblut.hemesh;

import wblut.math.WB_Epsilon;

/**
 * Flip face normals.
 *
 * @author Frederik Vanhoutte (W:Blut)
 *
 */
public class HEM_HideEdges extends HEM_Modifier {

	/**
	 * Instantiates a new HEM_FlipFaces.
	 */
	public HEM_HideEdges() {
		super();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.hemesh.modifiers.HEB_Modifier#modify(wblut.hemesh.HE_Mesh)
	 */
	@Override
	protected HE_Mesh applySelf(final HE_Mesh mesh) {

		HE_EdgeIterator eItr = mesh.eItr();
		HE_Halfedge e;
		while (eItr.hasNext()) {
			e = eItr.next();
			if (WB_Epsilon.isEqualAbs(e.getEdgeCosDihedralAngle(), -1.0)) {
				e.setVisible(false);
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
		HE_EdgeIterator eItr = selection.eItr();
		HE_Halfedge e;
		while (eItr.hasNext()) {
			e = eItr.next();
			if (WB_Epsilon.isEqualAbs(e.getEdgeCosDihedralAngle(), -1.0)) {
				e.setVisible(false);
			}
		}

		return selection.parent;
	}
}
