package com.bitwig.extensions.controllers.mcu.bindings;

import com.bitwig.extension.controller.api.HardwareAction;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareActionBinding;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extensions.framework.HardwareBinding;

public class ButtonBinding extends HardwareBinding<HardwareAction, HardwareActionBindable, HardwareActionBinding> {

    public ButtonBinding(final HardwareButton exclusiveButtonSource, final HardwareActionBindable target) {
        this(exclusiveButtonSource, exclusiveButtonSource.pressedAction(), target);
    }

    public ButtonBinding(final HardwareButton exclusiveButtonSource, final HardwareAction action,
                         final HardwareActionBindable target) {
        super(exclusiveButtonSource, action, target);
    }

    @Override
    protected HardwareActionBinding addHardwareBinding() {
        return getSource().addBinding(getTarget());
    }

}
