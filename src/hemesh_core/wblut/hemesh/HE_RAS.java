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
 * Random Access Set of HE_Element
 * Combines advantages of an ArrayList - random access, sizeable -
 * with those of a HashMap - fast lookup, unique members -.
 */
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap;

/**
 *
 *
 * @param <E>
 */
public abstract class HE_RAS<E extends HE_Element> extends AbstractSet<E> {

	/**
	 *
	 */
	public HE_RAS() {
	}

	/**
	 *
	 *
	 * @param items
	 */
	public HE_RAS(final Collection<E> items) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.AbstractCollection#add(java.lang.Object)
	 */
	@Override
	public abstract boolean add(final E item);

	/**
	 * Override element at position <code>id</code> with last element.
	 *
	 * @param id
	 * @return
	 */
	public abstract E removeAt(final int id);

	/**
	 *
	 *
	 * @param item
	 * @return
	 */
	public abstract boolean remove(final E item);

	/**
	 *
	 *
	 * @param i
	 * @return
	 */
	public abstract E get(final int i);

	/**
	 *
	 *
	 * @param i
	 * @return
	 */
	public abstract E getWithIndex(final int i);

	/**
	 *
	 *
	 * @param key
	 * @return
	 */
	public abstract E getWithKey(final long key);

	/**
	 *
	 *
	 * @param object
	 * @return
	 */
	public abstract int indexOf(final E object);

	/**
	 *
	 *
	 * @param rnd
	 * @return
	 */
	public abstract E pollRandom(final Random rnd);

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.AbstractCollection#size()
	 */
	@Override
	public abstract int size();

	/**
	 *
	 *
	 * @param object
	 * @return
	 */
	public abstract boolean contains(final E object);

	/**
	 *
	 *
	 * @param key
	 * @return
	 */
	public abstract boolean containsKey(final Long key);

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.AbstractCollection#iterator()
	 */
	@Override
	public abstract Iterator<E> iterator();

	/**
	 *
	 *
	 * @return
	 */
	protected abstract List<E> getObjects();

	/**
	 *
	 *
	 * @param <E>
	 */
	public static class HE_RASEC<E extends HE_Element> extends HE_RAS<E> {

		/**
		 *
		 */
		FastList<E> objects;

		/**
		 *
		 */
		LongIntHashMap indices;

		/**
		 *
		 */
		public HE_RASEC() {
			objects = new FastList<E>();
			indices = new LongIntHashMap();
		}

		/**
		 *
		 *
		 * @param n
		 */
		public HE_RASEC(final int n) {
			this();
		}

		/**
		 *
		 *
		 * @param items
		 */
		public HE_RASEC(final Collection<E> items) {
			this();
			for (final E e : items) {
				add(e);
			}
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.hemesh.HE_RAS#add(wblut.hemesh.HE_Element)
		 */
		@Override
		public boolean add(final E item) {
			if (item == null) {
				return false;
			}
			if (!indices.containsKey(item.key)) {
				indices.put(item.key, objects.size());
				objects.add(item);
				return true;
			}

			return false;
		}

		/**
		 * Override element at position <code>id</code> with last element.
		 *
		 * @param id
		 * @return
		 */
		@Override
		public E removeAt(final int id) {
			if (id >= objects.size()) {
				return null;
			}
			final E res = objects.get(id);
			indices.remove(res.key);
			final E last = objects.remove(objects.size() - 1);
			// skip filling the hole if last is removed
			if (id < objects.size()) {
				indices.put(last.key, id);
				objects.set(id, last);
			}
			return res;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.hemesh.HE_RAS#remove(wblut.hemesh.HE_Element)
		 */
		@Override
		public boolean remove(final E item) {
			if (item == null) {
				return false;
			}
			// @SuppressWarnings(value = "element-type-mismatch
			final int id = indices.getIfAbsent(item.key, -1);
			if (id == -1) {
				return false;
			}
			removeAt(id);
			return true;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.hemesh.HE_RAS#get(int)
		 */
		@Override
		public E get(final int i) {
			return objects.get(i);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.hemesh.HE_RAS#getByIndex(int)
		 */
		@Override
		public E getWithIndex(final int i) {
			return objects.get(i);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.hemesh.HE_RAS#getByKey(java.lang.Long)
		 */
		@Override
		public E getWithKey(final long key) {
			final int i = indices.getIfAbsent(key, -1);
			if (i == -1) {
				return null;
			}
			return objects.get(i);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.hemesh.HE_RAS#getIndex(wblut.hemesh.HE_Element)
		 */
		@Override
		public int indexOf(final E object) {
			return indices.getIfAbsent(object.key, -1);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.hemesh.HE_RAS#pollRandom(java.util.Random)
		 */
		@Override
		public E pollRandom(final Random rnd) {
			if (objects.isEmpty()) {
				return null;
			}
			final int id = rnd.nextInt(objects.size());
			return removeAt(id);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.hemesh.HE_RAS#size()
		 */
		@Override
		public int size() {
			return objects.size();
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.hemesh.HE_RAS#contains(wblut.hemesh.HE_Element)
		 */
		@Override
		public boolean contains(final E object) {
			if (object == null) {
				return false;
			}
			return indices.containsKey(object.key);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.hemesh.HE_RAS#containsKey(java.lang.Long)
		 */
		@Override
		public boolean containsKey(final Long key) {
			return indices.containsKey(key);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.hemesh.HE_RAS#iterator()
		 */
		@Override
		public Iterator<E> iterator() {
			return objects.iterator();
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see wblut.hemesh.HE_RAS#getObjects()
		 */
		@Override
		public List<E> getObjects() {
			return objects.asUnmodifiable();
		}
	}
}