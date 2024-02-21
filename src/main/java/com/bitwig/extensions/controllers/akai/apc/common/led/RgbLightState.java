package com.bitwig.extensions.controllers.akai.apc.common.led;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extensions.framework.values.Midi;

import java.util.HashMap;
import java.util.Map;

public class RgbLightState extends InternalHardwareLightState {

    private static final Map<Integer, RgbLightState> STATE_MAP = new HashMap<>();

    public static final RgbLightState OFF = new RgbLightState(0);
    public static final RgbLightState WHITE = RgbLightState.of(3);
    public static final RgbLightState WHITE_BRIGHT = RgbLightState.of(3, LedBehavior.FULL);
    public static final RgbLightState WHITE_SEL = RgbLightState.of(3, LedBehavior.PULSE_2);
    public static final RgbLightState WHITE_DIM = RgbLightState.of(1);
    public static final RgbLightState RED = new RgbLightState(5);
    public static final RgbLightState GREEN = new RgbLightState(21);
    public static final RgbLightState RED_FULL = new RgbLightState(5, LedBehavior.FULL);
    public static final RgbLightState RED_DIM = new RgbLightState(5, LedBehavior.LIGHT_10);
    public static final RgbLightState YELLOW_FULL = new RgbLightState(13, LedBehavior.FULL);
    public static final RgbLightState YELLOW_DIM = new RgbLightState(13, LedBehavior.LIGHT_10);
    public static final RgbLightState ORANGE_FULL = new RgbLightState(9, LedBehavior.FULL);
    public static final RgbLightState ORANGE_SEL = new RgbLightState(9, LedBehavior.PULSE_2);
    public static final RgbLightState ORANGE_DIM = new RgbLightState(9, LedBehavior.LIGHT_10);
    public static final RgbLightState GREEN_PLAY = new RgbLightState(21, LedBehavior.PULSE_2);

    public static final RgbLightState MUTE_PLAY_DIM = new RgbLightState(10, LedBehavior.LIGHT_10);
    public static final RgbLightState MUTE_PLAY_FULL = new RgbLightState(10, LedBehavior.FULL);
    public static final RgbLightState SOLO_PLAY_FULL = new RgbLightState(14, LedBehavior.FULL);
    public static final RgbLightState SOLO_PLAY_YELLOW_DIM = new RgbLightState(14, LedBehavior.LIGHT_10);

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
        if (colorIndex == 0) {
            return null;
        }
        return HardwareLightVisualState.createForColor(Color.fromRGB(255, 0, 0));
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof RgbLightState other) {
            return other.colorIndex == colorIndex && other.ledBehavior == ledBehavior;
        }
        return false;
    }

}
