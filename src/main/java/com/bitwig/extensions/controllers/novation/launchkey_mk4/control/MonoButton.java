package com.bitwig.extensions.controllers.novation.launchkey_mk4.control;

import java.util.function.Supplier;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.MidiProcessor;
import com.bitwig.extensions.framework.Layer;

public class MonoButton extends LaunchkeyButton {
    protected MultiStateHardwareLight light;
    
    public MonoButton(final int midiId, final String name, final HardwareSurface surface,
        final MidiProcessor midiProcessor) {
        super(ButtonMidiType.CC, midiId, name, surface, midiProcessor);
        light = surface.createMultiStateHardwareLight(name + "_LIGHT_" + midiId);
        light.state().setValue(RgbState.OFF);
        light.state().onUpdateHardware(this::updateStateCC);
    }
    
    private void updateStateCC(final InternalHardwareLightState state) {
        if (state instanceof final RgbState rgbState) {
            midiProcessor.sendMidi(0xB3, midiId, rgbState.getColorIndex());
        } else {
            midiProcessor.sendMidi(0xB0, midiId, 0);
        }
    }
    
    public void bindDisabled(final Layer layer) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> {});
        layer.bind(hwButton, hwButton.releasedAction(), () -> {});
        layer.bindLightState(() -> RgbState.OFF, light);
    }
    
    public void bindPressedLight(final Layer layer, final BooleanValue value) {
        value.markInterested();
        layer.bindLightState(() -> {
            if (hwButton.isPressed().get()) {
                return RgbState.MONO_ON;
            } else if (value.get()) {
                return RgbState.MONO_MID;
            }
            return RgbState.OFF;
        }, light);
    }
    
    public void bindLight(final Layer layer, final Supplier<InternalHardwareLightState> supplier) {
        layer.bindLightState(supplier, light);
    }
    
    public void bindLight(final Layer layer, final BooleanValue value) {
        value.markInterested();
        layer.bindLightState(() -> value.get() ? RgbState.MONO_ON : RgbState.OFF, light);
    }
    
}
