package com.bitwig.extensions.controllers.mcu.value;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class TrackColor extends InternalHardwareLightState {
    
    private final int[] colors;
    
    public TrackColor() {
        this.colors = new int[8];
    }
    
    private TrackColor(final int[] colors) {
        this.colors = colors;
    }
    
    @Override
    public HardwareLightVisualState getVisualState() {
        return null;
    }
    
    @Override
    public boolean equals(final Object obj) {
        return obj == this;
    }
    
    public int[] getColors() {
        return this.colors;
    }
    
    public TrackColor setColor(final int index, final int colorValue) {
        final int[] colors = new int[8];
        System.arraycopy(this.colors, 0, colors, 0, 8);
        colors[index] = colorValue;
        return new TrackColor(colors);
    }
}
