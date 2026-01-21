package com.bitwig.extensions.controllers.allenheath.xonek3.control;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.controllers.allenheath.xonek3.XoneMidiDevice;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.MidiStatus;

public class XoneEncoder {
    private final RelativeHardwareKnob hwEncoder;
    private final XoneRgbButton pushButton;
    
    public XoneEncoder(final int index, final int ledIndex, final int channel, final int ccNr, final int buttonNoteNr,
        final String name, final XoneMidiDevice midiProcessor, final HardwareSurface surface) {
        this.hwEncoder = surface.createRelativeHardwareKnob("%s %d".formatted(name, index + 1));
        final MidiIn midiIn = midiProcessor.getMidiIn();
        
        hwEncoder.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(channel, ccNr, 40));
        hwEncoder.setStepSize(0.025);
        pushButton = new XoneRgbButton(
            index, ledIndex, "%s Button".formatted(name), MidiStatus.NOTE_ON, buttonNoteNr + index, channel, surface,
            midiProcessor);
    }
    
    public void bindEncoder(final Layer layer, final RelativeHardwarControlBindable bindable) {
        layer.bind(hwEncoder, bindable);
    }
    
    public XoneRgbButton getPushButton() {
        return pushButton;
    }
}
