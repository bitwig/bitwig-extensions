package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.targets.RingDisplay;
import com.bitwig.extensions.framework.Binding;

public class RingDisplayBinding extends Binding<Parameter, RingDisplay> {

	private int lastValue = 0;

	public RingDisplayBinding(final Parameter source, final RingDisplay target, final RingDisplayType type) {
		super(target, source, target);
		source.value().addValueObserver(11, v -> {
			valueChange(type.getOffset() + v);
		});
		lastValue = type.getOffset() + (int) (source.value().get() * 10);
		source.exists().addValueObserver(this::handleExists);
	}

	public void handleExists(final boolean exist) {
		if (isActive() && !exist) {
			valueChange(0);
		}
	}

	private void valueChange(final int value) {
		lastValue = value;
		if (isActive()) {
			getTarget().sendValue(value, false);
		}
	}

	@Override
	protected void deactivate() {
	}

	@Override
	protected void activate() {
		getTarget().sendValue(lastValue, false);
	}

}
