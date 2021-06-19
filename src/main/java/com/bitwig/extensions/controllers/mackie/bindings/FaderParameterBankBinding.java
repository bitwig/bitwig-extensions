package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extensions.controllers.mackie.devices.ParameterPage;
import com.bitwig.extensions.controllers.mackie.target.MotorFader;
import com.bitwig.extensions.framework.Binding;

public class FaderParameterBankBinding extends Binding<ParameterPage, MotorFader> {

	public FaderParameterBankBinding(final ParameterPage source, final MotorFader target) {
		super(target, source, target);
		source.addDoubleValueObserver(this::valueChange);
	}

	private void valueChange(final double value) {
		if (isActive()) {
			getTarget().sendValue(value);
		}
	}

	@Override
	protected void deactivate() {
	}

	@Override
	protected void activate() {
		getTarget().sendValue(getSource().getParamValue());
	}

	public void update() {
		if (isActive()) {
			getTarget().sendValue(getSource().getParamValue());
		}
	}

}
