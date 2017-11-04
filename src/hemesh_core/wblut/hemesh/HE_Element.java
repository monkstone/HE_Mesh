/*
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package wblut.hemesh;

import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public abstract class HE_Element {

	protected static AtomicLong currentKey = new AtomicLong(0);
	protected final long key;
	protected long labels;

	/**
	 *
	 */
	public HE_Element() {
		key = currentKey.getAndAdd(1);
		labels = mergeLabels(-1, -1);
	}

	private static long mergeLabels(final int internal, final int external) {
		return (long) internal << 32 | external & 0xffffffffL;

	}

	protected final void setInternalLabel(final int label) {
		labels = mergeLabels(label, getUserLabel());
	}

	/**
	 *
	 *
	 * @param label
	 */
	public final void setUserLabel(final int label) {
		labels = mergeLabels(getInternalLabel(), label);
	}

	/**
	 *
	 *
	 * @return
	 */
	public final long getKey() {
		return key;
	}

	/**
	 *
	 *
	 * @return
	 */
	public final int getInternalLabel() {
		return (int) (labels >> 32);

	}

	/**
	 *
	 *
	 * @return
	 */
	public final int getUserLabel() {
		return (int) labels;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return (int) (key ^ key >>> 32);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object other) {
		if (other == null) {
			return false;
		}
		if (other == this) {
			return true;
		}
		if (!(other instanceof HE_Element)) {
			return false;
		}
		return ((HE_Element) other).getKey() == key;
	}

	/**
	 *
	 *
	 * @param el
	 */
	public void copyProperties(final HE_Element el) {
		labels = mergeLabels(el.getInternalLabel(), el.getUserLabel());
	}

	/**
	 *
	 */
	protected abstract void clear();

	/**
	 *
	 */
	protected abstract void clearPrecomputed();
}
