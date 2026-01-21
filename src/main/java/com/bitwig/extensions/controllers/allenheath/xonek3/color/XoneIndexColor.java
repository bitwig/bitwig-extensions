package com.bitwig.extensions.controllers.allenheath.xonek3.color;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class XoneIndexColor extends InternalHardwareLightState {


    public static final XoneIndexColor BLACK = new XoneIndexColor(0);
    public static final XoneIndexColor RED = new XoneIndexColor(0x1);
    public static final XoneIndexColor ORANGE = new XoneIndexColor(0x2);
    public static final XoneIndexColor YELLOW = new XoneIndexColor(0x3);
    public static final XoneIndexColor LIME = new XoneIndexColor(0x4);
    public static final XoneIndexColor GREEN = new XoneIndexColor(0x5);
    public static final XoneIndexColor TEAL = new XoneIndexColor(0x6);
    public static final XoneIndexColor CYAN = new XoneIndexColor(0x7);
    public static final XoneIndexColor AQUA = new XoneIndexColor(0x8);
    public static final XoneIndexColor Blue = new XoneIndexColor(0x9);
    public static final XoneIndexColor VIOLET = new XoneIndexColor(0xA);
    public static final XoneIndexColor MAGENTA = new XoneIndexColor(0xB);
    public static final XoneIndexColor LAVENDER = new XoneIndexColor(0xC);
    public static final XoneIndexColor DARK_GREY = new XoneIndexColor(0xD);
    public static final XoneIndexColor LIGHT_GREY = new XoneIndexColor(0xE);
    public static final XoneIndexColor WHITE = new XoneIndexColor(0xF);
    public static final XoneIndexColor BACKLIGHT = new XoneIndexColor(0x10);

    private final int colorIndex;
    private final boolean on;
    private final XoneIndexColor offState;

    private XoneIndexColor(final int colorIndex, final boolean isOn, final XoneIndexColor onState) {
        this.colorIndex = colorIndex;
        this.on = isOn;
        this.offState = onState;
    }

    public XoneIndexColor(final int colorIndex) {
        this(colorIndex, true, new XoneIndexColor(colorIndex, false, null));
    }

    public int getColorIndex() {
        return colorIndex;
    }

    public boolean isOn() {
        return on;
    }

    public int stateValue() {
        return on ? 0x7F : 0;
    }

    public XoneIndexColor getOffState() {
        return offState == null ? this : offState;
    }

    @Override
    public HardwareLightVisualState getVisualState() {
        return null;
    }

    public static XoneIndexColor forColor(final Color color) {
        if (color == null || color.getAlpha() == 0) {
            return XoneIndexColor.BLACK;
        }
        if (color.getRed255() == 0 && color.getGreen255() == 0 && color.getBlue255() == 0) {
            return XoneIndexColor.BLACK;
        }
        return XoneIndexColor.RED;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final XoneIndexColor that = (XoneIndexColor) o;
        return colorIndex == that.colorIndex && on == that.on;
    }

    @Override
    public int hashCode() {
        int result = colorIndex;
        result = 31 * result + Boolean.hashCode(on);
        return result;
    }

}
