package com.bitwig.extensions.controllers.mackie.targets;

import com.bitwig.extensions.controllers.mackie.display.LcdDisplay;

/**
 * LCD section for the name
 *
 */
public class DisplayNameTarget {
	private final LcdDisplay display;
	private final int index;

	public DisplayNameTarget(final LcdDisplay display, final int index) {
		this.display = display;
		this.index = index;
	}

	public void sendValue(final String value) {
		this.display.sendToRow(0, index, value);
	}

}
