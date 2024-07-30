package com.bitwig.extensions.controllers.mcu.control;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RelativeHardwareValueMatcher;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.controllers.mcu.MidiProcessor;
import com.bitwig.extensions.controllers.mcu.bindings.ButtonBinding;
import com.bitwig.extensions.controllers.mcu.bindings.RingDisplayDisabledBinding;
import com.bitwig.extensions.controllers.mcu.bindings.RingDisplayIntValueBinding;
import com.bitwig.extensions.controllers.mcu.bindings.RingDisplayParameterBinding;
import com.bitwig.extensions.controllers.mcu.bindings.RingDisplayParameterBoolBinding;
import com.bitwig.extensions.controllers.mcu.bindings.RingDisplayValueBinding;
import com.bitwig.extensions.controllers.mcu.bindings.paramslots.ResetableRelativeSlotBinding;
import com.bitwig.extensions.controllers.mcu.bindings.paramslots.RingParameterDisplaySlotBinding;
import com.bitwig.extensions.controllers.mcu.config.McuAssignments;
import com.bitwig.extensions.controllers.mcu.devices.ParamPageSlot;
import com.bitwig.extensions.controllers.mcu.value.IncrementHolder;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.RelativeHardwareControlToRangedValueBinding;
import com.bitwig.extensions.framework.values.IntValueObject;

public class RingEncoder {
    private final RelativeHardwareKnob encoder;
    private final HardwareButton encoderPress;
    private final RingDisplay ringDisplay;
    private final RelativeHardwareValueMatcher nonAcceleratedMatchers;
    private final RelativeHardwareValueMatcher acceleratedMatchers;
    private final EncoderMode encoderMode = EncoderMode.ACCELERATED;
    private final MidiProcessor midiProcessor;
    
    public RingEncoder(final HardwareSurface surface, final MidiProcessor midiProcessor, final int index) {
        this.midiProcessor = midiProcessor;
        encoder = surface.createRelativeHardwareKnob("PAN_KNOB" + midiProcessor.getPortIndex() + "_" + index);
        encoderPress = surface.createHardwareButton("ENCODER_PRESS_" + midiProcessor.getPortIndex() + "_" + index);
        midiProcessor.attachNoteOnOffMatcher(encoderPress, 0, McuAssignments.ENC_PRESS_BASE.getNoteNo() + index);
        ringDisplay = new RingDisplay(midiProcessor, index);
        encoder.setHardwareButton(encoderPress);
        nonAcceleratedMatchers = midiProcessor.createNonAcceleratedMatcher(0x10 + index);
        acceleratedMatchers = midiProcessor.createAcceleratedMatcher(0x10 + index);
        setEncoderBehavior(encoderMode, 128);
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
    
    public void bindParameter(final Layer layer, final ParamPageSlot slot) {
        layer.addBinding(new ResetableRelativeSlotBinding(encoder, slot, 1.0));
        layer.addBinding(createDisplayBinding(slot));
        layer.addBinding(new ButtonBinding(encoderPress, midiProcessor.createAction(slot::parameterReset)));
    }
    
    public RingParameterDisplaySlotBinding createDisplayBinding(final ParamPageSlot slot) {
        return new RingParameterDisplaySlotBinding(slot, ringDisplay);
    }
    
    public void bindIncrement(final Layer layer, final IntConsumer consumer, final double incrementMultiplier) {
        final IncrementHolder incHolder = new IncrementHolder(consumer, incrementMultiplier);
        layer.bind(encoder, v -> incHolder.increment(v));
    }
    
    public void bindIncrement(final Layer layer, final SettableBooleanValue value) {
        final IncrementHolder incHolder = new IncrementHolder(v -> value.set(v > 0), 0.1);
        layer.bind(encoder, v -> incHolder.increment(v));
    }
    
    public void bindIsPressed(final Layer layer, final Consumer<Boolean> pressAction) {
        layer.bind(encoderPress, encoderPress.pressedAction(), () -> pressAction.accept(true));
        layer.bind(encoderPress, encoderPress.releasedAction(), () -> pressAction.accept(false));
    }
    
    public void bindIsPressed(final Layer layer, final SettableBooleanValue pressValue) {
        layer.bind(encoderPress, encoderPress.pressedAction(), () -> pressValue.set(true));
        layer.bind(encoderPress, encoderPress.releasedAction(), () -> pressValue.set(false));
    }
    
    public void bindPressed(final Layer layer, final Runnable pressAction) {
        layer.bind(encoderPress, encoderPress.pressedAction(), pressAction);
    }
    
    public void bindRingValue(final Layer layer, final Parameter parameter, final RingDisplayType type) {
        layer.addBinding(new RingDisplayParameterBinding(parameter, ringDisplay, type));
    }
    
    public void bindRingValue(final Layer layer, final BooleanValue value) {
        layer.addBinding(new RingDisplayParameterBoolBinding(value, ringDisplay));
    }
    
    public void bindRingValue(final Layer layer, final IntValueObject value) {
        layer.addBinding(new RingDisplayIntValueBinding(value, ringDisplay));
    }
    
    public void bindEmpty(final Layer layer) {
        layer.addBinding(new RingDisplayDisabledBinding(ringDisplay, RingDisplayType.FILL_LR));
        layer.bind(encoder, v -> {
        });
        layer.bindPressed(encoderPress, () -> {
        });
    }
    
    public void bindRingEmpty(final Layer layer) {
        layer.addBinding(new RingDisplayDisabledBinding(ringDisplay, RingDisplayType.FILL_LR));
    }
    
    public void bindRingPressed(final Layer layer) {
        layer.addBinding(new RingDisplayParameterBoolBinding(encoderPress.isPressed(), ringDisplay));
    }
    
    public void bindValue(final Layer layer, final SettableRangedValue value, final RingDisplayType type) {
        layer.addBinding(createEncoderToParamBinding(value));
        layer.addBinding(new RingDisplayValueBinding(value, ringDisplay, type));
    }
    
    private RelativeHardwareControlToRangedValueBinding createEncoderToParamBinding(final SettableRangedValue param) {
        final RelativeHardwareControlToRangedValueBinding binding =
            new RelativeHardwareControlToRangedValueBinding(encoder, param);
        if (midiProcessor.isHas2ClickResolution()) {
            binding.setSensitivity(2.0);
        }
        return binding;
    }
    
    public void bindValue(final Layer layer, final SettableRangedValue value, final RingDisplayType type,
        final double sensitivity) {
        layer.addBinding(createEncoderToParamBinding(value, sensitivity));
        layer.addBinding(new RingDisplayValueBinding(value, ringDisplay, type));
    }
    
    private RelativeHardwareControlToRangedValueBinding createEncoderToParamBinding(final SettableRangedValue param,
        final double sensitivity) {
        final RelativeHardwareControlToRangedValueBinding binding =
            new RelativeHardwareControlToRangedValueBinding(encoder, param);
        // TODO CHECK 2click encoders
        binding.setSensitivity(sensitivity);
        return binding;
    }
    
    public void bindParameter(final Layer layer, final Parameter parameter, final RingDisplayType type) {
        layer.addBinding(createEncoderToParamBinding(parameter));
        layer.addBinding(new ButtonBinding(encoderPress, midiProcessor.createAction(parameter::reset)));
        layer.addBinding(new RingDisplayParameterBinding(parameter, ringDisplay, type));
    }
    
    private RelativeHardwareControlToRangedValueBinding createEncoderToParamBinding(final Parameter param) {
        final RelativeHardwareControlToRangedValueBinding binding =
            new RelativeHardwareControlToRangedValueBinding(encoder, param);
        if (midiProcessor.isHas2ClickResolution()) {
            binding.setSensitivity(2.0);
        }
        return binding;
    }
    
    public RelativeHardwareKnob getEncoder() {
        return encoder;
    }
    
    
    public void bind(final Layer layer, final RelativeHardwarControlBindable bindable) {
        layer.bind(encoder, bindable);
    }
}
