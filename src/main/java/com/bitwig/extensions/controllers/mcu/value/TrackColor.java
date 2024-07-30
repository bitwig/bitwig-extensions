package com.bitwig.extensions.controllers.mcu.value;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class TrackColor extends InternalHardwareLightState {

    private final int[] colors = new int[8];

    private TrackColor lastFetch = null;

    public TrackColor() {
    }

    private TrackColor(int[] colors) {
        System.arraycopy(colors, 0, this.colors, 0, 8);
    }

    @Override
    public HardwareLightVisualState getVisualState() {
        return null;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof TrackColor color) {
            return compares(color);
        }
        return false;
    }

    private boolean compares(final TrackColor other) {
        for (int i = 0; i < colors.length; i++) {
            if (other.colors[i] != colors[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean compares(int[] currentState) {
        if (currentState.length != colors.length) {
            return false;
        }
        for (int i = 0; i < colors.length; i++) {
            if (currentState[i] != colors[i]) {
                return false;
            }
        }
        return true;
    }

    public int[] getColors() {
        if (lastFetch != null) {
            return lastFetch.colors;
        }
        return this.colors;
    }

    public TrackColor getState(int[] currentState) {
        if (lastFetch != null && lastFetch.compares(currentState)) {
            return lastFetch;
        }
        lastFetch = new TrackColor(currentState);
        return lastFetch;
    }

}
