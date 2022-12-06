package com.bitwig.extensions.controllers.arturia.beatsteppro;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.AbsoluteHardwareControlBinding;
import com.bitwig.extensions.framework.Layer;

public class McuSlider {

    private final HardwareSlider fader;
    private final McuFaderResponse response;

    public McuSlider(final int pitchBendChannel, final HardwareSurface surface, final MidiIn midiIn,
                     final MidiOut midiOut) {
        fader = surface.createHardwareSlider("FADER_" + pitchBendChannel);
        fader.setAdjustValueMatcher(midiIn.createAbsolutePitchBendValueMatcher(pitchBendChannel));
        response = new McuFaderResponse(midiOut, pitchBendChannel);
    }

    public void bindParameter(final Layer layer, final Parameter parameter) {
        layer.addBinding(new McuFaderBinding(parameter, response));
        layer.addBinding(new AbsoluteHardwareControlBinding(fader, parameter));
    }

    public HardwareSlider getFader() {
        return fader;
    }

    public void sendValue(final int value) {
        response.sendValue(0);
    }

}
