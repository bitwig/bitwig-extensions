package com.bitwig.extensions.controllers.neuzeitinstruments.controls;

import java.util.function.Supplier;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.controllers.akai.apc.common.led.RgbLightState;
import com.bitwig.extensions.controllers.neuzeitinstruments.DropColor;
import com.bitwig.extensions.controllers.neuzeitinstruments.DropMidiProcessor;
import com.bitwig.extensions.framework.Layer;

public class DropColorButton extends DropButton {
    
    private final MultiStateHardwareLight light;
    private int lastColorIndex = -1;
    
    public DropColorButton(final int index, final int midiNote, final String name, final HardwareSurface surface,
        final DropMidiProcessor midiProcessor) {
        super(index, midiNote, name, surface, midiProcessor);
        light = surface.createMultiStateHardwareLight(name + "_LIGHT_" + midiNote);
        light.state().setValue(RgbLightState.OFF);
        hwButton.setBackgroundLight(light);
        hwButton.isPressed().markInterested();
        light.state().onUpdateHardware(this::handleState);
    }
    
    private void handleState(final InternalHardwareLightState state) {
        if (state instanceof final DropColor color) {
            midiProcessor.sendMidi(NOTE_STATUS, midiNote, color.getColorIndex());
            this.lastColorIndex = color.getColorIndex();
        } else {
            midiProcessor.sendMidi(NOTE_STATUS, midiNote, 0);
        }
    }
    
    public void forceLightUpdate() {
        if (lastColorIndex != -1) {
            midiProcessor.sendMidi(NOTE_STATUS, midiNote, lastColorIndex);
        }
    }
    
    public void bindLight(final Layer layer, final Supplier<InternalHardwareLightState> supplier) {
        layer.bindLightState(supplier, light);
    }
}
