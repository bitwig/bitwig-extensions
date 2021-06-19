package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extension.controller.api.ObjectProxy;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.controllers.mackie.target.DisplayValueTarget;

public class DisplayDoubleValueBinding extends AbstractDisplayValueBinding<SettableRangedValue> {

	private final ValueConverter converter;
	private boolean exists = false;
	private double lastDoubleValue = 0;

	public DisplayDoubleValueBinding(final SettableRangedValue source, final DisplayValueTarget target,
			final ObjectProxy exister, final ValueConverter converter) {
		super(source, target);
		this.converter = converter;
		exister.exists().addValueObserver(v -> {
			exists = v;
			update();
		});
	}

	private void update() {
		if (exists) {
			valueChanged(converter.convert(lastDoubleValue));
		} else {
			valueChanged("");
		}
	}

	@Override
	protected void initListening() {
		getSource().addValueObserver(this::handleValueChange);
	}

	private void handleValueChange(final double newvalue) {
		lastDoubleValue = newvalue;
		if (exists) {
			valueChanged(converter.convert(newvalue));
		} else {
			valueChanged("");
		}
	}
}
