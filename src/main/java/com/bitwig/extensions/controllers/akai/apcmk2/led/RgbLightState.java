package com.bitwig.extensions.controllers.akai.apcmk2.led;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extensions.framework.values.Midi;

public class RgbLightState extends InternalHardwareLightState {

    private static final Map<Integer, RgbLightState> STATE_MAP = new HashMap<>();

    public static final RgbLightState OFF = new RgbLightState(0);
    public static final RgbLightState WHITE = RgbLightState.of(3);
    public static final RgbLightState WHITE_DIM = RgbLightState.of(1);
    public static final RgbLightState RED = new RgbLightState(5);
    public static final RgbLightState GREEN = new RgbLightState(21);
    public static final RgbLightState GREEN_PLAY = new RgbLightState(21, LedBehavior.PULSE_2);

    private final int colorIndex;
    private final LedBehavior ledBehavior;

    public static RgbLightState of(final int colorIndex) {
        return STATE_MAP.computeIfAbsent(colorIndex | LedBehavior.FULL.getCode() << 8,
                index -> new RgbLightState(colorIndex));
    }

    public static RgbLightState of(final int colorIndex, final LedBehavior behavior) {
        return STATE_MAP.computeIfAbsent(colorIndex | behavior.getCode() << 8,
                index -> new RgbLightState(colorIndex, behavior));
    }

    public static RgbLightState forColor(final Color color) {
        if (color == null || color.getAlpha() == 0
                || color.getRed() == 0 && color.getGreen() == 0 && color.getBlue() == 0)
            return OFF;

        // TODO: Better color mapping here.
        // This will be used for manual mapping feedback
        return WHITE;
    }

    public RgbLightState behavior(final LedBehavior behavior) {
        if (this.ledBehavior == behavior) {
            return this;
        }
        return of(this.colorIndex, behavior);
    }

    private RgbLightState(final int colorIndex) {
        this(colorIndex, LedBehavior.FULL);
    }

    private RgbLightState(final int colorIndex, final LedBehavior ledBehavior) {
        this.colorIndex = colorIndex;
        this.ledBehavior = ledBehavior;
    }

    public int getColorIndex() {
        return colorIndex;
    }

    public int getMidiCode() {
        return Midi.NOTE_ON | ledBehavior.getCode();
    }

    @Override
    public HardwareLightVisualState getVisualState() {
        if (colorIndex == 0)
            return null;

        // TODO: Better visual representation
        return HardwareLightVisualState.createForColor(Color.fromRGB(1, 1, 1));
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof RgbLightState) {
            final RgbLightState other = (RgbLightState) o;
            return other.colorIndex == colorIndex && other.ledBehavior == ledBehavior;
        }
        return false;
    }

}
