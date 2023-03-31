package com.bitwig.extensions.controllers.akai.apcmk2.control;

import com.bitwig.extension.controller.api.HardwareBinding;
import com.bitwig.extension.controller.api.RelativeHardwareControlBinding;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.framework.Binding;

public class RelativeValueBinding extends Binding<RelativeHardwareKnob, SettableRangedValue> {

    private HardwareBinding hwBinding;
    private final double sensitivity;

    public RelativeValueBinding(final RelativeHardwareKnob source, final SettableRangedValue target,
                                         final double sensitivity) {
        super(source, source, target);
        this.sensitivity = sensitivity;
    }

    protected RelativeHardwareControlBinding getHardwareBinding() {
        return getTarget().addBindingWithSensitivity(getSource(), sensitivity);
    }

    public void reset() {
        if (!isActive()) {
            return;
        }
        if (hwBinding != null) {
            hwBinding.removeBinding();
        }
        hwBinding = getHardwareBinding();
    }

    @Override
    protected void deactivate() {
        if (hwBinding != null) {
            hwBinding.removeBinding();
            hwBinding = null;
        }
    }

    @Override
    protected void activate() {
        if (hwBinding != null) {
            hwBinding.removeBinding();
        }
        hwBinding = getHardwareBinding();
    }

}
