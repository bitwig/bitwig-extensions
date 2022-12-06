package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extensions.controllers.mackie.configurations.LayerConfiguration;

public class MenuModeAction {
	private LayerConfiguration storedConfiguration = null;
	private long downTime;

	public void enter(final LayerConfiguration previous, final String buttonName) {
		if (storedConfiguration == null) {
			storedConfiguration = previous;
		}
		downTime = System.currentTimeMillis();
	}

	public LayerConfiguration getStoredConfiguration() {
		return storedConfiguration;
	}

	public boolean exit() {
		final long diff = System.currentTimeMillis() - downTime;
		return diff > 2000;
	}
}
