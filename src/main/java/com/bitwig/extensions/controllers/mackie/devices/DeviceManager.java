package com.bitwig.extensions.controllers.mackie.devices;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.Device;

public interface DeviceManager {

	BooleanValue getCursorOnDevice();

	Device getDevice();

	CursorRemoteControlsPage getRemote();
}
