package com.bitwig.extensions.controllers.novation.commonsmk3;

import com.bitwig.extension.controller.api.HardwareSurface;

public class LabeledButton extends LaunchPadButton {
    private final int ccValue;

    public LabeledButton(final String name, final HardwareSurface surface, final MidiProcessor midiProcessor,
                         final int ccValue) {
        super(name.toLowerCase() + "_" + ccValue, surface, midiProcessor, 0);
        this.ccValue = ccValue;
        initButtonCc(midiProcessor.getMidiIn(), ccValue);
        light.state().onUpdateHardware(state -> midiProcessor.updatePadLed(state, ccValue));
    }

    public LabeledButton(final HardwareSurface surface, final MidiProcessor midiProcessor,
                         final CCSource ccAssignment) {
        super(ccAssignment.toString().toLowerCase(), surface, midiProcessor, 0);
        ccValue = ccAssignment.getCcValue();
        initButtonCc(midiProcessor.getMidiIn(), ccAssignment);
        light.state().onUpdateHardware(state -> midiProcessor.updatePadLed(state, ccValue));
    }

    @Override
    public void refresh() {
        midiProcessor.updatePadLed(light.state().currentValue(), ccValue);
    }

}
