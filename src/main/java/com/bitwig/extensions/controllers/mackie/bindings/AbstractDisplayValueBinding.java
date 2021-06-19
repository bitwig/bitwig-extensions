package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extensions.controllers.mackie.target.DisplayValueTarget;
import com.bitwig.extensions.framework.Binding;

public abstract class AbstractDisplayValueBinding<T> extends Binding<T, DisplayValueTarget> {
	private String lastValue = "";

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
			if (isActive()) {
				getTarget().sendValue(value);
			}
		}
	}

	@Override
	protected void deactivate() {
	}

	@Override
	protected void activate() {
		getTarget().sendValue(lastValue);
	}

}
