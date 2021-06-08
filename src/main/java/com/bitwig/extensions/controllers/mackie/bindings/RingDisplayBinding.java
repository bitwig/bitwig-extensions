package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mackie.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.target.RingDisplay;
import com.bitwig.extensions.framework.Binding;

public class RingDisplayBinding extends Binding<Parameter, RingDisplay> {

	private int lastValue = -1;

	public RingDisplayBinding(final Parameter source, final RingDisplay target, final RingDisplayType type) {
		super(target, source, target);
		source.value().addValueObserver(11, v -> {
			valueChange(type.getOffset() + v);
		});
		source.exists().addValueObserver(exist -> {
			if (!exist) {
				valueChange(0);
			}
		});
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
		if (lastValue != -1) {
			getTarget().sendValue(lastValue, false);
		}
	}

}
