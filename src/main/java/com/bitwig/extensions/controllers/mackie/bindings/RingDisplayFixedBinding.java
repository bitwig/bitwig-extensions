package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extensions.controllers.mackie.display.RingDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.framework.Binding;

public class RingDisplayFixedBinding extends Binding<Integer, RingDisplay> {

	private final RingDisplayType type;

	public RingDisplayFixedBinding(final Integer source, final RingDisplay target, final RingDisplayType type) {
		super(target, source, target);
		this.type = type;
	}

	@Override
	protected void deactivate() {
	}

	@Override
	protected void activate() {
		getTarget().sendValue(type.getOffset() + getSource(), false);
	}

}
