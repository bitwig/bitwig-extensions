package com.bitwig.extensions.controllers.arturia.keylab.mk3.controls;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.CcAssignment;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.MidiProcessor;

public class RgbCcButton extends RgbButton {
    
    public RgbCcButton(final CcAssignment assignment, final HardwareSurface surface,
        final MidiProcessor midiProcessor) {
        super(assignment.toString(), assignment.getCcNr(), assignment.getLedId(), surface, midiProcessor);
        final MidiIn midiIn = midiProcessor.getMidiIn();
        rgbCommand[9] = assignment.getLedId();
        hwButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, assignment.getCcNr(), 127));
        hwButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(0, assignment.getCcNr(), 0));
    }
    
    public RgbCcButton(final String name, final int ccValue, final int ledId, final HardwareSurface surface,
        final MidiProcessor midiProcessor) {
        super(name, ccValue, ledId, surface, midiProcessor);
        final MidiIn midiIn = midiProcessor.getMidiIn();
        hwButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, ccValue, 127));
        hwButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(0, ccValue, 0));
    }
    
}
