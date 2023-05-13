package com.bitwig.extensions.controllers.akai.apcmk2.control;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.Midi;

public class Encoder {
    private final RelativeHardwareKnob encoder;

    public Encoder(int ccNr, final HardwareSurface surface, MidiIn midiIn) {
        encoder = surface.createRelativeHardwareKnob("ENCODER_" + ccNr);
        final String matchExpr = String.format("(status==%d && data1==%d && data2>0)", Midi.CC, ccNr);
        encoder.setAdjustValueMatcher(midiIn.createRelative2sComplementValueMatcher(matchExpr, "0-data2", 7, 200));
        encoder.setStepSize(0.1);
    }

    public void setStepSize(final double value) {
        encoder.setStepSize(value);
    }

    public void bindParameter(final Layer layer, final Parameter parameter, final double sensitivity) {
        final RelativeValueBinding binding = new RelativeValueBinding(encoder, parameter, -sensitivity);
        layer.addBinding(binding);
    }

    public void bind(final Layer layer, final SettableRangedValue value, final double sensitivity) {
        final RelativeValueBinding binding = new RelativeValueBinding(encoder, value, -sensitivity);
        layer.addBinding(binding);
    }
}
