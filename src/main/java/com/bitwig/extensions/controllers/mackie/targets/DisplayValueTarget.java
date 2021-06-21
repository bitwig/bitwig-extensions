package com.bitwig.extensions.controllers.mackie.targets;

import com.bitwig.extensions.controllers.mackie.display.LcdDisplay;

public class DisplayValueTarget {
	private final LcdDisplay display;
	private final int index;
	private String lastValue = "";

	public DisplayValueTarget(final LcdDisplay display, final int index) {
		this.display = display;
		this.index = index;
	}

	public void sendValue(final String value) {
		lastValue = reduceValue(value);
		this.display.sendToRow(1, index, lastValue);
	}

	private String reduceValue(final String valueText) {
		if (valueText.startsWith("+")) {
			return valueText.substring(1).replaceAll(" ", "");
		}
		if (valueText.startsWith("-")) {
			return valueText.replaceAll(" ", "");
		}
		return valueText;
	}

	public void refresh() {
		this.display.sendToRow(1, index, lastValue);
	}

}
