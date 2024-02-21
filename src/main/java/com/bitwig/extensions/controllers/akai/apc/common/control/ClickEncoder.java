package com.bitwig.extensions.controllers.akai.apc.common.control;

import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RelativeHardwareValueMatcher;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.framework.Layer;

public class ClickEncoder {
    private final RelativeHardwareKnob encoder;
    private final ControllerHost host;
    
    public ClickEncoder(int ccNr, final ControllerHost host, final HardwareSurface surface, MidiIn midiIn) {
        encoder = surface.createRelativeHardwareKnob("ENCODER_" + ccNr);
        this.host = host;
        final RelativeHardwareValueMatcher stepUpMatcher =
            midiIn.createRelativeValueMatcher("(status == 176 && data1 == %d && data2==1)".formatted(ccNr), 1);
        final RelativeHardwareValueMatcher stepDownMatcher =
            midiIn.createRelativeValueMatcher("(status == 176 && data1 == %d && data2==127)".formatted(ccNr), -1);
        
        final RelativeHardwareValueMatcher matcher =
            host.createOrRelativeHardwareValueMatcher(stepDownMatcher, stepUpMatcher);
        encoder.setAdjustValueMatcher(matcher);
        encoder.setStepSize(1);
    }
    
    public void setStepSize(final double value) {
        encoder.setStepSize(value);
    }
    
    public void bindParameter(final Layer layer, final Parameter parameter) {
        final RelativeValueBinding binding = new RelativeValueBinding(encoder, parameter);
        layer.addBinding(binding);
    }
    
    public void bind(final Layer layer, final SettableRangedValue value) {
        final RelativeValueBinding binding = new RelativeValueBinding(encoder, value);
        layer.addBinding(binding);
    }
    
    public void bind(final Layer layer, IntConsumer action) {
        final HardwareActionBindable incAction = host.createAction(() -> action.accept(1), () -> "+");
        final HardwareActionBindable decAction = host.createAction(() -> action.accept(-1), () -> "-");
        layer.bind(encoder, host.createRelativeHardwareControlStepTarget(incAction, decAction));
    }
}
