/*
 * HE_Mesh  Frederik Vanhoutte - www.wblut.com
 *
 * https://github.com/wblut/HE_Mesh
 * A Processing/Java library for for creating and manipulating polygonal meshes.
 *
 * Public Domain: http://creativecommons.org/publicdomain/zero/1.0/
 */

package wblut.hemesh;

import java.util.List;

import org.eclipse.collections.impl.list.mutable.FastList;

import wblut.hemesh.HE_RAS.HE_RASEC;

/**
 * Collection of mesh elements. Contains methods to manipulate selections
 *
 * @author Frederik Vanhoutte (W:Blut)
 *
 */
public class HE_Selection extends HE_MeshStructure {
	/**
	 *
	 */
	HE_Mesh parent;
	String createdBy;

	private HE_Selection() {

	}

	/**
	 * Instantiates a new HE_Selection.
	 *
	 * @param parent
	 */
	private HE_Selection(final HE_Mesh parent) {
		super();
		this.parent = parent;
	}

	static HE_Selection getSelection(final HE_Mesh parent) {
		return new HE_Selection(parent);
	}

	/**
	 * Modify the mesh.
	 *
	 * @param modifier
	 *            HE_Modifier to apply
	 * @return self
	 */
	public HE_Mesh modify(final HEM_Modifier modifier) {
		modifier.apply(this);
		clearPrecomputed();
		return this.parent;
	}

	/**
	 * Subdivide the mesh.
	 *
	 * @param subdividor
	 *            HE_Subdividor to apply
	 * @return self
	 */
	public HE_Mesh subdivide(final HES_Subdividor subdividor) {
		subdividor.apply(this);
		clearPrecomputed();
		return this.parent;
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

		for (int i = 0; i < rep; i++) {
			subdividor.apply(this);
			clearPrecomputed();
		}
		return this.parent;
	}

	/**
	 * Simplify.
	 *
	 * @param simplifier
	 *            the simplifier
	 * @return the h e_ mesh
	 */
	public HE_Mesh simplify(final HES_Simplifier simplifier) {
		simplifier.apply(this);
		clearPrecomputed();
		return this.parent;
	}

	/**
	 * Get outer edges.
	 *
	 * @return outer edges as FastList<HE_Edge>
	 */
	public List<HE_Halfedge> getOuterEdges() {
		final HE_Selection sel = get();
		sel.collectEdgesByFace();
		final List<HE_Halfedge> result = new FastList<HE_Halfedge>();
		HE_Halfedge he;
		HE_HalfedgeIterator heItr = sel.heItr();
		while (heItr.hasNext()) {
			he = heItr.next();
			if (he.isEdge()) {
				final HE_Face f1 = he.getFace();
				final HE_Face f2 = he.getPair().getFace();
				if (f1 == null || f2 == null || !contains(f1) || !contains(f2)) {
					result.add(he);
				}
			}
		}
		return result;
	}

	/**
	 * Get inner edges.
	 *
	 * @return inner edges as FastList<HE_Edge>
	 */
	public List<HE_Halfedge> getInnerEdges() {
		final HE_Selection sel = get();
		sel.collectEdgesByFace();
		final List<HE_Halfedge> result = new FastList<HE_Halfedge>();
		HE_Halfedge he;
		HE_HalfedgeIterator heItr = sel.heItr();
		while (heItr.hasNext()) {
			he = heItr.next();
			if (he.isEdge()) {
				final HE_Face f1 = he.getFace();
				final HE_Face f2 = he.getPair().getFace();
				if (!(f1 == null || f2 == null || !contains(f1) || !contains(f2))) {
					result.add(he);
				}
			}
		}
		return result;
	}

	/**
	 * Get outer vertices.
	 *
	 * @return outer vertices as FastList<HE_Vertex>
	 */
	public List<HE_Vertex> getOuterVertices() {
		final List<HE_Vertex> result = new FastList<HE_Vertex>();
		final List<HE_Halfedge> outerEdges = getOuterEdges();
		for (int i = 0; i < outerEdges.size(); i++) {
			final HE_Halfedge e = outerEdges.get(i);
			final HE_Vertex v1 = e.getVertex();
			final HE_Vertex v2 = e.getEndVertex();
			if (!result.contains(v1)) {
				result.add(v1);
			}
			if (!result.contains(v2)) {
				result.add(v2);
			}
		}
		return result;
	}

	/**
	 * Get inner vertices.
	 *
	 * @return inner vertices as FastList<HE_Vertex>
	 */
	public List<HE_Vertex> getInnerVertices() {
		final HE_Selection sel = get();
		sel.collectVertices();
		final List<HE_Vertex> result = new FastList<HE_Vertex>();
		final List<HE_Vertex> outerVertices = getOuterVertices();
		HE_VertexIterator vItr = sel.vItr();
		HE_Vertex v;
		while (vItr.hasNext()) {
			v = vItr.next();
			if (!outerVertices.contains(v)) {
				result.add(v);
			}
		}
		return result;
	}

	/**
	 * Get vertices in selection on mesh boundary.
	 *
	 * @return boundary vertices in selection as FastList<HE_Vertex>
	 */
	@Override
	public List<HE_Vertex> getBoundaryVertices() {
		final List<HE_Vertex> result = new FastList<HE_Vertex>();
		final List<HE_Halfedge> outerEdges = getOuterEdges();
		for (int i = 0; i < outerEdges.size(); i++) {
			final HE_Halfedge e = outerEdges.get(i);
			if (e.getFace() == null || e.getPair().getFace() == null) {
				final HE_Vertex v1 = e.getVertex();
				final HE_Vertex v2 = e.getEndVertex();
				if (!result.contains(v1)) {
					result.add(v1);
				}
				if (!result.contains(v2)) {
					result.add(v2);
				}
			}
		}
		return result;
	}

	/**
	 * Get outer halfedges.
	 *
	 * @return outside halfedges of outer edges as FastList<HE_halfedge>
	 */
	public List<HE_Halfedge> getOuterHalfedges() {
		final HE_Selection sel = get();
		sel.collectHalfedges();
		final List<HE_Halfedge> result = new FastList<HE_Halfedge>();
		HE_Halfedge he;
		HE_HalfedgeIterator heItr = sel.heItr();
		while (heItr.hasNext()) {
			he = heItr.next();
			final HE_Face f1 = he.getFace();
			if (f1 == null || !contains(f1)) {
				result.add(he);
			}
		}
		return result;
	}

	/**
	 * Get outer halfedges.
	 *
	 * @return inside halfedges of outer edges as FastList<HE_halfedge>
	 */
	public List<HE_Halfedge> getOuterHalfedgesInside() {
		final HE_Selection sel = get();
		sel.collectHalfedges();
		final List<HE_Halfedge> result = new FastList<HE_Halfedge>();
		HE_Halfedge he;
		HE_HalfedgeIterator heItr = sel.heItr();
		while (heItr.hasNext()) {
			he = heItr.next();
			final HE_Face f1 = he.getPair().getFace();
			if (f1 == null || !contains(f1)) {
				result.add(he);
			}
		}
		return result;
	}

	/**
	 * Get innerhalfedges.
	 *
	 * @return inner halfedges as FastList<HE_halfedge>
	 */
	public List<HE_Halfedge> getInnerHalfedges() {
		final HE_Selection sel = get();
		sel.collectHalfedges();
		final List<HE_Halfedge> result = new FastList<HE_Halfedge>();
		HE_Halfedge he;
		HE_HalfedgeIterator heItr = sel.heItr();
		while (heItr.hasNext()) {
			he = heItr.next();
			if (contains(he.getPair().getFace()) && contains(he.getFace())) {
				result.add(he);
			}
		}
		return result;
	}

	/**
	 * Copy selection.
	 *
	 * @return copy of selection
	 */
	public HE_Selection get() {
		final HE_Selection copy = new HE_Selection(parent);
		HE_FaceIterator fItr = fItr();
		HE_Face f;
		while (fItr.hasNext()) {
			f = fItr.next();
			copy.add(f);
		}
		HE_Halfedge he;
		HE_HalfedgeIterator heItr = heItr();
		while (heItr.hasNext()) {
			he = heItr.next();
			copy.add(he);
		}
		HE_VertexIterator vItr = vItr();
		HE_Vertex v;
		while (vItr.hasNext()) {
			v = vItr.next();
			copy.add(v);
		}
		copy.createdBy = createdBy == null ? null : createdBy;
		return copy;
	}

	/**
	 * Creates a submesh from the faces in the selection. The original mesh is
	 * not modified. It is not necessary to use {@link #completeFromFaces()
	 * completeFromFaces} before using this operation.
	 *
	 * @return
	 */
	public HE_Mesh getAsMesh() {
		return new HE_Mesh(new HEC_Copy(this));
	}

	/**
	 * Add all halfedges and vertices belonging to the faces of the selection,
	 * except the outer boundary halfedges that belong to other faces. This
	 * clears all vertices and halfedges that might have been part of the
	 * selection. It also makes sure that vertices only refer to halfedges
	 * inside the selection. After this operation is done, the selection is in
	 * essence a self-consistent, open submesh, lacking only the halfedge caps
	 * on the boundaries that could refer to non-included faces.
	 */
	public void completeFromFaces() {
		this.clearHalfedges();
		this.clearVertices();
		HE_FaceIterator fitr = this.fItr();
		HE_Face f;
		HE_Halfedge he;
		while (fitr.hasNext()) {
			f = fitr.next();
			final HE_FaceVertexCirculator fvcrc = new HE_FaceVertexCirculator(f);
			while (fvcrc.hasNext()) {
				add(fvcrc.next());
			}
			final HE_FaceHalfedgeInnerCirculator fheicrc = new HE_FaceHalfedgeInnerCirculator(f);
			while (fheicrc.hasNext()) {
				he = fheicrc.next();
				add(he);
				if (he.getPair().isOuterBoundary()) {
					add(he.getPair());
				}
			}
		}
		fitr = this.fItr();
		while (fitr.hasNext()) {
			f = fitr.next();
			final HE_FaceHalfedgeInnerCirculator fheicrc = new HE_FaceHalfedgeInnerCirculator(f);
			while (fheicrc.hasNext()) {
				he = fheicrc.next();
				if (!contains(he.getVertex().getHalfedge())) {
					parent.setHalfedge(he.getVertex(), he);
				}
			}
		}
	}

	/**
	 * Add selection.
	 *
	 * @param sel
	 *            selection to add
	 */
	public void add(final HE_Selection sel) {
		HE_FaceIterator fItr = sel.fItr();
		HE_Face f;
		while (fItr.hasNext()) {
			f = fItr.next();
			add(f);
		}
		HE_Halfedge he;
		HE_HalfedgeIterator heItr = sel.heItr();
		while (heItr.hasNext()) {
			he = heItr.next();
			add(he);
		}
		HE_VertexIterator vItr = sel.vItr();
		HE_Vertex v;
		while (vItr.hasNext()) {
			v = vItr.next();
			add(v);
		}
	}

	/**
	 *
	 *
	 * @param sel
	 */
	public void union(final HE_Selection sel) {
		add(sel);
	}

	/**
	 * Remove selection.
	 *
	 * @param sel
	 *            selection to remove
	 */
	public void subtract(final HE_Selection sel) {
		HE_FaceIterator fItr = sel.fItr();
		HE_Face f;
		while (fItr.hasNext()) {
			f = fItr.next();
			remove(f);
		}
		HE_Halfedge he;
		HE_HalfedgeIterator heItr = sel.heItr();
		while (heItr.hasNext()) {
			he = heItr.next();
			remove(he);
		}
		HE_VertexIterator vItr = sel.vItr();
		HE_Vertex v;
		while (vItr.hasNext()) {
			v = vItr.next();
			remove(v);
		}
	}

	/**
	 * Remove elements outside selection.
	 *
	 * @param sel
	 *            selection to check
	 */
	public void intersect(final HE_Selection sel) {
		final HE_RAS<HE_Face> newFaces = new HE_RAS.HE_RASEC<HE_Face>();
		HE_FaceIterator fItr = sel.fItr();
		HE_Face f;
		while (fItr.hasNext()) {
			f = fItr.next();
			if (contains(f)) {
				newFaces.add(f);
			}
		}
		clearFaces();
		addFaces(newFaces);
		final HE_RAS<HE_Halfedge> newHalfedges = new HE_RAS.HE_RASEC<HE_Halfedge>();
		HE_Halfedge he;
		HE_HalfedgeIterator heItr = sel.heItr();
		while (heItr.hasNext()) {
			he = heItr.next();
			if (contains(he)) {
				newHalfedges.add(he);
			}
		}
		clearHalfedges();
		addHalfedges(newHalfedges);
		final HE_RAS<HE_Vertex> newVertices = new HE_RAS.HE_RASEC<HE_Vertex>();
		HE_VertexIterator vItr = sel.vItr();
		HE_Vertex v;
		while (vItr.hasNext()) {
			v = vItr.next();
			if (contains(v)) {
				newVertices.add(v);
			}
		}
		clearVertices();
		addVertices(newVertices);
	}

	/**
	 * Grow face selection outwards by one face.
	 */
	public void grow() {
		final List<HE_Face> currentFaces = getFaces();

		for (HE_Face f : currentFaces) {
			addFaces(f.getNeighborFaces());
		}
	}

	/**
	 * Grow face selection outwards.
	 *
	 * @param n
	 *            number of faces to grow
	 */
	public void grow(final int n) {
		for (int i = 0; i < n; i++) {
			grow();
		}
	}

	/**
	 * Grow face selection inwards by one face.
	 */
	public void shrink() {
		final List<HE_Halfedge> outerEdges = getOuterEdges();
		for (int i = 0; i < outerEdges.size(); i++) {
			final HE_Halfedge e = outerEdges.get(i);
			final HE_Face f1 = e.getFace();
			final HE_Face f2 = e.getPair().getFace();
			if (f1 == null || !contains(f1)) {
				remove(f2);
			}
			if (f2 == null || !contains(f2)) {
				remove(f1);
			}
		}
	}

	/**
	 * Shrink face selection inwards.
	 *
	 * @param n
	 *            number of faces to shrink
	 */
	public void shrink(final int n) {
		for (int i = 0; i < n; i++) {
			shrink();
		}
	}

	/**
	 * Select faces surrounding current face selection.
	 */
	public void surround() {
		final FastList<HE_Face> currentFaces = new FastList<HE_Face>();
		HE_FaceIterator fItr = fItr();
		HE_Face f;
		while (fItr.hasNext()) {
			f = fItr.next();
			currentFaces.add(f);
			addFaces(f.getNeighborFaces());
		}
		removeFaces(currentFaces);
	}

	/**
	 * Select faces surrounding current face selection at a distance of n-1
	 * faces.
	 *
	 * @param n
	 *            distance to current selection
	 */
	public void surround(final int n) {
		grow(n - 1);
		surround();
	}

	/**
	 * Add faces with certain number of edges in selection to selection.
	 *
	 * @param threshold
	 *            number of edges that have to belong to the selection before a
	 *            face is added
	 */
	public void smooth(final int threshold) {
		final FastList<HE_Halfedge> currentHalfedges = new FastList<HE_Halfedge>();
		HE_HalfedgeIterator heItr = heItr();
		while (heItr.hasNext()) {
			currentHalfedges.add(heItr.next());
		}
		for (int i = 0; i < currentHalfedges.size(); i++) {
			final HE_Face f = currentHalfedges.get(i).getPair().getFace();
			if (f != null && !contains(f)) {
				int ns = 0;
				HE_Halfedge he = f.getHalfedge();
				do {
					if (contains(he.getPair().getFace())) {
						ns++;
					}
					he = he.getNextInFace();
				} while (he != f.getHalfedge());
				if (ns >= threshold) {
					add(f);
				}
			}
		}
	}

	/**
	 * Add faces with certain proportion of edges in selection to selection.
	 *
	 * @param threshold
	 *            number of edges that have to belong to the selection before a
	 *            face is added
	 */
	public void smooth(final double threshold) {
		final FastList<HE_Halfedge> currentHalfedges = new FastList<HE_Halfedge>();
		HE_HalfedgeIterator heItr = heItr();
		while (heItr.hasNext()) {
			currentHalfedges.add(heItr.next());
		}
		for (int i = 0; i < currentHalfedges.size(); i++) {
			final HE_Face f = currentHalfedges.get(i).getPair().getFace();
			if (f != null && !contains(f)) {
				int ns = 0;
				HE_Halfedge he = f.getHalfedge();
				do {
					if (contains(he.getPair().getFace())) {
						ns++;
					}
					he = he.getNextInFace();
				} while (he != f.getHalfedge());
				if (ns >= threshold * f.getFaceDegree()) {
					add(f);
				}
			}
		}
	}

	/**
	 * Invert current selection.
	 *
	 * @return inverted selection
	 */
	public HE_Selection invertSelection() {
		invertFaces();
		invertEdges();
		invertHalfedges();
		invertVertices();
		return this;
	}

	/**
	 * Invert current face selection.
	 *
	 * @return inverted face selection
	 */
	public HE_Selection invertFaces() {
		final HE_RAS<HE_Face> newFaces = new HE_RAS.HE_RASEC<HE_Face>();
		HE_FaceIterator fItr = parent.fItr();
		HE_Face f;
		while (fItr.hasNext()) {
			f = fItr.next();
			if (!contains(f)) {
				newFaces.add(f);
			}
		}
		clearFaces();
		addFaces(newFaces);
		return this;
	}

	/**
	 * Invert current edge election.
	 *
	 * @return inverted edge selection
	 */
	public HE_Selection invertEdges() {
		final HE_RAS<HE_Halfedge> newEdges = new HE_RAS.HE_RASEC<HE_Halfedge>();
		HE_EdgeIterator eItr = parent.eItr();
		HE_Halfedge e;
		while (eItr.hasNext()) {
			e = eItr.next();
			if (!contains(e)) {
				newEdges.add(e);
			}
		}
		clearEdges();
		addHalfedges(newEdges);
		return this;
	}

	/**
	 * Invert current vertex selection.
	 *
	 * @return inverted vertex selection
	 */
	public HE_Selection invertVertices() {
		final HE_RAS<HE_Vertex> newVertices = new HE_RASEC<HE_Vertex>();
		HE_VertexIterator vItr = parent.vItr();
		HE_Vertex v;
		while (vItr.hasNext()) {
			v = vItr.next();
			if (!contains(v)) {
				newVertices.add(v);
			}
		}
		clearVertices();
		addVertices(newVertices);
		return this;
	}

	/**
	 * Invert current halfedge selection.
	 *
	 * @return inverted halfedge selection
	 */
	public HE_Selection invertHalfedges() {
		final HE_RAS<HE_Halfedge> newHalfedges = new HE_RAS.HE_RASEC<HE_Halfedge>();
		HE_HalfedgeIterator heItr = parent.heItr();
		HE_Halfedge he;
		while (heItr.hasNext()) {
			he = heItr.next();
			if (!contains(he)) {
				newHalfedges.add(he);
			}
		}
		clearHalfedges();
		addHalfedges(newHalfedges);
		return this;
	}

	/**
	 * Clean current selection, removes all elements no longer part of mesh.
	 *
	 * @return current selection
	 */
	public HE_Selection cleanSelection() {

		final HE_RAS<HE_Face> newFaces = new HE_RASEC<HE_Face>();
		HE_FaceIterator fItr = fItr();
		HE_Face f;
		while (fItr.hasNext()) {
			f = fItr.next();
			if (parent.contains(f)) {
				newFaces.add(f);
			}
		}
		clearFaces();
		addFaces(newFaces);

		final HE_RAS<HE_Halfedge> newHalfedges = new HE_RAS.HE_RASEC<HE_Halfedge>();
		HE_HalfedgeIterator heItr = heItr();
		HE_Halfedge he;
		while (heItr.hasNext()) {
			he = heItr.next();
			if (parent.contains(he)) {
				newHalfedges.add(he);
			}
		}
		clearHalfedges();
		addHalfedges(newHalfedges);
		final HE_RAS<HE_Vertex> newVertices = new HE_RAS.HE_RASEC<HE_Vertex>();
		HE_VertexIterator vItr = vItr();
		HE_Vertex v;
		while (vItr.hasNext()) {
			v = vItr.next();
			if (parent.contains(v)) {
				newVertices.add(v);
			}
		}
		clearVertices();
		addVertices(newVertices);
		return this;
	}

	/**
	 * Collect vertices belonging to selection elements.
	 */
	public void collectVertices() {
		List<HE_Vertex> tmpVertices = new FastList<HE_Vertex>();
		HE_FaceIterator fItr = fItr();
		HE_Face f;
		while (fItr.hasNext()) {
			f = fItr.next();

			tmpVertices = f.getUniqueFaceVertices();
			addVertices(tmpVertices);
		}
		HE_HalfedgeIterator heItr = heItr();
		while (heItr.hasNext()) {
			add(heItr.next().getVertex());
			add(heItr.next().getEndVertex());
		}
	}

	/**
	 * Collect faces belonging to selection elements.
	 */
	public void collectFaces() {
		HE_VertexIterator vItr = vItr();
		HE_Vertex v;
		while (vItr.hasNext()) {
			v = vItr.next();
			addFaces(v.getFaceStar());
		}
		HE_HalfedgeIterator heItr = heItr();
		while (heItr.hasNext()) {
			add(heItr.next().getFace());
		}
	}

	/**
	 * Collect edges belonging to face selection.
	 */
	public void collectEdgesByFace() {
		final HE_FaceIterator fitr = fItr();
		while (fitr.hasNext()) {
			HE_FaceEdgeCirculator feCrc = fitr.next().feCrc();
			while (feCrc.hasNext()) {
				add(feCrc.next());
			}
		}
	}

	/**
	 *
	 */
	public void collectEdgesByVertex() {
		final HE_VertexIterator vitr = vItr();
		while (vitr.hasNext()) {
			addHalfedges(vitr.next().getEdgeStar());
		}
	}

	/**
	 * Collect halfedges belonging to face selection.
	 */
	public void collectHalfedges() {
		HE_FaceIterator fItr = fItr();
		HE_Face f;
		while (fItr.hasNext()) {
			f = fItr.next();
			addHalfedges(f.getFaceHalfedgesTwoSided());
		}

	}

	@Override
	public void add(final HE_Halfedge he) {
		if (he.getPair() == null) {
			unpairedHalfedges.add(he);
		} else if (he.isEdge()) {
			edges.add(he);
			// halfedges.add(he.getPair());
		} else {
			halfedges.add(he);
			// edges.add(he.getPair());
		}
	}

	public void addEdge(final HE_Halfedge he) {
		if (he.getPair() == null) {
			unpairedHalfedges.add(he);
		} else if (he.isEdge()) {
			edges.add(he);
			halfedges.add(he.getPair());
		} else {
			halfedges.add(he);
			edges.add(he.getPair());
		}
	}

	public String createdBy() {
		return createdBy == null ? "" : createdBy;
	}

}
