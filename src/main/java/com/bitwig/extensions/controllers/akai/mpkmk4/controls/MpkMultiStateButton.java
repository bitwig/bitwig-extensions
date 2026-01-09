package com.bitwig.extensions.controllers.akai.mpkmk4.controls;

import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkColor;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkMonoState;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.Midi;

public class MpkMultiStateButton extends MpkButton {
    
    protected MultiStateHardwareLight light;
    
    public MpkMultiStateButton(final int channel, final int midiId, final boolean isCc, final String name,
        final HardwareSurface surface, final MpkMidiProcessor midiProcessor) {
        super(channel, midiId, isCc, name, surface, midiProcessor);
        light = surface.createMultiStateHardwareLight(name + "_LIGHT");
        light.state().onUpdateHardware(this::updateState);
        light.state().setValue(MpkColor.OFF);
        hwButton.setBackgroundLight(light);
    }
    
    private void updateState(final InternalHardwareLightState internalHardwareLightState) {
        if (internalHardwareLightState instanceof final MpkColor state) {
            midiProcessor.sendMidi(Midi.NOTE_ON | state.getState(), midiId, state.getColorIndex());
        } else if (internalHardwareLightState instanceof final MpkMonoState state) {
            midiProcessor.sendMidi(Midi.NOTE_ON | state.getState(), midiId, state.isOn() ? 0x7F : 0x00);
        } else {
            midiProcessor.sendMidi(Midi.NOTE_ON, midiId, 0);
        }
    }
    
    @Override
    public void forceUpdate() {
        updateState(light.state().currentValue());
    }
    
    public void bindLightPressedOnOff(final Layer layer) {
        layer.bindLightState(() -> hwButton.isPressed().get() ? MpkMonoState.FULL_ON : MpkMonoState.OFF, light);
    }
    
    public void bindLightPressedOnDimmed(final Layer layer) {
        layer.bindLightState(() -> hwButton.isPressed().get() ? MpkMonoState.FULL_ON : MpkMonoState.DIMMED, light);
    }
    
    public void bindLight(final Layer layer, final Supplier<InternalHardwareLightState> supplier) {
        layer.bindLightState(supplier, light);
    }
    
    public void bindLightDimmed(final Layer layer, final BooleanSupplier state) {
        layer.bindLightState(() -> state.getAsBoolean() ? MpkMonoState.FULL_ON : MpkMonoState.DIMMED, light);
    }
    
    public void bindLightDimmed(final Layer layer, final BooleanValue value) {
        value.markInterested();
        layer.bindLightState(() -> value.get() ? MpkMonoState.FULL_ON : MpkMonoState.DIMMED, light);
    }
    
    public void bindLightOff(final Layer layer) {
        layer.bindLightState(() -> MpkMonoState.OFF, light);
    }
    
    public void bindLightOnOff(final Layer layer, final BooleanSupplier state) {
        layer.bindLightState(() -> state.getAsBoolean() ? MpkMonoState.FULL_ON : MpkMonoState.OFF, light);
    }
    
    public void bindLightOnOff(final Layer layer, final BooleanValue value) {
        value.markInterested();
        layer.bindLightState(() -> value.get() ? MpkMonoState.FULL_ON : MpkMonoState.OFF, light);
    }
    
    public void bindLightPressed(final Layer layer, final Function<Boolean, InternalHardwareLightState> supplier) {
        layer.bindLightState(() -> supplier.apply(hwButton.isPressed().get()), light);
    }
    
    public void bindLight(final Layer layer, final Function<Boolean, InternalHardwareLightState> pressedCombine) {
        layer.bindLightState(() -> pressedCombine.apply(hwButton.isPressed().get()), light);
    }
    
    public void bindLightPressed(final Layer layer, final InternalHardwareLightState state,
        final InternalHardwareLightState holdState) {
        layer.bindLightState(() -> hwButton.isPressed().get() ? holdState : state, light);
    }
    
}
