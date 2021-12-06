package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extensions.controllers.mackie.display.RingDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.value.EnumValueSetting;
import com.bitwig.extensions.framework.Binding;

public class RingDisplayEnumValueBinding extends Binding<SettableEnumValue, RingDisplay> {

	private final RingDisplayType type;
	private final EnumValueSetting values;

	public RingDisplayEnumValueBinding(final SettableEnumValue source, final RingDisplay target,
			final RingDisplayType type, final EnumValueSetting values) {
		super(target, source, target);
		this.type = type;
		this.values = values;
		source.addValueObserver(v -> {
			valueChange(v);
		});
	}

	private void valueChange(final String value) {
		if (isActive()) {
			getTarget().sendValue(values.toIndexed(value) + type.getOffset(), false);
		}
	}

	@Override
	protected void deactivate() {
	}

	@Override
	protected void activate() {
		getTarget().sendValue(calcCurrentValue(), false);
	}

	private int calcCurrentValue() {
		final int value = values.toIndexed(getSource().get()) + type.getOffset();
		return value;
	}

}
