package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extensions.controllers.mackie.devices.ParameterPage;
import com.bitwig.extensions.controllers.mackie.target.RingDisplay;
import com.bitwig.extensions.framework.Binding;

/**
 * Special binding for the encoder ring display, that also responds to being
 * enabled or not.
 *
 */
public class RingParameterBankDisplayBinding extends Binding<ParameterPage, RingDisplay> {

	private int lastValue = 0;
	private int lastEnableValue = 0;

	public RingParameterBankDisplayBinding(final ParameterPage source, final RingDisplay target) {
		super(target, source, target);
		source.addIntValueObserver(v -> {
			valueChange(source.getRingDisplayType().getOffset() + v);
		});
		lastValue = source.getRingDisplayType().getOffset() + source.getIntValue();
	}

	public void handleEnabled(final int enableValue) {
		lastEnableValue = enableValue;
		if (isActive()) {
			update();
		}
	}

	public void update() {
		if (isActive()) {
			lastValue = getSource().getIntValue() + getSource().getRingDisplayType().getOffset();
			getTarget().sendValue(lastValue * lastEnableValue, false);
		}
	}

	private void valueChange(final int value) {
		lastValue = value;
		if (isActive()) {
			getTarget().sendValue(value * lastEnableValue, false);
		}
	}

	@Override
	protected void deactivate() {
	}

	@Override
	protected void activate() {
		lastValue = getSource().getRingDisplayType().getOffset() + getSource().getIntValue();
		getTarget().sendValue(lastValue * lastEnableValue, false);
	}

}
