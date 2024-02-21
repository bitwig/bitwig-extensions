package com.bitwig.extensions.controllers.mcu.bindings.paramslots;

import com.bitwig.extension.controller.api.HardwareBinding;
import com.bitwig.extension.controller.api.RelativeHardwareControlBinding;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.controllers.mcu.bindings.ResetableBinding;
import com.bitwig.extensions.controllers.mcu.devices.ParamPageSlot;
import com.bitwig.extensions.framework.Binding;

public class ResetableRelativeSlotBinding extends Binding<RelativeHardwareKnob, ParamPageSlot> implements ResetableBinding {

    private HardwareBinding hwBinding;
    private final double sensitivity;

    public ResetableRelativeSlotBinding(final RelativeHardwareKnob source, final ParamPageSlot target,
                                        final double sensitivity) {
        super(source, source, target);
        this.sensitivity = sensitivity;
    }

    public ResetableRelativeSlotBinding(final RelativeHardwareKnob source, final ParamPageSlot target) {
        this(source, target, 1.0);
    }

    protected RelativeHardwareControlBinding getHardwareBinding() {
        return getTarget().addBindingWithSensitivity(getSource(), sensitivity);
    }

    @Override
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
