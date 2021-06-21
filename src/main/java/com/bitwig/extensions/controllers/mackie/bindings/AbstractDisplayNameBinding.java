package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extensions.controllers.mackie.StringUtil;
import com.bitwig.extensions.controllers.mackie.targets.DisplayNameTarget;
import com.bitwig.extensions.framework.Binding;

public abstract class AbstractDisplayNameBinding<T> extends Binding<T, DisplayNameTarget> {
	private String lastValue = "";

	public AbstractDisplayNameBinding(final T source, final DisplayNameTarget target) {
		super(source, target);
		initListening();
	}

	protected abstract void initListening();

	protected void valueChanged(final String value) {
		final String newValue = StringUtil.toAsciiDisplay(value, 7);
		if (!lastValue.equals(newValue)) {
			lastValue = newValue;
			if (isActive()) {
				getTarget().sendValue(newValue);
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
