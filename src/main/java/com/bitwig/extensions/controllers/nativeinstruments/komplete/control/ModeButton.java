package com.bitwig.extensions.controllers.nativeinstruments.komplete.control;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.CcAssignment;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.MidiProcessor;

public class ModeButton {
    private final HardwareButton hwButton;
    private final OnOffHardwareLight led;
    
    public ModeButton(final MidiProcessor midiProcessor, final String name, final CcAssignment assignment) {
        final HardwareSurface surface = midiProcessor.getSurface();
        hwButton = surface.createHardwareButton(name);
        final MidiIn midiIn = midiProcessor.getMidiIn();
        hwButton.pressedAction().setActionMatcher(assignment.createActionMatcher(midiIn, 1));
        hwButton.releasedAction().setActionMatcher(assignment.createActionMatcher(midiIn, 0));
        led = surface.createOnOffHardwareLight(name + "_LED");
        hwButton.setBackgroundLight(led);
        led.onUpdateHardware(() -> midiProcessor.sendLedUpdate(assignment, led.isOn().currentValue() ? 1 : 0));
        hwButton.isPressed().markInterested();
    }
    
    public ModeButton bindLightToPressed() {
        hwButton.isPressed().addValueObserver(v -> led.isOn().setValue(v));
        return this;
    }
    
    public HardwareButton getHwButton() {
        return hwButton;
    }
    
    public OnOffHardwareLight getLed() {
        return led;
    }
    
}
