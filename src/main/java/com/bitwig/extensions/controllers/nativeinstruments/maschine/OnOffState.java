package com.bitwig.extensions.controllers.nativeinstruments.maschine;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class OnOffState extends InternalHardwareLightState {

	public static final OnOffState ON_STATE = new OnOffState(127);
	public static final OnOffState OFF_STATE = new OnOffState(0);

	private final int value;

	private OnOffState(int value) {
		super();
		this.value = value;
	}

	@Override
	public HardwareLightVisualState getVisualState() {
		return null;
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof OnOffState && equals((OnOffState) obj);
	}

	public boolean equals(final OnOffState obj) {
		if (obj == this) {
			return true;
		}
		return value == obj.value;
	}

}
