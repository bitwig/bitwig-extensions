package com.bitwig.extensions.controllers.mcu.control;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RelativeHardwareValueMatcher;
import com.bitwig.extensions.controllers.mcu.MidiProcessor;
import com.bitwig.extensions.controllers.mcu.TimedProcessor;
import com.bitwig.extensions.controllers.mcu.config.ButtonAssignment;
import com.bitwig.extensions.controllers.mcu.config.ControllerConfig;
import com.bitwig.extensions.controllers.mcu.config.EncoderBehavior;
import com.bitwig.extensions.controllers.mcu.config.McuFunction;
import com.bitwig.extensions.controllers.mcu.display.TimeCodeLed;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;

public class MainHardwareSection {
    
    private static final int JOG_WHEEL_CC = 60;
    private final RelativeHardwareKnob jogWheelEncoder;
    private final ControllerHost host;
    private final Map<McuFunction, McuButton> buttonMap = new HashMap<>();
    private final TimeCodeLed timeCodeLed;
    private final MidiProcessor midiProcessor;
    
    public MainHardwareSection(final Context context, final MidiProcessor midiProcessor, final int subIndex) {
        final HardwareSurface surface = context.getService(HardwareSurface.class);
        final ControllerConfig config = context.getService(ControllerConfig.class);
        final TimedProcessor timedProcessor = context.getService(TimedProcessor.class);
        host = context.getService(ControllerHost.class);
        this.midiProcessor = midiProcessor;
        
        for (final McuFunction function : McuFunction.values()) {
            final ButtonAssignment assignment = config.getAssignment(function);
            if (assignment != null) {
                buttonMap.put(function, new McuButton(assignment, subIndex, surface, midiProcessor, timedProcessor));
            }
        }
        jogWheelEncoder = surface.createRelativeHardwareKnob("JOG_WHEEL_" + subIndex);
        switch (config.getJogWheelBehavior()) {
            case ACCEL -> setUpAccelerated(midiProcessor);
            case STEP -> setUp2Complement(midiProcessor);
            case STEP_1_65 -> setUpStepped(midiProcessor, config.getJogWheelBehavior());
        }
        timeCodeLed = config.hasTimecodeLed() ? new TimeCodeLed(midiProcessor) : null;
    }
    
    private void setUpAccelerated(final MidiProcessor midiProcessor) {
        jogWheelEncoder.setAdjustValueMatcher(
            midiProcessor.getMidiIn().createRelativeSignedBitCCValueMatcher(0, JOG_WHEEL_CC, 100));
        jogWheelEncoder.setStepSize(1.0 / 50.0);
    }
    
    private void setUp2Complement(final MidiProcessor midiProcessor) {
        jogWheelEncoder.setAdjustValueMatcher(
            midiProcessor.getMidiIn().createRelative2sComplementCCValueMatcher(0, JOG_WHEEL_CC, 100));
        jogWheelEncoder.setStepSize(1.0);
    }
    
    private void setUpStepped(final MidiProcessor midiProcessor, final EncoderBehavior behavior) {
        final RelativeHardwareValueMatcher stepUpMatcher = midiProcessor.getMidiIn().createRelativeValueMatcher(
            "(status == 176 && data1 == %d && data2 == %d)".formatted(JOG_WHEEL_CC, behavior.getUpValue()), 1);
        final RelativeHardwareValueMatcher stepDownMatcher = midiProcessor.getMidiIn().createRelativeValueMatcher(
            "(status == 176 && data1 == %d && data2 == %d)".formatted(JOG_WHEEL_CC, behavior.getDownValue()), -1);
        final RelativeHardwareValueMatcher matcher =
            host.createOrRelativeHardwareValueMatcher(stepDownMatcher, stepUpMatcher);
        jogWheelEncoder.setAdjustValueMatcher(matcher);
    }
    
    public MidiProcessor getMidiProcessor() {
        return midiProcessor;
    }
    
    public Optional<TimeCodeLed> getTimeCodeLed() {
        return Optional.ofNullable(timeCodeLed);
    }
    
    public void bindJogWheel(final Layer layer, final IntConsumer value) {
        layer.bind(jogWheelEncoder, createIncrementBinder(value));
    }
    
    private RelativeHardwarControlBindable createIncrementBinder(final IntConsumer consumer) {
        return host.createRelativeHardwareControlStepTarget(//
            host.createAction(() -> consumer.accept(1), () -> "+"),
            host.createAction(() -> consumer.accept(-1), () -> "-"));
    }
    
    public Optional<McuButton> getButton(final McuFunction assignment) {
        return Optional.ofNullable(buttonMap.get(assignment));
    }
    
    public void clearAll() {
        timeCodeLed.clearAll();
        buttonMap.forEach((key, value) -> value.clear(midiProcessor));
    }
}
