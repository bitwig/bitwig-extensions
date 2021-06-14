package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extension.controller.api.HardwareAction;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareActionBinding;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extensions.framework.HardwareBinding;

public class TouchFaderBinding extends
		HardwareBinding<HardwareAction, HardwareActionBindable, com.bitwig.extension.controller.api.HardwareActionBinding> {

	public TouchFaderBinding(final HardwareButton excusiveButtonSource, final HardwareActionBindable target) {
		super(excusiveButtonSource, excusiveButtonSource.releasedAction(), target);
	}

	@Override
	protected HardwareActionBinding addHardwareBinding() {
		return getSource().addBinding(getTarget());
	}

}
