package com.bitwig.extensions.controllers.novation.commonsmk3;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

import java.util.HashMap;
import java.util.Objects;

public class RgbState extends InternalHardwareLightState {

    private static final RgbState[] registry = new RgbState[128];
    private static final RgbState[] pulseRegistry = new RgbState[128];
    private static final HashMap<Integer, RgbState> flashingRegistry = new HashMap<>();

    public static final RgbState OFF = RgbState.of(0);
    public static final RgbState WHITE = RgbState.of(3);
    public static final RgbState DIM_WHITE = RgbState.of(1);
    public static final RgbState RED = RgbState.of(5);
    public static final RgbState RED_LO = RgbState.of(7);
    public static final RgbState YELLOW = RgbState.of(13);
    public static final RgbState YELLOW_LO = RgbState.of(15);
    public static final RgbState ORANGE = RgbState.of(9);
    public static final RgbState ORANGE_LO = RgbState.of(11);
    public static final RgbState BLUE = RgbState.of(41);
    public static final RgbState GREEN = RgbState.of(21);
    public static final RgbState BLUE_LO = RgbState.of(43);
    public static final RgbState GREEN_FLASH = RgbState.flash(21, 0);
    public static final RgbState SHIFT_INACTIVE = RgbState.of(12);
    public static final RgbState SHIFT_ACTIVE = RgbState.of(3);
    public static final RgbState BUTTON_INACTIVE = DIM_WHITE;
    public static final RgbState BUTTON_ACTIVE = WHITE;
    public static final RgbState TURQUOISE = RgbState.of(29);
    public static final RgbState ORANGE_PULSE = RgbState.pulse(9);

    private final int colorIndex;
    private final int altColorIndex;
    private LightState state;

    private RgbState(final int colorIndex, final LightState state) {
        super();
        this.colorIndex = colorIndex;
        this.state = state;
        altColorIndex = 0;
    }

    private RgbState(final int colorIndex, final LightState state, final int altColorIndex) {
        super();
        this.colorIndex = colorIndex;
        this.state = state;
        this.altColorIndex = altColorIndex;
    }

    public static RgbState flash(final int colorIndex, final int altColor) {
        final int index = Math.min(Math.max(0, colorIndex), 127);
        final int altInd = Math.min(Math.max(0, altColor), 127);
        final int lookUp = index << 8 | altInd;
        return flashingRegistry.computeIfAbsent(lookUp, key -> new RgbState(index, LightState.FLASHING, altInd));
    }

    public static RgbState pulse(final int colorIndex) {
        final int index = Math.min(Math.max(0, colorIndex), 127);
        if (pulseRegistry[index] == null) {
            pulseRegistry[index] = new RgbState(colorIndex, LightState.PULSING);
        }
        return pulseRegistry[index];
    }

    public static RgbState of(final int colorIndex) {
        final int index = Math.min(Math.max(0, colorIndex), 127);
        if (registry[index] == null) {
            registry[index] = new RgbState(index, LightState.NORMAL);
        }
        return registry[index];
    }

    @Override
    public HardwareLightVisualState getVisualState() {
        return null;
    }

    public int getColorIndex() {
        return colorIndex;
    }

    public LightState getState() {
        return state;
    }

    public void setState(final LightState state) {
        this.state = state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(colorIndex, state);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RgbState other = (RgbState) obj;
        return colorIndex == other.colorIndex && state == other.state && altColorIndex == other.altColorIndex;
    }


    public int getAltColor() {
        return altColorIndex;
    }
}
