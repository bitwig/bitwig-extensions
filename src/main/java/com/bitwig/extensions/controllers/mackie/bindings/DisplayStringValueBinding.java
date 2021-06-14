package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.mackie.target.DisplayValueTarget;

public class DisplayStringValueBinding extends AbstractDisplayValueBinding<StringValue> {

	public DisplayStringValueBinding(final StringValue source, final DisplayValueTarget target) {
		super(source, target);
	}

	@Override
	protected void initListening() {
		getSource().addValueObserver(this::valueChanged);
	}

}
