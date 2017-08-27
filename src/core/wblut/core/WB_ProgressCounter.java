/*
 * This file is part of HE_Mesh, a library for creating and manipulating meshes.
 * It is dedicated to the public domain. To the extent possible under law,
 * I , Frederik Vanhoutte, have waived all copyright and related or neighboring
 * rights.
 *
 * This work is published from Belgium. (http://creativecommons.org/publicdomain/zero/1.0/)
 *
 */
package wblut.core;

public class WB_ProgressCounter {
	protected volatile int count;
	protected volatile String caller;
	protected volatile String text;

	private int limit;
	private int percentageStep;
	private volatile int currentPercentage;
	private volatile int nextUpdate;

	static WB_ProgressTracker tracker = WB_ProgressTracker.instance();

	/**
	 *
	 */
	public WB_ProgressCounter(final int limit, final int percentageStep) {
		this.count = 0;
		this.limit = limit;
		this.percentageStep = percentageStep;
		if (percentageStep < 0) {
			this.percentageStep = 10;
		}
		if (percentageStep > 100) {
			this.percentageStep = 10;
		}
		currentPercentage = 0;
		nextUpdate = (int) (limit * 0.01 * (currentPercentage + percentageStep));
		caller = null;
		text = null;

	}

	/**
	 *
	 */
	public void increment() {
		increment(1);
	}

	/**
	 *
	 *
	 * @param inc
	 */
	public void increment(final int inc) {
		count += inc;
		if (count >= nextUpdate) {
			while (nextUpdate <= count) {
				currentPercentage += percentageStep;
				nextUpdate = (int) (limit * 0.01 * (currentPercentage + percentageStep));
			}
			tracker.setCounterStatusStr(caller, text, this);
		} else if (count >= limit) {
			tracker.setCounterStatusStr(caller, text, this);
		}
	}

	/**
	 *
	 *
	 * @return
	 */
	protected int getCount() {

		return count;
	}

	/**
	 *
	 *
	 * @return
	 */
	protected int getLimit() {
		return limit;
	}

}
