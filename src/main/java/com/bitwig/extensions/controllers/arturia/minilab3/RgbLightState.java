package com.bitwig.extensions.controllers.arturia.minilab3;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class RgbLightState extends InternalHardwareLightState {
    private static final Map<Integer, RgbLightState> indexLookup = new HashMap<>();

    public static final RgbLightState RED = new RgbLightState(127, 0, 0);
    public static final RgbLightState BLUE = new RgbLightState(0, 0, 127);
    public static final RgbLightState WHITE = new RgbLightState(127, 127, 127);
    public static final RgbLightState OFF = new RgbLightState(0, 0, 0);
    public static final RgbLightState YELLOW = new RgbLightState(127, 127, 0);
    public static final RgbLightState ORANGE = new RgbLightState(127, 0x32, 0);
    public static final RgbLightState ORANGE_DIMMED = new RgbLightState(0x14, 0x05, 0);
    public static final RgbLightState GREEN = new RgbLightState(0, 127, 0);
    public static final RgbLightState GREEN_DIMMED = new RgbLightState(0, 0x14, 0);
    public static final RgbLightState RED_DIMMED = new RgbLightState(0x14, 0, 0);
    public static final RgbLightState WHITE_DIMMED = new RgbLightState(0x14, 0x14, 0x14);

    private final byte red;
    private final byte green;
    private final byte blue;
    private final HardwareLightVisualState visualState;
    private final RgbLightState brighter;
    private final RgbLightState darker;

    public static RgbLightState getColor(final double red, final double green, final double blue) {
        final int rv = (int) Math.floor(red * 255);
        final int gv = (int) Math.floor(green * 255);
        final int bv = (int) Math.floor(blue * 255);

        final int colorLookup = rv << 16 | gv << 8 | bv;
        return indexLookup.computeIfAbsent(colorLookup, index -> {
            final double[] sat = saturate(red, green, blue, 0.2);
            return new RgbLightState(sat[0], sat[1], sat[2], true);
        });
    }

    public RgbLightState(final double red, final double green, final double blue, final boolean variants) {
        this.red = convert(red, 1);
        this.green = convert(green, 1);
        this.blue = convert(blue, 1);
        brighter = new RgbLightState(red * 1.50, green * 1.50, blue * 1.50);
        darker = new RgbLightState(red * 0.35, green * 0.3, blue * 0.3);
        visualState = HardwareLightVisualState.createForColor(
                Color.fromRGB(this.red << 1, this.green << 1, this.blue << 1));
    }

    public RgbLightState(final double red, final double green, final double blue) {
        this.red = convert(red, 1.0);
        this.green = convert(green, 1.0);
        this.blue = convert(blue, 1.0);
        brighter = this;
        darker = this;
        visualState = HardwareLightVisualState.createForColor(
                Color.fromRGB(this.red << 1, this.green << 1, this.blue << 1));
    }

    public RgbLightState(final int red, final int green, final int blue, final boolean variants) {
        this.red = convert(red);
        this.green = convert(green);
        this.blue = convert(blue);
        brighter = new RgbLightState(Math.round(red * 1.10), green + 10, blue + 10);
        darker = new RgbLightState(red * 0.3, green * 0.3, blue * 0.3);
        visualState = HardwareLightVisualState.createForColor(Color.fromRGB(red << 1, green << 1, blue << 1));
    }

    public RgbLightState(final int red, final int green, final int blue) {
        this.red = convert(red);
        this.green = convert(green);
        this.blue = convert(blue);
        brighter = this;
        darker = this;
        visualState = HardwareLightVisualState.createForColor(Color.fromRGB(red << 1, green << 1, blue << 1));
    }

    public byte convert(final double colorValue, final double factor) {
        final int value = (int) Math.floor(colorValue * 100 * factor);
        if (value >= 0 && value <= 127) {
            return (byte) value;
        }
        if (value < 0) {
            return 0;
        }
        return 127;
    }

    public byte convert(final int value) {
        if (value >= 0 && value <= 127) {
            return (byte) value;
        }
        if (value < 0) {
            return 0;
        }
        return 127;
    }

    @Override
    public HardwareLightVisualState getVisualState() {
        return visualState;
    }

    public void apply(final byte[] rgbCommand) {
        rgbCommand[10] = red;
        rgbCommand[11] = green;
        rgbCommand[12] = blue;
    }

    public void apply(final int bankOffset, final byte[] rgbCommand) {
        rgbCommand[bankOffset * 3] = red;
        rgbCommand[bankOffset * 3 + 1] = green;
        rgbCommand[bankOffset * 3 + 2] = blue;
    }

    public RgbLightState getBrighter() {
        return brighter;
    }

    public RgbLightState getDarker() {
        return darker;
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
        final RgbLightState other = (RgbLightState) obj;
        return blue == other.blue && green == other.green && red == other.red;
    }

    @Override
    public String toString() {
        return "RgbLightState{" + "red=" + red + ", green=" + green + ", blue=" + blue + '}';
    }

    private static double[] saturate(final double red, final double green, final double blue, final double amount) {
        final double max = Math.max(Math.max(red, green), blue);
        final double min = Math.min(Math.min(red, green), blue);
        double rv = red;
        double bv = blue;
        double gv = green;
        if (red == max) {
            rv = Math.min(1.0, red + amount);
        } else if (red == min) {
            rv = Math.max(0.0, red - amount);
        }
        if (green == max) {
            gv = Math.min(1.0, green + amount);
        } else if (green == min) {
            gv = Math.max(0.0, green - amount);
        }
        if (blue == max) {
            bv = Math.min(1.0, blue + amount);
        } else if (blue == min) {
            bv = Math.max(0.0, blue - amount);
        }

        return new double[]{rv, gv, bv};
    }


    public byte getRed() {
        return red;
    }

    public byte getBlue() {
        return blue;
    }

    public byte getGreen() {
        return green;
    }
}
