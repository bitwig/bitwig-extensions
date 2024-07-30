package com.bitwig.extensions.controllers.mcu.devices;

import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extension.controller.api.AbsoluteHardwareControlBinding;
import com.bitwig.extension.controller.api.DoubleValue;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwareControl;
import com.bitwig.extension.controller.api.RelativeHardwareControlToRangedValueBinding;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.controllers.mcu.bindings.paramslots.ResetableAbsoluteValueSlotBinding;
import com.bitwig.extensions.controllers.mcu.bindings.paramslots.ResetableRelativeSlotBinding;
import com.bitwig.extensions.controllers.mcu.bindings.paramslots.RingParameterDisplaySlotBinding;
import com.bitwig.extensions.controllers.mcu.control.MotorSlider;
import com.bitwig.extensions.controllers.mcu.control.RingDisplayType;
import com.bitwig.extensions.controllers.mcu.control.RingEncoder;
import com.bitwig.extensions.framework.values.BasicDoubleValue;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.IntValueObject;

import java.util.ArrayList;
import java.util.List;

public class ParamPageSlot {
    private static final int RING_RANGE = 10;

    private final List<DeviceParameter> parameters = new ArrayList<>();
    private final int slotIndex;
    private final SpecificDevice device;
    private final BasicStringValue nameValue = new BasicStringValue("");
    private final BasicStringValue displayValue = new BasicStringValue("");
    private final IntValueObject ringValue = new IntValueObject(0, 0, 10);
    private final BasicDoubleValue doubleValue = new BasicDoubleValue();
    private final BooleanValueObject existsValue = new BooleanValueObject();
    private final BooleanValueObject enabledValue = new BooleanValueObject();

    public ParamPageSlot(final int index, final SpecificDevice device) {
        this.slotIndex = index;
        this.device = device;
        device.addExistChangeListener(exists -> {
            existsValue.set(exists);
        });
    }


    public void notifyEnablement(final boolean enabled) {
        this.enabledValue.set(enabled);
    }

    public void addParameter(final DeviceParameter deviceParameter) {
        final Parameter param = deviceParameter.getParameter();
        final int runningIndex = parameters.size();
        param.value().addValueObserver(v -> handleValueChanged(runningIndex, v));
        param.displayedValue().addValueObserver(v -> handleDisplayValueChanged(runningIndex, v));
        parameters.add(deviceParameter);
        if (runningIndex == 0) {
            update();
        }
    }

    public void update() {
        final DeviceParameter currentParameter = getCurrentParameter();
        nameValue.set(currentParameter.getName());
        displayValue.set(currentParameter.getStringValue());
        ringValue.set(currentParameter.getRingValue());
        doubleValue.set(currentParameter.getParameter().getAsDouble());
        existsValue.set(true); // Need to deal with empty slots
    }

    private void handleDisplayValueChanged(final int index, final String value) {
        if (index != device.getCurrentPage()) {
            return;
        }
        final CustomValueConverter valueConverter = getCurrentParameter().getCustomValueConverter();
        if (valueConverter == null) {
            displayValue.set(value);
        }
    }

    private void handleValueChanged(final int index, final double value) {
        if (index != device.getCurrentPage()) {
            return;
        }
        ringValue.set((int) Math.round(value * RING_RANGE));
        doubleValue.set(value);
        final CustomValueConverter valueConverter = getCurrentParameter().getCustomValueConverter();
        if (valueConverter != null) {
            displayValue.set(valueConverter.convert(value));
        }
    }

    public ResetableRelativeSlotBinding getRelativeEncoderBinding(final RelativeHardwareKnob encoder) {
        return new ResetableRelativeSlotBinding(encoder, this, getCurrentParameter().getSensitivity());
    }

    public ResetableAbsoluteValueSlotBinding getFaderBinding(final MotorSlider fader) {
        return fader.createSlotBinding(this);
    }

    public AbsoluteHardwareControlBinding addBinding(final AbsoluteHardwareControl hardwareControl) {
        return this.addBindingWithRange(hardwareControl, 0.0, 1.0);
    }

    public AbsoluteHardwareControlBinding addBindingWithRange(final AbsoluteHardwareControl hardwareControl,
                                                              final double minNormalizedValue,
                                                              final double maxNormalizedValue) {
        return getCurrentParameter().getParameter()
                .addBindingWithRange(hardwareControl, minNormalizedValue, maxNormalizedValue);
    }

    public RelativeHardwareControlToRangedValueBinding addBindingWithRangeAndSensitivity(
            final RelativeHardwareControl hardwareControl, final double minNormalizedValue,
            final double maxNormalizedValue, final double sensitivity) {
        final DeviceParameter currentParameter = getCurrentParameter();
        return currentParameter.getParameter()
                .addBindingWithRangeAndSensitivity(hardwareControl, minNormalizedValue, maxNormalizedValue,
                        currentParameter.getSensitivity());
    }

    public RelativeHardwareControlToRangedValueBinding addBindingWithSensitivity(
            final RelativeHardwareControl hardwareControl, final double sensitivity) {
        return this.addBindingWithRangeAndSensitivity(hardwareControl, 0.0, 1.0, sensitivity);
    }

    public RingParameterDisplaySlotBinding createRingBinding(final RingEncoder encoder) {
        final RingParameterDisplaySlotBinding ringBinding = encoder.createDisplayBinding(this);
        return ringBinding;
    }

    public String getCurrentValue() {
        return getCurrentParameter().getStringValue();
    }

    public double getCurrentDoubleValue() {
        return getCurrentParameter().getParameter().get();
    }

    public void parameterReset() {
        getCurrentParameter().doReset();
    }

    public BooleanValueObject getEnabledValue() {
        return enabledValue;
    }

    private DeviceParameter getCurrentParameter() {
        return parameters.get(device.getCurrentPage());
    }

    public BasicStringValue getDisplayValue() {
        return displayValue;
    }

    public BasicStringValue getNameValue() {
        return nameValue;
    }

    public IntValueObject getRingValue() {
        return ringValue;
    }

    public BooleanValueObject getExistsValue() {
        return existsValue;
    }

    public RingDisplayType getRingDisplayType() {
        return getCurrentParameter().getRingDisplayType();
    }

    public String getCurrentName() {
        return getCurrentParameter().getName();
    }

    public void touch(final boolean value) {
        getCurrentParameter().getParameter().touch(value);
    }

    public DoubleValue getValue() {
        return doubleValue;
    }

    public int getSlotIndex() {
        return slotIndex;
    }
}
