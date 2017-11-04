/*
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package wblut.core;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import wblut.hemesh.HE_Element;

public class WB_ProgressTracker {

	protected Queue<WB_ProgressStatus> statuses;
	protected volatile int level;
	static int indent = 3;
	protected volatile int maxLevel;
	final int INCLVL = +1;
	final int DECLVL = -1;

	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	/**
	 *
	 */
	protected WB_ProgressTracker() {
		statuses = new ConcurrentLinkedQueue<WB_ProgressStatus>();
		level = 0;
		maxLevel = 2;
	}

	private static final WB_ProgressTracker tracker = new WB_ProgressTracker();

	/**
	 *
	 *
	 * @return
	 */
	public static WB_ProgressTracker instance() {
		return tracker;
	}

	/**
	 *
	 *
	 * @param indent
	 */
	public void setIndent(final int indent) {
		WB_ProgressTracker.indent = Math.max(0, indent);

	}

	/**
	 *
	 *
	 * @return
	 */
	protected WB_ProgressStatus getStatus() {
		if (statuses.size() > 0) {
			return statuses.poll();
		}
		return null;

	}

	public void setStartStatus(final Object caller, final String status) {
		if (level <= maxLevel) {
			String key = "";
			if (caller instanceof HE_Element) {
				key = " (key: " + ((HE_Element) caller).getKey() + ")";
			}
			statuses.add(new WB_ProgressStatus("\u250C", caller.getClass().getSimpleName() + key, status, level,
					sdf.format(new Date().getTime())));
		}
		level = Math.max(0, level + INCLVL);
	}

	public void setStopStatus(final Object caller, final String status) {
		level = Math.max(0, level + DECLVL);
		if (level <= maxLevel) {
			String key = "";
			if (caller instanceof HE_Element) {
				key = " (key: " + ((HE_Element) caller).getKey() + ")";
			}
			statuses.add(new WB_ProgressStatus("\u2514", caller.getClass().getSimpleName() + key, status, level,
					sdf.format(new Date().getTime())));
		}
	}

	public void setDuringStatus(final Object caller, final String status) {
		if (level <= maxLevel) {
			String key = "";
			if (caller instanceof HE_Element) {
				key = " (key: " + ((HE_Element) caller).getKey() + ")";
			}
			statuses.add(new WB_ProgressStatus("|", caller.getClass().getSimpleName() + key, status, level,
					sdf.format(new Date().getTime())));
		}
	}

	public void setStartStatusStr(final String caller, final String status) {

		if (level <= maxLevel) {
			statuses.add(new WB_ProgressStatus("\u250C", caller, status, level, sdf.format(new Date().getTime())));
		}

		level = Math.max(0, level + INCLVL);

	}

	public void setStopStatusStr(final String caller, final String status) {

		level = Math.max(0, level + DECLVL);
		if (level <= maxLevel) {
			statuses.add(new WB_ProgressStatus("\u2514", caller, status, level, sdf.format(new Date().getTime())));
		}
	}

	public void setDuringStatusStr(final String caller, final String status) {
		if (level <= maxLevel) {
			statuses.add(new WB_ProgressStatus("|", caller, status, level, sdf.format(new Date().getTime())));
		}
	}

	/**
	 *
	 *
	 * @param caller
	 * @param status
	 * @param counter
	 */
	public void setCounterStatus(final Object caller, final String status, final WB_ProgressCounter counter) {

		if (counter.getLimit() > 0) {
			String key = "";
			if (caller instanceof HE_Element) {
				key = " (key: " + ((HE_Element) caller).getKey() + ")";
			}
			counter.caller = caller.getClass().getSimpleName() + key;
			counter.text = status;
			if (level <= maxLevel) {
				statuses.add(new WB_ProgressStatus("|", caller.getClass().getSimpleName() + key, status, counter, level,
						sdf.format(new Date().getTime())));
			}
		}
	}

	/**
	 *
	 *
	 * @param caller
	 * @param status
	 * @param counter
	 */
	public void setCounterStatusStr(final String caller, final String status, final WB_ProgressCounter counter) {
		if (counter.getLimit() > 0) {
			if (level <= maxLevel) {
				statuses.add(
						new WB_ProgressStatus("|", caller, status, counter, level, sdf.format(new Date().getTime())));
			}
		}
	}

	public void setSpacer(final String caller) {
		if (level <= maxLevel) {
			statuses.add(new WB_ProgressStatus(caller, level, sdf.format(new Date().getTime())));
		}
	}

	public void setSpacer(final Object caller) {
		if (level <= maxLevel) {
			statuses.add(
					new WB_ProgressStatus(caller.getClass().getSimpleName(), level, sdf.format(new Date().getTime())));
		}
	}

	/**
	 *
	 *
	 * @return
	 */
	public boolean isUpdated() {

		return statuses.size() > 0;

	}

	public void setMaxLevel(final int maxLevel) {
		this.maxLevel = maxLevel;

	}
}
