/**
 *
 */
package wblut.core;

class WB_ProgressStatus {
	String caller;
	String text;
	String counterString;
	String indent;
	String time;
	String delim;
	int level;

	/**
	 *
	 *
	 * @param caller
	 * @param text
	 * @param counter
	 * @param depth
	 */
	WB_ProgressStatus(final String delim, final String caller, final String text, final WB_ProgressCounter counter,
			final int depth, final String time) {
		this.caller = caller;
		this.text = text;
		StringBuffer outputBuffer = new StringBuffer(depth);
		for (int i = 0; i < depth; i++) {
			outputBuffer.append("|");
			for (int j = 0; j < WB_ProgressTracker.indent; j++) {
				outputBuffer.append(" ");
			}
		}
		this.indent = outputBuffer.toString();

		this.counterString = counter.getLimit() > 0 ? " (" + counter.getCount() + " of " + counter.getLimit() + ")"
				: "";
		level = depth;
		this.time = new String(time);
		this.delim = delim;
	}

	/**
	 *
	 *
	 * @param caller
	 * @param text
	 * @param depth
	 */
	WB_ProgressStatus(final String delim, final String caller, final String text, final int depth, final String time) {
		this.caller = caller;
		this.text = text;
		StringBuffer outputBuffer = new StringBuffer(depth);
		for (int i = 0; i < depth; i++) {
			outputBuffer.append("|");
			for (int j = 0; j < WB_ProgressTracker.indent; j++) {
				outputBuffer.append(" ");
			}
		}
		this.indent = outputBuffer.toString();
		this.counterString = null;
		this.time = new String(time);
		this.delim = delim;
		level = depth;
	}

	WB_ProgressStatus(final String caller, final int depth, final String time) {
		this.caller = "spacer";
		StringBuffer outputBuffer = new StringBuffer(caller.length());
		for (int i = 0; i < caller.length() + 1; i++) {
			outputBuffer.append("-");
		}

		this.text = outputBuffer.toString();

		outputBuffer = new StringBuffer(depth);
		for (int i = 0; i < time.length() + 1; i++) {
			outputBuffer.append(" ");
		}
		for (int i = 0; i < depth; i++) {
			outputBuffer.append("|");
			for (int j = 0; j < WB_ProgressTracker.indent; j++) {
				outputBuffer.append(" ");
			}
		}

		this.indent = outputBuffer.toString();
		this.counterString = null;
		this.time = new String(time);
		this.delim = "|";
		level = depth;
	}

	/**
	 *
	 *
	 * @return
	 */
	String getText() {
		if (caller == null) {
			return null;
		} else if (caller.equals("spacer")) {
			return indent + text;
		}
		if (text == " ") {
			return "";
		}
		if (counterString == null) {
			return time + " " + indent + delim + " " + caller + ": " + text;
		}
		return time + " " + indent + delim + " " + caller + ": " + text + counterString;

	}

	String getConsoleText() {
		if (caller == null) {
			return null;
		} else if (caller.equals("spacer")) {
			return indent + text;
		}
		if (text == " ") {
			return "";
		}
		if (counterString == null) {
			return time + " " + indent + (delim.equals("|") ? "|" : "*") + " " + caller + ": " + text;
		}
		return time + " " + indent + (delim.equals("|") ? "|" : "*") + " " + caller + ": " + text + counterString;

	}

	int getLevel() {
		return level;

	}

}