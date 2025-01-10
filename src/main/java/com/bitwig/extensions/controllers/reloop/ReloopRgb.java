package com.bitwig.extensions.controllers.reloop;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class ReloopRgb extends InternalHardwareLightState {
    
    private static final ReloopRgb[] lookup = new ReloopRgb[128];
    
    public static final ReloopRgb OFF = ReloopRgb.of(0);
    public static final ReloopRgb BRIGHT_RED = ReloopRgb.of(0b1110000);
    public static final ReloopRgb DIMMED_RED = ReloopRgb.of(0b0110000);
    public static final ReloopRgb BRIGHT_ORANGE = ReloopRgb.of(0b1110100);
    public static final ReloopRgb DIMMED_ORANGE = ReloopRgb.of(0b0110100);
    public static final ReloopRgb BRIGHT_YELLOW = ReloopRgb.of(0b1111100);
    public static final ReloopRgb DIMMED_YELLOW = ReloopRgb.of(0b0111100);
    public static final ReloopRgb BRIGHT_GREEN = ReloopRgb.of(0b1001100);
    public static final ReloopRgb DIMMED_GREEN = ReloopRgb.of(0b0001100);
    public static final ReloopRgb BRIGHT_BLUE = ReloopRgb.of(0b1001111);
    public static final ReloopRgb DIMMED_BLUE = ReloopRgb.of(0b0001111);
    public static final ReloopRgb WHITE_DIM = ReloopRgb.of(0b0111111);
    public static final ReloopRgb WHITE_BRIGHT = ReloopRgb.of(0b1111111);
    
    
    private final int colorValue;
    private final Color color;
    
    public static ReloopRgb of(final int value) {
        if (value >= 0 && value < lookup.length) {
            if (lookup[value] == null) {
                lookup[value] = new ReloopRgb(value);
            }
            return lookup[value];
        }
        return OFF;
    }
    
    public static ReloopRgb ofBright(final int value) {
        return of(0x40 | value);
    }
    
    public ReloopRgb(final int colorValue) {
        this.colorValue = colorValue;
        this.color = Color.fromRGB255((colorValue >> 4) << 5, ((colorValue >> 2) & 0x3) << 5, (colorValue & 0x3) << 5);
    }
    
    public int getColorValue() {
        return colorValue;
    }
    
    @Override
    public HardwareLightVisualState getVisualState() {
        return HardwareLightVisualState.createForColor(color);
    }
    
    @Override
    public boolean equals(final Object o) {
        if (o instanceof ReloopRgb rgb) {
            return rgb.colorValue == colorValue;
        }
        return false;
    }
    
    public static int toColorValue(final double r, final double g, final double b) {
        return toColorValue(r) << 4 | toColorValue(g) << 2 | toBlueValue(b, g);
    }
    
    public static ReloopRgb toColor(final double r, final double g, final double b) {
        return ReloopRgb.of(toColorValue(r) << 4 | toColorValue(g) << 2 | toBlueValue(b, g));
    }
    
    public static int toColorValue(final double v) {
        if (v < 0.30) {
            return 0;
        }
        if (v < 0.55) {
            return 1;
        }
        if (v < 0.72) {
            return 2;
        }
        return 3;
    }
    
    public static int toBlueValue(final double blue, final double green) {
        if (blue < 0.33) {
            return 0;
        }
        if (blue < 0.55) {
            return green > 0.7 && blue < 0.4 ? 0 : 1;
        }
        if (blue < 0.75) {
            return 2;
        }
        return 3;
    }
    
}
