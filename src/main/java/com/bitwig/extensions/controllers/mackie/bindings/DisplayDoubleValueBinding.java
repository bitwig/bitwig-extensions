package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.controllers.mackie.target.DisplayValueTarget;

public class DisplayDoubleValueBinding extends AbstractDisplayValueBinding<SettableRangedValue> {

	private final ValueConverter converter;

	public DisplayDoubleValueBinding(final SettableRangedValue source, final DisplayValueTarget target,
			final ValueConverter converter) {
		super(source, target);
		this.converter = converter;
	}

	@Override
	protected void initListening() {
		getSource().addValueObserver(this::handleValueChange);
	}

	private void handleValueChange(final double newvalue) {
		valueChanged(converter.convert(newvalue));
	}
}
