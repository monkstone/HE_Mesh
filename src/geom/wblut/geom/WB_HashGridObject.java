/*
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package wblut.geom;

import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;

public class WB_HashGridObject {

	@SuppressWarnings("rawtypes")
	private final LongObjectHashMap values;

	private final Object defaultValue;

	private final int K, L, M, KL;

	/**
	 *
	 *
	 * @param K
	 * @param L
	 * @param M
	 * @param defaultValue
	 */
	@SuppressWarnings("rawtypes")
	public WB_HashGridObject(final int K, final int L, final int M, final Object defaultValue) {
		this.K = K;
		this.L = L;
		this.M = M;
		KL = K * L;
		this.defaultValue = defaultValue;
		values = new LongObjectHashMap();
	}

	/**
	 *
	 *
	 * @param K
	 * @param L
	 * @param M
	 */
	@SuppressWarnings("rawtypes")
	public WB_HashGridObject(final int K, final int L, final int M) {
		this.K = K;
		this.L = L;
		this.M = M;
		KL = K * L;
		defaultValue = null;
		values = new LongObjectHashMap();
	}

	/**
	 *
	 *
	 * @param value
	 * @param i
	 * @param j
	 * @param k
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean setValue(final Object value, final int i, final int j, final int k) {
		final long id = safeIndex(i, j, k);
		if (id > 0) {
			values.put(id, value);
			return true;
		}
		return false;
	}

	/**
	 *
	 *
	 * @param i
	 * @param j
	 * @param k
	 * @return
	 */
	public boolean clearValue(final int i, final int j, final int k) {
		final long id = safeIndex(i, j, k);
		if (id > 0) {
			values.remove(id);
			return true;
		}
		return false;
	}

	/**
	 *
	 *
	 * @param i
	 * @param j
	 * @param k
	 * @return
	 */
	public Object getValue(final int i, final int j, final int k) {
		final long id = safeIndex(i, j, k);
		if (id == -1) {
			return defaultValue;
		}
		if (id >= 0) {
			final Object val = values.get(id);
			return val;
		}
		return defaultValue;
	}

	/**
	 *
	 *
	 * @param i
	 * @param j
	 * @param k
	 * @return
	 */
	private long safeIndex(final int i, final int j, final int k) {
		if (i < 0) {
			return -1;
		}
		if (i > K - 1) {
			return -1;
		}
		if (j < 0) {
			return -1;
		}
		if (j > L - 1) {
			return -1;
		}
		if (k < 0) {
			return -1;
		}
		if (k > M - 1) {
			return -1;
		}
		return i + j * K + k * KL;
	}

	/**
	 *
	 *
	 * @return
	 */
	public int getWidth() {
		return K;
	}

	/**
	 *
	 *
	 * @return
	 */
	public int getHeight() {
		return L;
	}

	/**
	 *
	 *
	 * @return
	 */
	public int getDepth() {
		return M;
	}

	/**
	 *
	 *
	 * @return
	 */
	public Object getDefaultValue() {
		return defaultValue;
	}

	/**
	 *
	 *
	 * @return
	 */
	public long[] getKeys() {
		return values.keySet().toArray();
	}

	/**
	 *
	 *
	 * @return
	 */
	public int size() {
		return values.size();
	}
}
