package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extensions.controllers.mackie.StringUtil;
import com.bitwig.extensions.controllers.mackie.target.DisplayNameTarget;
import com.bitwig.extensions.framework.Binding;

public abstract class AbstractDisplayNameBinding<T> extends Binding<T, DisplayNameTarget> {
	private String lastValue = "";
	private boolean active = false;

	public AbstractDisplayNameBinding(final T source, final DisplayNameTarget target) {
		super(source, target);
		initListening();
	}

	protected abstract void initListening();

	protected void valueChanged(final String value) {
		final String newValue = StringUtil.toAsciiDisplay(value, 7);
		if (!lastValue.equals(newValue)) {
			lastValue = newValue;
			if (active) {
				getTarget().sendValue(newValue);
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
