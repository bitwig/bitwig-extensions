package com.bitwig.extensions.controllers.mackie;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.OnOffHardwareLight;

public class ModeButton {
	private final HardwareButton hwButton;
	private final OnOffHardwareLight led;

	public ModeButton(final MackieMcuProExtension driver, final String name, final NoteOnAssignment assingment) {
		hwButton = driver.getSurface().createHardwareButton(name);
		assingment.holdActionAssign(driver.getMidiIn(), hwButton);
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
