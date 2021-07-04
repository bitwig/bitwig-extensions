package com.bitwig.extensions.controllers.mackie.devices;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extensions.controllers.mackie.layer.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.layer.InfoSource;

public interface DeviceManager {

	BooleanValue getCursorOnDevice();

	Device getDevice();

	CursorRemoteControlsPage getRemote();

	boolean isCanTrackMultiple();

	void moveDeviceToLeft();

	void moveDeviceToRight();

	void removeDevice();

	void setInfoLayer(DisplayLayer infoLayer);

	void enableInfo(InfoSource type);

	void disableInfo();

	InfoSource getInfoSource();
}
