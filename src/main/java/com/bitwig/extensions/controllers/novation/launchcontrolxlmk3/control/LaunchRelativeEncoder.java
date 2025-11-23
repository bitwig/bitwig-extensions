package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control;

import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RelativeHardwareValueMatcher;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlMidiProcessor;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.ControlTargetId;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.RelativeHardwareControlToRangedValueBinding;

public class LaunchRelativeEncoder extends LaunchKnob {
    
    private final RelativeHardwareKnob encoder;
    private final RelativeHardwareValueMatcher nonAcceleratedMatchers;
    private final RelativeHardwareValueMatcher acceleratedMatchers;
    private final EncoderMode encoderMode = EncoderMode.ACCELERATED;
    
    public enum EncoderMode {
        ACCELERATED,
        NON_ACCELERATED
    }
    
    public LaunchRelativeEncoder(final int index, final HardwareSurface surface,
        final LaunchControlMidiProcessor midiProcessor, final LaunchLight light) {
        super(index, 0x4D + index, midiProcessor, light);
        encoder = surface.createRelativeHardwareKnob("ENCODER_" + index);
        nonAcceleratedMatchers = midiProcessor.createNonAcceleratedMatcher(this.ccNr);
        acceleratedMatchers = midiProcessor.createAcceleratedMatcher(this.ccNr);
        setEncoderBehavior(encoderMode, 64);
    }
    
    @Override
    public ControlTargetId getId() {
        return new ControlTargetId(0xD + index);
    }
    
    public LaunchLight getLight() {
        return light;
    }
    
    public void setEncoderBehavior(final EncoderMode mode) {
        setEncoderBehavior(mode, mode == EncoderMode.ACCELERATED ? 64 : 1);
    }
    
    public void setEncoderBehavior(final EncoderMode mode, final int stepSizeDivisor) {
        if (mode == EncoderMode.ACCELERATED) {
            encoder.setAdjustValueMatcher(acceleratedMatchers);
            encoder.setStepSize(1.0 / stepSizeDivisor);
        } else if (mode == EncoderMode.NON_ACCELERATED) {
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
    
    private RelativeHardwareControlToRangedValueBinding createEncoderToParamBinding(final Parameter param) {
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
        layer.bind(
            encoder, value -> {
            });
    }
    
    public void bindParameter(final Layer layer, final Parameter parameter) {
        layer.addBinding(createEncoderToParamBinding(parameter));
    }
}
