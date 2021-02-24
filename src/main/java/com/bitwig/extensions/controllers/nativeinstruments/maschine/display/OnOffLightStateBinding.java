package com.bitwig.extensions.controllers.nativeinstruments.maschine.display;

import java.util.function.BooleanSupplier;

import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extensions.framework.Binding;

public class OnOffLightStateBinding extends Binding<BooleanSupplier, OnOffHardwareLight> {

	public OnOffLightStateBinding(final BooleanSupplier source, final OnOffHardwareLight target) {
		super(target, source, target);
	}

	@Override
	protected void deactivate() {
		getTarget().isOn().setValueSupplier(null);
	}

	@Override
	protected void activate() {
		getTarget().isOn().setValueSupplier(getSource());
	}

}
