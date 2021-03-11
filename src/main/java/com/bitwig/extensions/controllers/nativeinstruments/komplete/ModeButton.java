package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.OnOffHardwareLight;

public class ModeButton {
	private final HardwareButton hwButton;
	private final OnOffHardwareLight led;

	public ModeButton(final KompleteKontrolExtension driver, final String name, final CcAssignment assingment) {
		hwButton = driver.getSurface().createHardwareButton(name);
		hwButton.pressedAction().setActionMatcher(assingment.createActionMatcher(driver.getMidiIn(), 1));
		hwButton.releasedAction().setActionMatcher(assingment.createActionMatcher(driver.getMidiIn(), 0));
		led = driver.getSurface().createOnOffHardwareLight(name + "_LED");
		hwButton.setBackgroundLight(led);
		led.onUpdateHardware(() -> driver.sendLedUpdate(assingment, led.isOn().currentValue() ? 1 : 0));
	}

	public ModeButton bindLightToPressed() {
		hwButton.isPressed().addValueObserver(v -> led.isOn().setValue(v));
		return this;
	}

	public HardwareButton getHwButton() {
		return hwButton;
	}

	public OnOffHardwareLight getLed() {
		return led;
	}

}
