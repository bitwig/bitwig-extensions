package com.bitwig.extensions.controllers.novation.slmk3.control;

import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RelativeHardwareValueMatcher;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.controllers.novation.slmk3.MidiProcessor;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.RelativeHardwareControlToRangedValueBinding;

public class SlEncoder {
    private final RelativeHardwareKnob encoder;
    private final RelativeHardwareValueMatcher nonAcceleratedMatchers;
    private final RelativeHardwareValueMatcher acceleratedMatchers;
    private final EncoderMode encoderMode = EncoderMode.ACCELERATED;
    private final MidiProcessor midiProcessor;
    
    public enum EncoderMode {
        ACCELERATED,
        NONACCELERATED
    }
    
    public SlEncoder(final HardwareSurface surface, final MidiProcessor midiProcessor, final int index) {
        encoder = surface.createRelativeHardwareKnob("ENCODER_" + index);
        this.midiProcessor = midiProcessor;
        nonAcceleratedMatchers = midiProcessor.createNonAcceleratedMatcher(0x15 + index);
        acceleratedMatchers = midiProcessor.createAcceleratedMatcher(0x15 + index);
        setEncoderBehavior(encoderMode, 64);
    }
    
    public void setEncoderBehavior(final EncoderMode mode, final int stepSizeDivisor) {
        if (mode == EncoderMode.ACCELERATED) {
            encoder.setAdjustValueMatcher(acceleratedMatchers);
            encoder.setStepSize(1.0 / stepSizeDivisor);
        } else if (mode == EncoderMode.NONACCELERATED) {
            encoder.setAdjustValueMatcher(nonAcceleratedMatchers);
            encoder.setStepSize(1);
        }
    }
    
    public void bindIncrementAction(final Layer layer, final IntConsumer changeAction) {
        layer.bind(encoder, midiProcessor.createIncAction(changeAction));
    }
    
    private RelativeHardwareControlToRangedValueBinding createEncoderToParamBinding(final SettableRangedValue param,
        final double sensitivity) {
        final RelativeHardwareControlToRangedValueBinding binding =
            new RelativeHardwareControlToRangedValueBinding(encoder, param);
        binding.setSensitivity(sensitivity);
        return binding;
    }
    
    private RelativeHardwareControlToRangedValueBinding createEncoderToParamBinding(final SettableRangedValue param) {
        final RelativeHardwareControlToRangedValueBinding binding =
            new RelativeHardwareControlToRangedValueBinding(encoder, param);
        binding.setSensitivity(1);
        return binding;
    }
    
    public RelativeHardwareKnob getEncoder() {
        return encoder;
    }
    
    public void bind(final Layer layer, final RelativeHardwarControlBindable bindable) {
        layer.bind(encoder, bindable);
    }
    
    public void bindEmpty(final Layer layer) {
        layer.bind(encoder, value -> {});
    }
    
    public void bindParameter(final Layer layer, final SettableRangedValue parameter) {
        layer.addBinding(createEncoderToParamBinding(parameter));
    }
}
