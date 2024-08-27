package com.bitwig.extensions.controllers.arturia.keylab.mk3.controls;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.MidiProcessor;

public class RgbNoteButton extends RgbButton {
    
    public RgbNoteButton(final String name, final int noteNr, final int ledId, final HardwareSurface surface,
        final MidiProcessor midiProcessor) {
        super(name, noteNr, ledId, surface, midiProcessor);
        final MidiIn midiIn = midiProcessor.getMidiIn();
        hwButton.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(9, noteNr));
        hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(9, noteNr));
    }
    
}
