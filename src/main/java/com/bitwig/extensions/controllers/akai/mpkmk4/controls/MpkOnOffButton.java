package com.bitwig.extensions.controllers.akai.mpkmk4.controls;

import java.util.function.BooleanSupplier;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.Midi;

public class MpkOnOffButton extends MpkButton {
    private final OnOffHardwareLight light;
    public static final int STD_REPEAT_DELAY = 400;
    public static final int STD_REPEAT_FREQUENCY = 50;
    
    
    public MpkOnOffButton(final int channel, final int midiId, final String name, final HardwareSurface surface,
        final MpkMidiProcessor midiProcessor) {
        super(channel, midiId, true, name, surface, midiProcessor);
        light = surface.createOnOffHardwareLight(name + "_LIGHT");
        light.onUpdateHardware(
            () -> midiProcessor.sendMidi(Midi.NOTE_ON | channel, midiId, light.isOn().currentValue() ? 0x7F : 0x0));
        hwButton.setBackgroundLight(light);
    }
    
    @Override
    public void forceUpdate() {
        midiProcessor.sendMidi(Midi.NOTE_ON | channel, midiId, light.isOn().currentValue() ? 0x7F : 0x0);
    }
    
    public void bindLight(final Layer layer, final BooleanSupplier state) {
        layer.bind(state, light);
    }
    
    public void bindLightPressed(final Layer layer) {
        hwButton.isPressed().markInterested();
        layer.bind(hwButton.isPressed(), light);
    }
    
    
}
