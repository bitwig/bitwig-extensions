package com.bitwig.extensions.controllers.allenheath.xonek3.color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class XoneRgbColor extends InternalHardwareLightState {
    public static final int FULL_BRIGHT = 0x30;
    public static final int HALF_BRIGHT = 0x10;
    public static final int DIM = 0x3;
    
    public static final XoneRgbColor OFF = new XoneRgbColor(0, 0, 0, 0);
    public static final XoneRgbColor RED = new XoneRgbColor(127, 0, 0, FULL_BRIGHT);
    public static final XoneRgbColor RED_OVER = new XoneRgbColor(127, 5, 0, FULL_BRIGHT);
    public static final XoneRgbColor GREEN = new XoneRgbColor(0, 127, 0, FULL_BRIGHT);
    public static final XoneRgbColor TEAL = new XoneRgbColor(0, 64, 64, FULL_BRIGHT);
    public static final XoneRgbColor LIME = new XoneRgbColor(85, 127, 0, FULL_BRIGHT);
    public static final XoneRgbColor YELLOW = new XoneRgbColor(127, 127, 0, FULL_BRIGHT);
    public static final XoneRgbColor PURPLE = new XoneRgbColor(24, 2, 120, FULL_BRIGHT);
    public static final XoneRgbColor MAGENTA = new XoneRgbColor(127, 1, 127, FULL_BRIGHT);
    public static final XoneRgbColor PINK = new XoneRgbColor(127, 40, 80, FULL_BRIGHT);
    public static final XoneRgbColor ORANGE = new XoneRgbColor(127, 20, 0, FULL_BRIGHT);
    public static final XoneRgbColor WHITE = new XoneRgbColor(127, 127, 127, HALF_BRIGHT);
    public static final XoneRgbColor GRAY = new XoneRgbColor(64, 64, 64, FULL_BRIGHT);
    public static final XoneRgbColor DARK_GRAY = new XoneRgbColor(12, 12, 12, FULL_BRIGHT);
    public static final XoneRgbColor CYAN = new XoneRgbColor(20, 127, 127, FULL_BRIGHT);
    public static final XoneRgbColor BLUE = new XoneRgbColor(0, 20, 127, FULL_BRIGHT);
    public static final XoneRgbColor AQUA = new XoneRgbColor(20, 110, 127, FULL_BRIGHT);
    public static final XoneRgbColor ORANGE_REMOTES = new XoneRgbColor(127, 40, 2, FULL_BRIGHT);
    
    public static final XoneRgbColor RED_DIM = RED.asBright(DIM);
    public static final XoneRgbColor GREEN_DIM = GREEN.asBright(DIM);
    public static final XoneRgbColor YELLOW_DIM = YELLOW.asBright(DIM);
    public static final XoneRgbColor ORANGE_DIM = ORANGE.asBright(DIM);
    public static final XoneRgbColor BLUE_DIM = BLUE.asBright(DIM);
    public static final XoneRgbColor MAGENTA_DIM = MAGENTA.asBright(DIM);
    public static final XoneRgbColor WHITE_LO = WHITE.asBright(1);
    public static final XoneRgbColor WHITE_DIM = WHITE.asBright(DIM);
    public static final XoneRgbColor WHITE_HALF = WHITE.asBright(0x20);
    
    public static final XoneRgbColor RED_OVER_DIM = RED_OVER.asBright(DIM);
    
    private static final Map<Integer, XoneRgbColor> COLOR_MAP = new HashMap<>();
    
    private final int red;
    private final int green;
    private final int blue;
    private final int brightness;
    
    public static class BrightnessScale {
        
        private final List<XoneRgbColor> colorScales = new ArrayList<>();
        
        public BrightnessScale(final XoneRgbColor baseColor, final int... brightnesses) {
            for (int i = 0; i < brightnesses.length; i++) {
                colorScales.add(baseColor.asBright(brightnesses[i]));
            }
        }
        
        public XoneRgbColor getColor(final int index) {
            if (index < colorScales.size()) {
                return colorScales.get(index);
            }
            return colorScales.get(colorScales.size() - 1);
        }
    }
    
    private static float inverseLogLike(float x, final float alpha) {
        x = Math.max(0f, Math.min(1f, x));
        if (alpha == 0f) {
            return x;
        }
        final double e = Math.exp(alpha);
        return (float) ((Math.exp(alpha * x) - 1.0) / (e - 1.0));
    }
    
    public static XoneRgbColor getPaletteColor(final int paletteIndex) {
        return switch (paletteIndex) {
            case 0 -> OFF;
            case 1 -> RED;
            case 2 -> ORANGE;
            case 3 -> YELLOW;
            case 4 -> LIME;
            case 5 -> GREEN;
            case 6 -> TEAL;
            case 7 -> CYAN;
            case 8 -> AQUA;
            case 9 -> BLUE;
            case 10 -> PURPLE;
            case 11 -> MAGENTA;
            case 12 -> PINK;
            case 13 -> DARK_GRAY;
            case 14 -> GRAY;
            case 15 -> WHITE;
            default -> RED;
        };
    }
    
    private static int flatten(final int color) {
        return color < 4 ? 0 : color;
    }
    
    public static XoneRgbColor of(final float r, final float g, final float b) {
        return of(r, g, b, FULL_BRIGHT);
    }
    
    public static XoneRgbColor of(final float r, final float g, final float b, final int brightness,
        final XoneRgbColor zeroColor) {
        final int red = Math.round(r * 127);
        final int green = Math.round(g * 127);
        final int blue = Math.round(b * 127);
        if (red == 36 && blue == 36 && green == 36) {
            return zeroColor;
        }
        final int code = red | (green << 7) | (blue << 14) | (brightness << 21);
        return COLOR_MAP.computeIfAbsent(code, key -> createCorrected(r, g, b, brightness));
    }
    
    public BrightnessScale scaleOf(final int... brightness) {
        return new BrightnessScale(this, brightness);
    }
    
    public static XoneRgbColor of(final float r, final float g, final float b, final int brightness) {
        final int red = Math.round(r * 127);
        final int green = Math.round(g * 127);
        final int blue = Math.round(b * 127);
        final int code = red | (green << 7) | (blue << 14) | (brightness << 21);
        return COLOR_MAP.computeIfAbsent(code, key -> createCorrected(r, g, b, brightness));
    }
    
    public static XoneRgbColor createCorrected(final float r, final float g, final float b, final int brightness) {
        final float alpha = 3.5f; // >0 compresses low end; try 1.0 - 4.0
        final int red = flatten(Math.round(inverseLogLike(r, alpha) * 127f));
        final int green = flatten(Math.round(inverseLogLike(g, alpha) * 127f));
        final int blue = flatten(Math.round(inverseLogLike(b, alpha) * 127f));
        return new XoneRgbColor(red, green, blue, brightness);
    }
    
    
    public XoneRgbColor asBright(final int brightness) {
        return new XoneRgbColor(red, green, blue, brightness);
    }
    
    public XoneRgbColor bright(final int brightness) {
        final int code = getBaseCode() | (brightness << 21);
        return COLOR_MAP.computeIfAbsent(code, key -> new XoneRgbColor(red, green, blue, brightness));
    }
    
    private int getBaseCode() {
        return red | (green << 7) | (blue << 14);
    }
    
    private XoneRgbColor(final int red, final int green, final int blue, final int brightness) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.brightness = brightness;
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
    
    public int getBrightness() {
        return brightness;
    }
    
    @Override
    public HardwareLightVisualState getVisualState() {
        return HardwareLightVisualState.createForColor(Color.fromRGB255(red * 2, green * 2, blue * 2));
    }
    
    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        
        final XoneRgbColor that = (XoneRgbColor) o;
        return red == that.red && green == that.green && blue == that.blue && brightness == that.brightness;
    }
    
    @Override
    public int hashCode() {
        int result = red;
        result = 31 * result + green;
        result = 31 * result + blue;
        result = 31 * result + brightness;
        return result;
    }
    
    public static XoneRgbColor forColor(final Color color) {
        if (color == null || color.getAlpha() == 0) {
            return OFF;
        }
        return new XoneRgbColor(color.getRed255() / 2, color.getGreen255() / 2, color.getBlue255() / 2, FULL_BRIGHT);
    }
    
}
