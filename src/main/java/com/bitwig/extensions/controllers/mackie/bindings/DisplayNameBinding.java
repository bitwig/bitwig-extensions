package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.mackie.targets.DisplayNameTarget;

public class DisplayNameBinding extends AbstractDisplayNameBinding<StringValue> {

	public DisplayNameBinding(final StringValue source, final DisplayNameTarget target) {
		super(source, target);
	}

	@Override
	protected void initListening() {
		getSource().addValueObserver(this::valueChanged);
	}

}
