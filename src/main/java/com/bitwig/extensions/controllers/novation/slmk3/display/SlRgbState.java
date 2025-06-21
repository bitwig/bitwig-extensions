package com.bitwig.extensions.controllers.novation.slmk3.display;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class SlRgbState extends InternalHardwareLightState {
    
    private static final int DIM_VAL = 20;
    private static final Map<Integer, SlRgbState> LOOKUP = new HashMap<>();
    
    public static final SlRgbState OFF = new SlRgbState(0, 0, 0, 1);
    public static final SlRgbState RED = new SlRgbState(127, 0, 0, 1);
    public static final SlRgbState RED_BLINK = new SlRgbState(127, 0, 0, 2);
    public static final SlRgbState RED_PULSE = new SlRgbState(127, 0, 0, 3);
    public static final SlRgbState RED_DIM = new SlRgbState(DIM_VAL, 0, 0, 1);
    
    public static final SlRgbState RED_CL = new SlRgbState(127, 0, 30, 1);
    public static final SlRgbState RED_CL_DIM = new SlRgbState(DIM_VAL, 0, 10, 1);
    
    public static final SlRgbState GREEN = new SlRgbState(0, 127, 0, 1);
    public static final SlRgbState GREEN_DIM = new SlRgbState(0, DIM_VAL, 0, 1);
    public static final SlRgbState BLUE = new SlRgbState(0, 40, 127, 1);
    public static final SlRgbState CYAN = new SlRgbState(0, 127, 127, 1);
    public static final SlRgbState BITWIG_BLUE = new SlRgbState(6, 40, 100, 1);
    public static final SlRgbState DEEP_BLUE = new SlRgbState(2, 10, 127, 1);
    
    public static final SlRgbState BLUE_DIM = new SlRgbState(0, 2, 10, 1);
    public static final SlRgbState GREEN_PULSE = new SlRgbState(0, 127, 0, 3);
    public static final SlRgbState GREEN_BLINK = new SlRgbState(0, 127, 0, 2);
    public static final SlRgbState YELLOW = new SlRgbState(127, 127, 0, 1);
    public static final SlRgbState WHITE = new SlRgbState(127, 127, 127, 1);
    public static final SlRgbState GRAY = new SlRgbState(50, 50, 50, 1);
    public static final SlRgbState WHITE_DIM = new SlRgbState(DIM_VAL, DIM_VAL, DIM_VAL, 1);
    public static final SlRgbState DARK_GRAY = new SlRgbState(3, 3, 3, 1);
    public static final SlRgbState WHITE_BLINK = new SlRgbState(127, 127, 127, 2);
    public static final SlRgbState ORANGE = new SlRgbState(127, 20, 0, 1);
    public static final SlRgbState BITWIG_ORANGE = new SlRgbState(127, 30, 3, 1);
    public static final SlRgbState DARK_GREEN = new SlRgbState(10, 100, 20, 1);
    public static final SlRgbState PURPLE = new SlRgbState(127, 0, 127, 1);
    public static final SlRgbState PINK = new SlRgbState(127, 60, 60, 1);
    
    private final int red;
    private final int green;
    private final int blue;
    private final int behavior;
    private final SlRgbState otherColor;
    
    public static SlRgbState get(final int red, final int green, final int blue, final int behavior) {
        final int code = (red & 0x7F) | ((green & 0x7F) << 7) | ((blue & 0x7F) << 14) | ((behavior & 0x7F) << 21);
        return LOOKUP.computeIfAbsent(code, key -> new SlRgbState(red, green, blue, behavior));
    }
    
    public static SlRgbState get(final int red, final int green, final int blue) {
        return get(red, green, blue, 1);
    }
    
    public static SlRgbState get(final double r, final double g, final double b) {
        final int red = (int) (r * 127);
        final int green = (int) (g * 127);
        final int blue = (int) (b * 127);
        final int code = (red & 0x7F) | ((green & 0x7F) << 7) | ((blue & 0x7F) << 14) | (0x1 << 21);
        return LOOKUP.computeIfAbsent(code, key -> new SlRgbState(red, green, blue, 1));
    }
    
    private SlRgbState(final int red, final int green, final int blue, final int behavior) {
        this(red, green, blue, behavior, null);
    }
    
    private SlRgbState(final int red, final int green, final int blue, final SlRgbState otherColor) {
        this(red, green, blue, 2, otherColor);
    }
    
    private SlRgbState(final int red, final int green, final int blue, final int behavior,
        final SlRgbState otherColor) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.behavior = behavior;
        this.otherColor = otherColor;
        final int code = (red & 0x7F) | ((green & 0x7F) << 7) | ((blue & 0x7F) << 14) | (0x1 << 21);
    }
    
    public SlRgbState getBlink() {
        if (this.behavior == 2) {
            return this;
        }
        return get(this.red, this.green, this.blue, 2);
    }
    
    public SlRgbState getPulse() {
        if (this.behavior == 3) {
            return this;
        }
        return get(this.red, this.green, this.blue, 3);
    }
    
    public SlRgbState getBlink(final SlRgbState otherColor) {
        return new SlRgbState(this.red, this.green, this.blue, otherColor);
    }
    
    public int getRed() {
        return red;
    }
    
    public int getGreen() {
        return green;
    }
    
    public int getBlue() {
        return blue;
    }
    
    public int getBehavior() {
        return behavior;
    }
    
    public SlRgbState getOtherColor() {
        return otherColor;
    }
    
    public boolean isOff() {
        return this.red == 0 && this.blue == 0 && this.green == 0;
    }
    
    @Override
    public HardwareLightVisualState getVisualState() {
        return null;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof SlRgbState other) {
            return other.red == this.red && other.green == this.green && other.blue == this.blue
                && other.behavior == this.behavior && Objects.equals(otherColor, this.otherColor);
        }
        return false;
    }
    
    public SlRgbState reduced(final int value) {
        if (value == 127) {
            return this;
        }
        final double factor = (double) value / 127.0;
        
        final int red = (int) (Math.round(factor * this.red));
        final int green = (int) (Math.round(factor * this.green));
        final int blue = (int) (Math.round(factor * this.blue));
        return get(red, green, blue);
    }
    
    @Override
    public String toString() {
        return "COLOR %03d %03d %03d %03d".formatted(red, green, blue, behavior);
    }
    
    
}
