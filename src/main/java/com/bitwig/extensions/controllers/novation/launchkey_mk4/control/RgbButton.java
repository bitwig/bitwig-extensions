package com.bitwig.extensions.controllers.novation.launchkey_mk4.control;

import java.util.function.Supplier;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.MidiProcessor;
import com.bitwig.extensions.framework.Layer;

public class RgbButton extends LaunchkeyButton {
    protected MultiStateHardwareLight light;
    
    public RgbButton(final ButtonMidiType type, final int midiId, final String name, final HardwareSurface surface,
        final MidiProcessor midiProcessor) {
        super(type, midiId, name, surface, midiProcessor);
        light = surface.createMultiStateHardwareLight(name + "_LIGHT_" + midiId);
        light.state().setValue(RgbState.OFF);
        if (type == ButtonMidiType.PAD) {
            light.state().onUpdateHardware(this::updateState);
        } else if (type == ButtonMidiType.PAD_DRUM) {
            light.state().onUpdateHardware(this::updateStateDrum);
        } else {
            light.state().onUpdateHardware(this::updateStateCC);
        }
    }
    
    public int getMidiId() {
        return midiId;
    }
    
    private void updateState(final InternalHardwareLightState state) {
        if (state instanceof final RgbState rgbState) {
            switch (rgbState.getState()) {
                case NORMAL:
                    midiProcessor.sendMidi(0x90, midiId, rgbState.getColorIndex());
                    break;
                case FLASHING:
                    midiProcessor.sendMidi(0x90, midiId, rgbState.getColorIndex());
                    midiProcessor.sendMidi(0x91, midiId, rgbState.getAltColor());
                    break;
                case PULSING:
                    midiProcessor.sendMidi(0x92, midiId, rgbState.getColorIndex());
                    break;
            }
        } else {
            midiProcessor.sendMidi(0x90, midiId, 0);
        }
    }
    
    private void updateStateDrum(final InternalHardwareLightState state) {
        if (state instanceof final RgbState rgbState) {
            switch (rgbState.getState()) {
                case NORMAL:
                    midiProcessor.sendMidi(0x9A, midiId, rgbState.getColorIndex());
                    break;
                case FLASHING:
                    midiProcessor.sendMidi(0x9B, midiId, rgbState.getColorIndex());
                    break;
                case PULSING:
                    midiProcessor.sendMidi(0x9C, midiId, rgbState.getColorIndex());
                    break;
            }
        } else {
            midiProcessor.sendMidi(0x9A, midiId, 0);
        }
    }
    
    private void updateStateCC(final InternalHardwareLightState state) {
        if (state instanceof final RgbState rgbState) {
            switch (rgbState.getState()) {
                case NORMAL:
                    midiProcessor.sendMidi(0xB0, midiId, rgbState.getColorIndex());
                    break;
                case FLASHING:
                    midiProcessor.sendMidi(0xB1, midiId, rgbState.getColorIndex());
                    break;
                case PULSING:
                    midiProcessor.sendMidi(0xB2, midiId, rgbState.getColorIndex());
                    break;
            }
        } else {
            midiProcessor.sendMidi(0xB0, midiId, 0);
        }
    }
    
    public void bindDisabled(final Layer layer) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> {});
        layer.bind(hwButton, hwButton.releasedAction(), () -> {});
        layer.bindLightState(() -> RgbState.OFF, light);
    }
    
    public void bindLight(final Layer layer, final Supplier<InternalHardwareLightState> supplier) {
        layer.bindLightState(supplier, light);
    }
    
    
}
