package com.bitwig.extensions.controllers.nativeinstruments.maschine;

/**
 * Preset Modes enumerator.
 */
public enum FocusMode {
	LAUNCHER("Launcher"), ARRANGER("Arranger");
	private String descriptor;

	private FocusMode(final String descriptor) {
		this.descriptor = descriptor;
	}

	public static FocusMode toMode(final String s) {
		for (final FocusMode mode : FocusMode.values()) {
			if (mode.getDescriptor().equals(s)) {
				return mode;
			}
		}
		return ARRANGER;
	}

	public String getDescriptor() {
		return descriptor;
	}

}
