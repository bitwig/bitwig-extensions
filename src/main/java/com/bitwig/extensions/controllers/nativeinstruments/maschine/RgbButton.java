package com.bitwig.extensions.controllers.nativeinstruments.maschine;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;

public interface RgbButton {

	HardwareButton getHwButton();

	MultiStateHardwareLight getLight();

	int getMidiStatus();

	int getMidiDataNr();

}
