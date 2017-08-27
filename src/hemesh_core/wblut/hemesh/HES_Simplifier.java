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
 * Abstract base class for mesh reduction. Implementation should preserve mesh
 * validity.
 *
 * @author Frederik Vanhoutte (W:Blut)
 *
 */
abstract public class HES_Simplifier extends HE_Machine {
	/**
	 * Instantiates a new HES_Simplifier.
	 */
	public HES_Simplifier() {
	}

	@Override
	public HE_Mesh apply(final HE_Mesh mesh) {
		if (mesh == null || mesh.getNumberOfVertices() == 0) {
			tracker.setStopStatus(this, "Nothing to simplify.");
			return new HE_Mesh();
		}
		HE_Mesh copy = mesh.get();
		try {
			HE_Mesh result = applySelf(mesh);
			tracker.setStopStatus(this, "Mesh simplified.");

			return result;
		} catch (Exception e) {
			e.printStackTrace();
			mesh.setNoCopy(copy);
			tracker.setStopStatus(this, "Simplifier failed. Resetting mesh.");
			return mesh;
		}

	}

	@Override
	public HE_Mesh apply(final HE_Selection selection) {
		if (selection == null) {
			tracker.setStopStatus(this, "Nothing to simplify.");

			return new HE_Mesh();
		}
		HE_Mesh copy = selection.parent.get();
		try {
			HE_Mesh result = applySelf(selection);
			tracker.setStopStatus(this, "Mesh simplified.");
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			selection.parent.setNoCopy(copy);
			tracker.setStopStatus(this, "Simplifier failed. Resetting mesh.");
			return selection.parent;
		}

	}

	protected abstract HE_Mesh applySelf(final HE_Mesh mesh);

	protected abstract HE_Mesh applySelf(final HE_Selection selection);
}