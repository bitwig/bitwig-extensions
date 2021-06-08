package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mackie.target.DisplayValueTarget;
import com.bitwig.extensions.framework.Binding;

public class DisplayValueBinding extends Binding<Parameter, DisplayValueTarget> {
	private String lastValue = "";
	private boolean active = false;

	public DisplayValueBinding(final Parameter source, final DisplayValueTarget target) {
		super(source, target);
		source.displayedValue().addValueObserver(this::valueChanged);
	}

	private void valueChanged(final String value) {
		if (!lastValue.equals(value)) {
			lastValue = value;
			if (active) {
				getTarget().sendValue(value);
			}
		}
	}

	@Override
	protected void deactivate() {
		active = false;
	}

	@Override
	protected void activate() {
		active = true;
		getTarget().sendValue(lastValue);
	}

}
