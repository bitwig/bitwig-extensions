package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.targets.RingDisplay;
import com.bitwig.extensions.framework.Binding;

public class RingDisplayParameterBinding extends Binding<Parameter, RingDisplay> {

	private final RingDisplayType type;

	public RingDisplayParameterBinding(final Parameter source, final RingDisplay target, final RingDisplayType type) {
		super(target, source, target);
		this.type = type;
		final int vintRange = type.getRange() + 1;
		source.value().addValueObserver(vintRange, v -> {
			valueChange(type.getOffset() + v);
		});
		source.exists().addValueObserver(this::handleExists);
		source.name().markInterested();
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
		final int value = exists ? type.getOffset() + (int) (getSource().value().get() * type.getRange()) : 0;
		return value;
	}

}
