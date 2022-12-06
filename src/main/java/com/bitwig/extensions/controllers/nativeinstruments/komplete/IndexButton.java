package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.api.HardwareButton;

public class IndexButton {
	private final HardwareButton hwButton;

	public IndexButton(final KompleteKontrolExtension driver, final int index, final String name, final int ccNr) {
		hwButton = driver.getSurface().createHardwareButton(name + "_" + index);
		hwButton.pressedAction().setActionMatcher(driver.getMidiIn().createCCActionMatcher(0xF, ccNr, index));
	}

	public HardwareButton getHwButton() {
		return hwButton;
	}

}
