package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.targets.RingDisplay;
import com.bitwig.extensions.framework.Binding;

public class RingDisplayBoolBinding extends Binding<BooleanValue, RingDisplay> {

	private final RingDisplayType type;

	public RingDisplayBoolBinding(final BooleanValue source, final RingDisplay target, final RingDisplayType type) {
		super(target, source, target);
		this.type = type;
		source.addValueObserver(this::handleBooleanValue);
	}

	public void handleBooleanValue(final boolean exist) {
		if (isActive()) {
			valueChange(calcCurrentValue(exist));
		}
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
		getTarget().sendValue(calcCurrentValue(getSource().get()), false);
	}

	private int calcCurrentValue(final boolean exists) {
		final int value = exists ? type.getOffset() + type.getRange() : type.getOffset();
		return value;
	}

}
