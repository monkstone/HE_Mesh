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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class WB_ProgressReporter extends Thread {

	WB_ProgressTracker tracker;
	WB_ProgressStatus status;
	PrintStream output;
	String path;

	/**
	 *
	 */
	public WB_ProgressReporter(final String path) {
		this(0, path, false);
	}

	/**
	 *
	 */
	public WB_ProgressReporter(final int depth, final int consoleDepth, final String path) {
		this(depth, path, false);
	}

	public WB_ProgressReporter(final int depth, final String path, final boolean append) {
		super();
		tracker = WB_ProgressTracker.instance();
		tracker.setMaxLevel(depth);
		this.path = path;
		try {
			output = new PrintStream(new BufferedOutputStream(new FileOutputStream(path, append)), true, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Thread#start()
	 */
	@Override
	public void start() {
		super.start();
		System.out.println("Starting WB_ProgressTracker: " + path);
		System.out.println("");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		while (!Thread.interrupted()) {
			try {

				while (tracker.isUpdated()) {
					status = tracker.getStatus();
					if (status != null) {
						String s = status.getText();
						if (s != null) {
							output.println(s);
							if (status.getLevel() < 2) {
								System.out.println(status.getConsoleText());
							}
						}
					}
				}
			} catch (final Exception e) {

				e.printStackTrace();
			}
		}
	}
}
