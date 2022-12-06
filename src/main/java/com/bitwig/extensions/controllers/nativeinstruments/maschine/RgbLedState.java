package com.bitwig.extensions.controllers.nativeinstruments.maschine;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.controller.api.HardwareLightVisualState;

public final class RgbLedState extends RgbLed {

	private static Map<Integer, RgbLedState> cache = new HashMap<Integer, RgbLedState>();
	public static RgbLedState OFF = new RgbLedState(0);
	public static RgbLedState TRACK_ON = new RgbLedState(Colors.LIGHT_ORANGE, ColorBrightness.DARKENED);
	public static RgbLedState TRACK_OFF = new RgbLedState(Colors.LIGHT_ORANGE, ColorBrightness.BRIGHT);
	public static RgbLedState SOLO_ON = new RgbLedState(Colors.YELLOW, ColorBrightness.BRIGHT);
	public static RgbLedState SOLO_OFF = new RgbLedState(Colors.YELLOW, ColorBrightness.DARKENED);
	public static RgbLedState ARM_ON = new RgbLedState(Colors.RED, ColorBrightness.BRIGHT);
	public static RgbLedState ARM_OFF = new RgbLedState(Colors.RED, ColorBrightness.DARKENED);
	public static RgbLedState GROUP_TRACK_ACTIVE = new RgbLedState(Colors.WHITE, ColorBrightness.BRIGHT);
	public static RgbLedState GROUP_TRACK_EXISTS = new RgbLedState(Colors.WHITE, ColorBrightness.DARKENED);
	public static RgbLedState CREATE_TRACK = new RgbLedState(Colors.WHITE, ColorBrightness.DIMMED);
	public static RgbLedState WHITE_BRIGHT = new RgbLedState(Colors.WHITE, ColorBrightness.BRIGHT);
	public static RgbLedState WHITE_MID = new RgbLedState(Colors.WHITE, ColorBrightness.DIMMED);
	public static RgbLedState WHITE_DIM = new RgbLedState(Colors.WHITE, ColorBrightness.DARKENED);

	private int pulse = 0;
	private int offColor = 0;

	public RgbLedState(final int color, final int offcolor, final int pulse) {
		super(color);
		this.pulse = pulse;
		this.offColor = offcolor;
	}

	public RgbLedState(final int color, final int pulse) {
		super(color);
		this.pulse = pulse;
		this.offColor = 0;
	}

	private RgbLedState(final int color) {
		super(color);
	}

	private RgbLedState(final Colors color, final ColorBrightness brightness) {
		super(color.getIndexValue(brightness));
	}

	public static RgbLedState colorOf(final Colors color, final ColorBrightness brightness) {
		return colorOf(color.getIndexValue(brightness));
	}

	public static RgbLedState colorOf(final int colorCode) {
		RgbLedState rgbColor = cache.get(colorCode);
		if (rgbColor == null) {
			rgbColor = new RgbLedState(colorCode);
			cache.put(colorCode, rgbColor);
		}
		return rgbColor;
	}

	@Override
	public HardwareLightVisualState getVisualState() {
		return null;
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof RgbLedState && equals((RgbLedState) obj);
	}

	public boolean equals(final RgbLedState obj) {
		if (obj == this) {
			return true;
		}

		return color == obj.color && pulse == obj.pulse;
	}

	@Override
	public int getColor() {
		return color;
	}

	@Override
	public int getOffColor() {
		return offColor;
	}

	@Override
	public boolean isBlinking() {
		return pulse > 0;
	}

	public int getPulse() {
		return pulse;
	}

}
