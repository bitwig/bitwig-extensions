package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extension.controller.api.ObjectProxy;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.targets.RingDisplay;
import com.bitwig.extensions.framework.Binding;

public class RingDisplayExistsBinding extends Binding<ObjectProxy, RingDisplay> {

	private final RingDisplayType type;

	public RingDisplayExistsBinding(final ObjectProxy source, final RingDisplay target, final RingDisplayType type) {
		super(target, source, target);
		this.type = type;
		source.exists().addValueObserver(this::handleExists);
	}

	public void handleExists(final boolean exist) {
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
		getTarget().sendValue(calcCurrentValue(getSource().exists().get()), false);
	}

	private int calcCurrentValue(final boolean exists) {
		final int value = exists ? type.getOffset() + type.getRange() - 1 : type.getOffset();
		return value;
	}

}
