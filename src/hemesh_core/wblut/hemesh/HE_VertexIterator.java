/*
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package wblut.hemesh;

import java.util.Iterator;

/**
 *
 */
public class HE_VertexIterator implements Iterator<HE_Vertex> {

	/**
	 *
	 */
	Iterator<HE_Vertex> _itr;

	/**
	 *
	 *
	 * @param vertices
	 */
	HE_VertexIterator(final HE_RAS<HE_Vertex> vertices) {
		_itr = vertices.iterator();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext() {
		return _itr.hasNext();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Iterator#next()
	 */
	@Override
	public HE_Vertex next() {
		return _itr.next();
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
