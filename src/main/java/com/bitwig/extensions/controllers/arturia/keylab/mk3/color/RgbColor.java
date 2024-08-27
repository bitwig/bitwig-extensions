package com.bitwig.extensions.controllers.arturia.keylab.mk3.color;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RgbColor {
    private static final Map<Integer, RgbColor> indexLookup = new HashMap<>();
    private RgbLightState brightColor;
    private RgbLightState dimColor;
    private final Map<BlinkState, RgbLightState> states = new HashMap<>();
    private final RgbLightState basicColor;
    private final int redValue;
    private final int greenValue;
    private final int blueValue;
    public static final RgbColor OFF = getColor(0, 0, 0);
    public static final RgbColor RED = getColor(127, 0, 0);
    public static final RgbColor GREEN = getColor(0, 127, 0);
    public static final RgbColor GREEN_DIM = getColor(0, 50, 0);
    public static final RgbColor BLUE = getColor(20, 10, 127);
    public static final RgbColor WHITE = getColor(127, 127, 127);
    public static final RgbColor GRAY = getColor(32, 32, 32);
    public static final RgbColor BLACK = getColor(0, 0, 0);
    public static final RgbColor ORANGE = getColor(127, 64, 0);
    public static final RgbColor YELLOW = getColor(127, 127, 0);
    public static final RgbColor WIDGET = getColor(0, 0x6C, 0x7a);
    public static final RgbColor DEFAULT_SCENE = getColor(39, 39, 39);
    
    public static RgbColor getColor(final double red, final double green, final double blue) {
        final int rv = (int) Math.floor(red * 127);
        final int gv = (int) Math.floor(green * 127);
        final int bv = (int) Math.floor(blue * 127);
        final int colorLookup = rv << 16 | gv << 8 | bv;
        return indexLookup.computeIfAbsent(colorLookup, index -> new RgbColor(rv, gv, bv));
    }
    
    public static RgbColor getColor(final int red, final int green, final int blue) {
        final int colorLookup = red << 16 | green << 8 | blue;
        return indexLookup.computeIfAbsent(colorLookup, index -> new RgbColor(red, green, blue));
    }
    
    public RgbColor(final int red, final int green, final int blue) {
        redValue = red;
        greenValue = green;
        blueValue = blue;
        basicColor = new RgbLightState(red, green, blue, BlinkState.NONE);
    }
    
    public RgbLightState getColorState() {
        return basicColor;
    }
    
    public RgbLightState getColorState(final BlinkState state) {
        if (state == BlinkState.NONE) {
            return basicColor;
        }
        return states.computeIfAbsent(state, aState -> new RgbLightState(redValue, greenValue, blueValue, state));
    }
    
    public int getGreenValue() {
        return greenValue;
    }
    
    public int getBlueValue() {
        return blueValue;
    }
    
    public int getRedValue() {
        return redValue;
    }
    
    public RgbColor getDimmed() {
        final int red = (int) (redValue * 0.3);
        final int green = (int) (greenValue * 0.3);
        final int blue = (int) (blueValue * 0.3);
        return getColor(red, green, blue);
    }
    
    RgbLightState getDimColor() {
        return basicColor;
    }
    
    RgbLightState getBrightColor() {
        return basicColor;
    }
    
    @Override
    public String toString() {
        return "%d %d %d".formatted(redValue, greenValue, blueValue);
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final RgbColor rgbColor = (RgbColor) o;
        return redValue == rgbColor.redValue && greenValue == rgbColor.greenValue && blueValue == rgbColor.blueValue;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(redValue, greenValue, blueValue);
    }
}
