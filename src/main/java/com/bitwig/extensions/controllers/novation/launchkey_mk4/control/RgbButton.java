package com.bitwig.extensions.controllers.novation.launchkey_mk4.control;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.MidiProcessor;
import com.bitwig.extensions.framework.Layer;

public class RgbButton extends LaunchkeyButton {
    protected MultiStateHardwareLight light;
    private final int lightMidiStatus;
    
    public RgbButton(final ButtonMidiType type, final int midiId, final String name, final HardwareSurface surface,
        final MidiProcessor midiProcessor) {
        super(type, type == ButtonMidiType.PAD_DRUM ? 0x9 : 0x0, midiId, name, surface, midiProcessor);
        lightMidiStatus = type == ButtonMidiType.PAD_DRUM ? 0x99 : 0x90;
        light = surface.createMultiStateHardwareLight(name + "_LIGHT_" + midiId);
        light.state().setValue(RgbState.OFF);
        if (type == ButtonMidiType.PAD || type == ButtonMidiType.PAD_DRUM) {
            light.state().onUpdateHardware(this::updateState);
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
                    midiProcessor.sendMidi(lightMidiStatus, midiId, rgbState.getColorIndex());
                    break;
                case FLASHING:
                    midiProcessor.sendMidi(lightMidiStatus, midiId, rgbState.getColorIndex());
                    midiProcessor.sendMidi(lightMidiStatus + 1, midiId, rgbState.getAltColor());
                    break;
                case PULSING:
                    midiProcessor.sendMidi(lightMidiStatus + 2, midiId, rgbState.getColorIndex());
                    break;
            }
        } else {
            midiProcessor.sendMidi(lightMidiStatus, midiId, 0);
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
    
    public void bindLightPressed(final Layer layer, final BooleanValue value) {
        hwButton.isPressed().markInterested();
        value.markInterested();
        layer.bindLightState(() -> {
            if (value.get()) {
                return hwButton.isPressed().get() ? RgbState.MONO_ON : RgbState.MONO_MID;
            }
            return RgbState.OFF;
        }, light);
    }
    
    public void bindLightOnOff(final Layer layer, final BooleanValue value) {
        hwButton.isPressed().markInterested();
        value.markInterested();
        layer.bindLightState(() -> value.get() ? RgbState.MONO_ON : RgbState.OFF, light);
    }
    
    public void bindLightPressed(final Layer layer, final BooleanSupplier value) {
        hwButton.isPressed().markInterested();
        layer.bindLightState(() -> {
            if (value.getAsBoolean()) {
                return hwButton.isPressed().get() ? RgbState.MONO_ON : RgbState.MONO_MID;
            }
            return RgbState.OFF;
        }, light);
    }
}
