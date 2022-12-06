package com.bitwig.extensions.controllers.nativeinstruments.maschine;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class RgbLed extends InternalHardwareLightState {

	private static final Map<Integer, RgbLed> cache = new HashMap<Integer, RgbLed>();

	protected int color = 0;

	RgbLed(final int color) {
		super();
		this.color = color;
	}

	public static RgbLed colorOf(final int colorCode) {
		RgbLed cachedColor = cache.get(colorCode);
		if (cachedColor == null) {
			cachedColor = new RgbLed(colorCode);
			cache.put(colorCode, cachedColor);
		}
		return cachedColor;
	}

	@Override
	public HardwareLightVisualState getVisualState() {
		return null;
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof RgbLed && equals((RgbLed) obj);
	}

	public boolean equals(final RgbLed obj) {
		if (obj == this) {
			return true;
		}
		return color == obj.color;
	}

	public int getColor() {
		return color;
	}

	public int getOffColor() {
		return color;
	}

	public boolean isBlinking() {
		return false;
	}

}
