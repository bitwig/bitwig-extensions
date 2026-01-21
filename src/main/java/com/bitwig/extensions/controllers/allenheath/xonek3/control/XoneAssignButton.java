package com.bitwig.extensions.controllers.allenheath.xonek3.control;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.controllers.allenheath.xonek3.XoneMidiDevice;
import com.bitwig.extensions.controllers.allenheath.xonek3.color.XoneIndexColor;
import com.bitwig.extensions.framework.values.MidiStatus;

public class XoneAssignButton {
    
    private final HardwareButton hwButton;
    private final MultiStateHardwareLight light;
    private final XoneMidiDevice midiProcessor;
    private final int midiNr;
    private final int midiStatus;
    private final int layerIndex;
    private final int ledIndex;
    
    
    public XoneAssignButton(final XoneMidiDevice midiProcessor, final HardwareSurface surface, final int layer,
        final int ledIndex, final int midiNr) {
        final String name = "GRID%d LAYER %d - %d".formatted(midiProcessor.getDeviceIndex(), layer + 2, ledIndex + 1);
        hwButton = surface.createHardwareButton(name);
        this.midiProcessor = midiProcessor;
        this.midiNr = midiNr;
        this.layerIndex = layer;
        this.ledIndex = ledIndex;
        final MidiIn midiIn = midiProcessor.getMidiIn();
        this.midiStatus = MidiStatus.NOTE_ON.getValue() | 0xE;
        hwButton.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(0xE, midiNr));
        hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0xE, midiNr));
        light = surface.createMultiStateHardwareLight("%s LIGHT".formatted(name));
        light.state().setValue(XoneIndexColor.BLACK);
        light.setColorToStateFunction(XoneIndexColor::forColor);
        hwButton.setBackgroundLight(light);
        light.state().onUpdateHardware(this::handleState);
    }
    
    public int getLayerIndex() {
        return layerIndex;
    }
    
    public int getLedIndex() {
        return ledIndex;
    }
    
    public int getMidiNr() {
        return midiNr;
    }
    
    private void handleState(final InternalHardwareLightState state) {
        if (state instanceof final XoneIndexColor color) {
            midiProcessor.updateAssignLed(this.midiStatus, midiNr, layerIndex, ledIndex, color != XoneIndexColor.BLACK);
        } else {
            midiProcessor.sendMidi(midiStatus, midiNr, 0);
        }
    }
    
}
