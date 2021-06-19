package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extension.controller.api.HardwareAction;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareActionBinding;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extensions.framework.HardwareBinding;

public class ButtonBinding extends
		HardwareBinding<HardwareAction, HardwareActionBindable, com.bitwig.extension.controller.api.HardwareActionBinding> {

	public ButtonBinding(final HardwareButton excusiveButtonSource, final HardwareActionBindable target) {
		super(excusiveButtonSource, excusiveButtonSource.pressedAction(), target);
	}

	@Override
	protected HardwareActionBinding addHardwareBinding() {
		return getSource().addBinding(getTarget());
	}

}
