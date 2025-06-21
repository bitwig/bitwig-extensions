package com.bitwig.extensions.controllers.arturia.keylab.mk3.color;

import java.util.Objects;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class SceneState extends InternalHardwareLightState {
    
    private final RgbColor color;
    private final boolean noClips;
    private final boolean held;
    
    public SceneState(final RgbColor color, final boolean noClips, final boolean held) {
        this.color = color;
        this.noClips = noClips;
        this.held = held;
    }
    
    @Override
    public HardwareLightVisualState getVisualState() {
        return null;
    }
    
    public RgbColor getColor() {
        return color;
    }
    
    public boolean isNoClips() {
        return noClips;
    }
    
    public boolean isHeld() {
        return held;
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SceneState that = (SceneState) o;
        return held == that.held && noClips == that.noClips && Objects.equals(color, that.color);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(color, noClips, held);
    }
}
