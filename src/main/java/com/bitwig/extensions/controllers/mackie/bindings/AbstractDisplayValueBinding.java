package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extensions.controllers.mackie.target.DisplayValueTarget;
import com.bitwig.extensions.framework.Binding;

public abstract class AbstractDisplayValueBinding<T> extends Binding<T, DisplayValueTarget> {
	private String lastValue = "";
	private boolean active = false;

	public AbstractDisplayValueBinding(final T source, final DisplayValueTarget target) {
		super(source, target);
		initListening();
	}

	protected void setLastValue(final String lastValue) {
		this.lastValue = lastValue;
	}

	protected abstract void initListening();

	protected void valueChanged(final String value) {
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
