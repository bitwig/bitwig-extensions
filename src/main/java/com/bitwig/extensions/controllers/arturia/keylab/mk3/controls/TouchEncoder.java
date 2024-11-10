package com.bitwig.extensions.controllers.arturia.keylab.mk3.controls;

import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.MidiProcessor;
import com.bitwig.extensions.framework.Layer;

public class TouchEncoder extends TouchControl {
    
    private final RelativeHardwareKnob encoder;
    
    public TouchEncoder(final int id, final int ccNr, final int ccTouchNr, final HardwareSurface surface,
        final MidiProcessor midiProcessor) {
        super(surface.createHardwareButton("ENCODER_BUTTON_%d".formatted(id + 1)), id, ccTouchNr, midiProcessor);
        final MidiIn midiIn = midiProcessor.getMidiIn();
        encoder = surface.createRelativeHardwareKnob("ENCODER_%d".formatted(id + 1));
        encoder.setAdjustValueMatcher(midiIn.createRelativeBinOffsetCCValueMatcher(0, ccNr, 400));
        encoder.setStepSize(0.01);
    }
    
    public void bindParameter(final Layer layer, final Parameter parameter, final StringValue labelValue) {
        layer.bind(encoder, parameter);
        layer.bind(hwButton, hwButton.pressedAction(), () -> parameter.touch(true));
        layer.bind(hwButton, hwButton.releasedAction(), () -> parameter.touch(false));
        layer.addBinding(new TouchDisplayBinding(this, parameter, labelValue));
    }
    
    public void bind(final Layer layer, final IntConsumer changeAction) {
        layer.bind(encoder, midiProcessor.createIncrementBindable(changeAction));
    }
    
    
}
