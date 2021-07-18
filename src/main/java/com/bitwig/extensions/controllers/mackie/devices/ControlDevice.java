package com.bitwig.extensions.controllers.mackie.devices;

import com.bitwig.extension.controller.api.BooleanValue;

public interface ControlDevice {

	int getCurrentPage();

	int getPages();

	BooleanValue exists();

	DeviceParameter createDeviceParameter(int page, int index);

}
