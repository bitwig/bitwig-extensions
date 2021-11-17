package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.controllers.mackie.display.RingDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.framework.Binding;

public class RingDisplayRangedValueBinding extends Binding<SettableRangedValue, RingDisplay> {

	private final RingDisplayType type;

	public RingDisplayRangedValueBinding(final SettableRangedValue source, final RingDisplay target,
			final RingDisplayType type) {
		super(target, source, target);
		this.type = type;
		final int vintRange = type.getRange() + 1;
		source.addValueObserver(vintRange, v -> {
			valueChange(type.getOffset() + v);
		});
	}

	private void valueChange(final int value) {
		if (isActive()) {
			getTarget().sendValue(value, false);
		}
	}

	@Override
	protected void deactivate() {
	}

	@Override
	protected void activate() {
		getTarget().sendValue(calcCurrentValue(), false);
	}

	private int calcCurrentValue() {
		final int value = type.getOffset() + (int) (getSource().get() * type.getRange());
		return value;
	}

}
