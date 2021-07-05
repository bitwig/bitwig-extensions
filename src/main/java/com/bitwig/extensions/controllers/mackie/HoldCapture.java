package com.bitwig.extensions.controllers.mackie;

import com.bitwig.extensions.controllers.mackie.layer.LayerConfiguration;

public class HoldCapture {
	private LayerConfiguration storedConfiguration;
	private long downTime;
	private String holdButtonName;

	public void enter(final LayerConfiguration previous, final String buttonName) {
		if (holdButtonName != null) {
			return;
		}
		storedConfiguration = previous;
		downTime = System.currentTimeMillis();
		holdButtonName = buttonName;
	}

	public LayerConfiguration endHold() {
		final LayerConfiguration config = storedConfiguration;
		storedConfiguration = null;
		return config;
	}

	public boolean exit() {
		holdButtonName = null;
		return System.currentTimeMillis() - downTime < 1200;
	}
}
