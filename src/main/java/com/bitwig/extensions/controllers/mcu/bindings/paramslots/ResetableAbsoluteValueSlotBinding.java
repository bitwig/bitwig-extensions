package com.bitwig.extensions.controllers.mcu.bindings.paramslots;

import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extension.controller.api.AbsoluteHardwareControlBinding;
import com.bitwig.extension.controller.api.HardwareBinding;
import com.bitwig.extensions.controllers.mcu.bindings.ResetableBinding;
import com.bitwig.extensions.controllers.mcu.devices.ParamPageSlot;
import com.bitwig.extensions.framework.Binding;

public class ResetableAbsoluteValueSlotBinding extends Binding<AbsoluteHardwareControl, ParamPageSlot> implements ResetableBinding {

    HardwareBinding hwBinding;

    public ResetableAbsoluteValueSlotBinding(final AbsoluteHardwareControl source, final ParamPageSlot target) {
        super(source, source, target);
    }

    protected AbsoluteHardwareControlBinding getHardwareBinding() {
        return getTarget().addBinding(getSource());
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
