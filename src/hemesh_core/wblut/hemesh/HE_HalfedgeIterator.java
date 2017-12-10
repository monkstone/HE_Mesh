/*
 * HE_Mesh  Frederik Vanhoutte - www.wblut.com
 *
 * https://github.com/wblut/HE_Mesh
 * A Processing/Java library for for creating and manipulating polygonal meshes.
 *
 * Public Domain: http://creativecommons.org/publicdomain/zero/1.0/
 */

package wblut.hemesh;

import java.util.Collection;
import java.util.Iterator;

/**
 *
 */
class HE_HalfedgeIterator implements Iterator<HE_Halfedge> {

	/**
	 *
	 */
	Iterator<HE_Halfedge> _itrhe;

	/**
	 *
	 *
	 * @param edges
	 * @param halfedges
	 */
	HE_HalfedgeIterator(final Collection<HE_Halfedge> halfedges) {

		_itrhe = halfedges.iterator();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext() {
		return _itrhe.hasNext();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.Iterator#next()
	 */
	@Override
	public HE_Halfedge next() {
		return _itrhe.next();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.Iterator#remove()
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
