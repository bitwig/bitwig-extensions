package com.bitwig.extensions.controllers.mcu.layer;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.mcu.StringUtil;
import com.bitwig.extensions.controllers.mcu.bindings.display.DisplayTarget;
import com.bitwig.extensions.controllers.mcu.bindings.display.ModelessDisplayBinding;
import com.bitwig.extensions.controllers.mcu.bindings.display.StringDisplayBinding;
import com.bitwig.extensions.controllers.mcu.control.RingDisplayType;
import com.bitwig.extensions.controllers.mcu.control.RingEncoder;
import com.bitwig.extensions.controllers.mcu.display.DisplayManager;
import com.bitwig.extensions.controllers.mcu.display.DisplayRow;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.IntValueObject;

public class LayerGroup {
    private static final BasicStringValue EMPTY = new BasicStringValue("");
    
    private final Layer labelLayer;
    private final Layer valueLayer;
    private final Layer encoderLayer;
    private final String name;
    
    public LayerGroup(final Context context, final String name) {
        this.name = name;
        this.labelLayer = context.createLayer("LABEL_%s".formatted(name));
        this.valueLayer = context.createLayer("VALUE_%s".formatted(name));
        this.encoderLayer = context.createLayer("ENCODER_%s".formatted(name));
    }
    
    public String getName() {
        return name;
    }
    
    public Layer getLabelLayer() {
        return labelLayer;
    }
    
    public Layer getEncoderLayer() {
        return encoderLayer;
    }
    
    public Layer getValueLayer() {
        return valueLayer;
    }
    
    public void bindControls(final RingEncoder encoder, final RingDisplayType ringType, final Parameter parameter) {
        encoder.bindParameter(this.encoderLayer, parameter, ringType);
    }
    
    public void bindControls(final RingEncoder encoder, final RingDisplayType ringType,
        final SettableRangedValue parameter) {
        encoder.bindValue(this.encoderLayer, parameter, ringType);
    }
    
    public void bindControls(final RingEncoder encoder, final RelativeHardwarControlBindable bindable) {
        encoder.bind(this.encoderLayer, bindable);
    }
    
    public void bindPressAction(final RingEncoder encoder, final Runnable action) {
        encoder.bindPressed(this.encoderLayer, action);
    }
    
    public void bindControls(final RingEncoder encoder, final RingDisplayType ringType,
        final SettableRangedValue parameter, final double sensitivity) {
        encoder.bindValue(this.encoderLayer, parameter, ringType, sensitivity);
    }
    
    public void bindEncoderIsPressed(final RingEncoder encoder, final Consumer<Boolean> booleanConsumer) {
        encoder.bindIsPressed(encoderLayer, booleanConsumer);
    }
    
    public void bindEncoderIsPressed(final RingEncoder encoder, final SettableBooleanValue booleanValue) {
        encoder.bindIsPressed(encoderLayer, booleanValue);
    }
    
    public void bindEncoderPressed(final RingEncoder encoder, final Runnable action) {
        encoder.bindPressed(encoderLayer, action);
    }
    
    public void bindRingValue(final RingEncoder encoder, final BooleanValue value) {
        encoder.bindRingValue(encoderLayer, value);
    }
    
    public void bindRingValue(final RingEncoder encoder, final IntValueObject value) {
        encoder.bindRingValue(encoderLayer, value);
    }
    
    public void bindEncoderIncrement(final RingEncoder encoder, final IntConsumer incHandler,
        final double incrementMultiplier) {
        encoder.bindIncrement(encoderLayer, incHandler, incrementMultiplier);
    }
    
    public void bindEncoderIncrement(final RingEncoder encoder, final HardwareActionBindable incAction,
        final HardwareActionBindable decAction, final double incrementMultiplier) {
        encoder.bindIncrement(encoderLayer, inc -> {
            if (inc > 0) {
                incAction.invoke();
            } else {
                decAction.invoke();
            }
        }, incrementMultiplier);
    }
    
    
    public void bindControlsInc(final RingEncoder encoder, final IntConsumer incHandler, final Parameter parameter,
        final RingDisplayType type, final double incrementMultiplier) {
        encoder.bindIncrement(encoderLayer, incHandler, incrementMultiplier);
        encoder.bindRingValue(encoderLayer, parameter, type);
    }
    
    public void bindEncoderTurnedBoolean(final RingEncoder encoder, final SettableBooleanValue value) {
        encoder.bindIncrement(encoderLayer, value);
    }
    
    public void bindDisplay(final DisplayManager displayManager, final Parameter parameter, final int index) {
        final BooleanValue exists = parameter.exists();
        final StringValue labelValue = parameter.name();
        labelLayer.addBinding(
            new StringDisplayBinding(displayManager, ControlMode.MENU, DisplayTarget.of(DisplayRow.LABEL, index),
                labelValue, exists, name -> StringUtil.reduceAscii(name, 6)));
        valueLayer.addBinding(
            new StringDisplayBinding(displayManager, ControlMode.MENU, DisplayTarget.of(DisplayRow.VALUE, index),
                parameter.displayedValue(), exists));
    }
    
    public void bindDisplay(final DisplayManager displayManager, final String label, final SettableRangedValue value,
        final int index) {
        final StringValue labelValue = new BasicStringValue(label);
        labelLayer.addBinding(
            new ModelessDisplayBinding(displayManager, DisplayTarget.of(DisplayRow.LABEL, index), labelValue));
        valueLayer.addBinding(new ModelessDisplayBinding(displayManager, DisplayTarget.of(DisplayRow.VALUE, index),
            value.displayedValue()));
    }
    
    public void bindDisplay(final DisplayManager displayManager, final StringValue label, final StringValue value,
        final int index) {
        labelLayer.addBinding(
            new ModelessDisplayBinding(displayManager, DisplayTarget.of(DisplayRow.LABEL, index), label));
        valueLayer.addBinding(
            new ModelessDisplayBinding(displayManager, DisplayTarget.of(DisplayRow.VALUE, index), value));
    }
    
    public void bindDisplay(final DisplayManager displayManager, final String label, final StringValue value,
        final int index) {
        final StringValue labelValue = new BasicStringValue(label);
        labelLayer.addBinding(
            new ModelessDisplayBinding(displayManager, DisplayTarget.of(DisplayRow.LABEL, index), labelValue));
        valueLayer.addBinding(
            new ModelessDisplayBinding(displayManager, DisplayTarget.of(DisplayRow.VALUE, index), value));
    }
    
    public void bindDisplay(final DisplayManager displayManager, final String name, final BooleanValue value,
        final int index) {
        value.markInterested();
        final BasicStringValue onOffValue = new BasicStringValue(value.get() ? " ON" : " OFF");
        value.addValueObserver(val -> onOffValue.set(val ? " ON" : " OFF"));
        labelLayer.addBinding(new ModelessDisplayBinding(displayManager, DisplayTarget.of(DisplayRow.LABEL, index),
            new BasicStringValue(name)));
        valueLayer.addBinding(
            new ModelessDisplayBinding(displayManager, DisplayTarget.of(DisplayRow.VALUE, index), onOffValue));
    }
    
    public void bindEncoderEmpty(final RingEncoder encoder) {
        encoder.bindEmpty(encoderLayer);
    }
    
    public void bindEmpty(final DisplayManager displayManager, final RingEncoder encoder, final int index) {
        encoder.bindEmpty(encoderLayer);
        labelLayer.addBinding(
            new ModelessDisplayBinding(displayManager, DisplayTarget.of(DisplayRow.LABEL, index), EMPTY));
        valueLayer.addBinding(
            new ModelessDisplayBinding(displayManager, DisplayTarget.of(DisplayRow.VALUE, index), EMPTY));
    }
    
    public void bindRingToIsPressed(final RingEncoder ringEncoder) {
        ringEncoder.bindRingPressed(encoderLayer);
    }
    
    
    public void bindRingEmpty(final RingEncoder ringEncoder) {
        ringEncoder.bindRingEmpty(encoderLayer);
    }
}
