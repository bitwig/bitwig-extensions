package com.bitwig.extensions.controllers.neuzeitinstruments.controls;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extensions.controllers.neuzeitinstruments.DropMidiProcessor;
import com.bitwig.extensions.framework.values.MidiStatus;

public class MapButton {
    private final int ccNr;
    private final HardwareButton button;
    private final int midiStatusValue;
    
    public MapButton(final int index, final MidiStatus midiStatus, final int channel, final int noteNr,
        final String name, final HardwareSurface surface, final DropMidiProcessor midiProcessor) {
        this.ccNr = noteNr;
        this.midiStatusValue = midiStatus.getStatus(channel);
        button = surface.createHardwareButton("%s-%d".formatted(name, index + 1));
        midiProcessor.assignCcAction(button, channel, noteNr);
        
        final MidiIn midiIn = midiProcessor.getMidiIn();
        if (midiStatus == MidiStatus.NOTE_ON) {
            button.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(channel, noteNr));
            button.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(channel, noteNr));
        } else {
            button.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, noteNr, 0x7F));
            button.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, noteNr, 0x0));
        }
        
        final OnOffHardwareLight light = surface.createOnOffHardwareLight("%s-light-%d".formatted(name, index + 1));
        button.setBackgroundLight(light);
        button.isPressed().markInterested();
        
        light.onUpdateHardware(() -> {
            midiProcessor.sendMidi(midiStatusValue, noteNr, light.isOn().currentValue() ? 0x7F : 0);
        });
    }
    
}
