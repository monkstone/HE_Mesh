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

/**
 *
 */
public class HEM_Clean extends HEM_Modifier {

	/**
	 *
	 */
	public HEM_Clean() {
		super();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.hemesh.HE_Modifier#apply(wblut.hemesh.HE_Mesh)
	 */
	@Override
	protected HE_Mesh applySelf(final HE_Mesh mesh) {
		mesh.getFaceColors();
		mesh.getFaceLabels();
		mesh.getFaceInternalLabels();
		mesh.getFaceTextureIds();
		mesh.getFaceVisibility();

		final HEC_FromFacelist ffl = new HEC_FromFacelist().setVertices(mesh.getPoints()).setFaces(mesh.getFacesAsInt())
				.setDuplicate(true).setCheckNormals(true).setUseFaceInformation(true)
				.setFaceInformation(mesh.getFaceColors(), mesh.getFaceLabels(), mesh.getFaceVisibility(),
						mesh.getFaceInternalLabels(), mesh.getFaceTextureIds())
				.setUseVertexInformation(true).setVertexInformation(mesh.getVertexColors(), mesh.getVertexVisibility(),
						mesh.getVertexLabels(), mesh.getVertexInternalLabels());

		mesh.setNoCopy(ffl.create());
		return mesh;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.hemesh.HE_Modifier#apply(wblut.hemesh.HE_Mesh)
	 */
	@Override
	protected HE_Mesh applySelf(final HE_Selection selection) {
		return applySelf(selection.parent);
	}
}
