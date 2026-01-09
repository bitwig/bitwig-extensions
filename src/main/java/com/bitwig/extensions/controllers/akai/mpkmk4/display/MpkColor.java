package com.bitwig.extensions.controllers.akai.mpkmk4.display;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class MpkColor extends InternalHardwareLightState {
    private static final Map<Integer, MpkColor> indexLookup = new HashMap<>();
    
    public static final MpkColor OFF = new MpkColor(0);
    public static final MpkColor WHITE = new MpkColor(3);
    public static final MpkColor GRAY = new MpkColor(2);
    public static final MpkColor YELLOW = new MpkColor(13);
    public static final MpkColor RED = new MpkColor(5);
    public static final MpkColor ORANGE = new MpkColor(9);
    public static final MpkColor BLUE = new MpkColor(67);
    public static final MpkColor GREEN = new MpkColor(21);
    
    private final int colorIndex;
    private final int state;
    private final MpkColor[] stateVariants = new MpkColor[16];
    
    private MpkColor(final int colorIndex, final int state) {
        this.colorIndex = colorIndex;
        this.state = state;
    }
    
    private MpkColor(final int colorIndex) {
        this(colorIndex, MpkMonoState.SOLID_STATE);
        this.stateVariants[MpkMonoState.SOLID_STATE] = this;
    }
    
    public static MpkColor getColor(final float red, final float green, final float blue) {
        final int rv = (int) Math.floor(red * 255);
        final int gv = (int) Math.floor(green * 255);
        final int bv = (int) Math.floor(blue * 255);
        final int colorLookup = rv << 16 | gv << 8 | bv;
        
        
        final MpkColor idx = indexLookup.computeIfAbsent(
            colorLookup, index -> {
                final int ci = MpkColorLookup.rgbToIndex(red, green, blue);
                return new MpkColor(ci, MpkMonoState.SOLID_STATE);
            });
        //MpkMk4ControllerExtension.println(" > %d %d %d = %d", rv, gv, bv, idx.colorIndex);
        return idx;
    }
    
    public MpkColor variant(final int state) {
        if (this.stateVariants[state] == null) {
            this.stateVariants[state] = new MpkColor(this.colorIndex, state);
        }
        return this.stateVariants[state];
    }
    
    public int getState() {
        return state;
    }
    
    public int getColorIndex() {
        return colorIndex;
    }
    
    @Override
    public HardwareLightVisualState getVisualState() {
        return null;
    }
    
    @Override
    public final boolean equals(final Object o) {
        if (!(o instanceof final MpkColor mpkColor)) {
            return false;
        }
        return colorIndex == mpkColor.colorIndex && state == mpkColor.state;
    }
    
    @Override
    public int hashCode() {
        int result = colorIndex;
        result = 31 * result + state;
        return result;
    }
}
