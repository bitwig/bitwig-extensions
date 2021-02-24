package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.framework.Layer;

public class KompleteLayer extends Layer {

	private final KompleteKontrolSMk2Extension driver;

	public KompleteLayer(final KompleteKontrolSMk2Extension driver, final String name) {
		super(driver.getLayers(), name);
		this.driver = driver;
	}

	public KompleteKontrolSMk2Extension getDriver() {
		return driver;
	}

	protected void bindPressed(final ModeButton bt, final Runnable runnable) {
		bindPressed(bt.getHwButton(), runnable);
	}

	void bindPressed(final ModeButton bt, final SettableBooleanValue value) {
		bindPressed(bt.getHwButton(), value);
	}

	void bindIsPressed(final ModeButton bt, final SettableBooleanValue value) {
		bindIsPressed(bt.getHwButton(), value);
	}

	void bindLightState(final ModeButton but, final BooleanValue value) {
		value.addValueObserver(v -> but.getLed().isOn().setValue(v));
	}

}
