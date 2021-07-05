package com.bitwig.extensions.controllers.mackie.layer;

public class MenuModeAction {
	private LayerConfiguration storedConfiguration = null;
	private long downTime;
	private String activeButton;

	public void enter(final LayerConfiguration previous, final String buttonName) {
		if (storedConfiguration == null) {
			storedConfiguration = previous;
		}
		downTime = System.currentTimeMillis();
		activeButton = buttonName;
	}

	public LayerConfiguration getStoredConfiguration() {
		return storedConfiguration;
	}

	public boolean exit() {
		final long diff = System.currentTimeMillis() - downTime;
		return diff > 2000;
	}
}
