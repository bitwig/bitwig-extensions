package com.bitwig.extensions.controllers.akai.mpkmk4.controls;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.controllers.akai.mpkminiplus.RelativeValueBinding;
import com.bitwig.extensions.framework.Layer;

public class Encoder {
    private final RelativeHardwareKnob encoder;
    
    public Encoder(final int index, final int ccNr, final HardwareSurface surface, final MidiIn midiIn) {
        encoder = surface.createRelativeHardwareKnob("ENCODER_" + (index + 1));
        encoder.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0, ccNr, 200));
        encoder.setStepSize(0.1);
    }
    
    public void setStepSize(final double value) {
        encoder.setStepSize(value);
    }
    
    public void bindParameter(final Layer layer, final Parameter parameter) {
        final RelativeValueBinding binding = new RelativeValueBinding(encoder, parameter);
        layer.addBinding(binding);
    }
    
    public void bindValue(final Layer layer, final SettableRangedValue value) {
        final RelativeValueBinding binding = new RelativeValueBinding(encoder, value);
        layer.addBinding(binding);
    }
    
    public RelativeHardwareKnob getEncoder() {
        return encoder;
    }
}
