package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.mackie.StringUtil;
import com.bitwig.extensions.controllers.mackie.target.DisplayNameTarget;
import com.bitwig.extensions.framework.Binding;

public class DisplayNameBinding extends Binding<StringValue, DisplayNameTarget> {
	private String lastValue = "";
	private boolean active = false;

	public DisplayNameBinding(final StringValue source, final DisplayNameTarget target) {
		super(source, target);
		source.addValueObserver(this::valueChanged);
	}

	private void valueChanged(final String value) {
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
		// RemoteConsole.out.println(" DEACTIVATE {}", name);
		active = false;
	}

	@Override
	protected void activate() {
		// RemoteConsole.out.println(" ACTIVATE {}", name);
		active = true;
		getTarget().sendValue(lastValue);
	}

}
