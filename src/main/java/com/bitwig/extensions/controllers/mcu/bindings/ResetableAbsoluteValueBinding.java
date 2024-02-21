package com.bitwig.extensions.controllers.mcu.bindings;

import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extension.controller.api.AbsoluteHardwareControlBinding;
import com.bitwig.extension.controller.api.HardwareBinding;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.framework.Binding;

public class ResetableAbsoluteValueBinding extends Binding<AbsoluteHardwareControl, SettableRangedValue> implements ResetableBinding {

    HardwareBinding hwBinding;

    public ResetableAbsoluteValueBinding(final AbsoluteHardwareControl source, final SettableRangedValue target) {
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
