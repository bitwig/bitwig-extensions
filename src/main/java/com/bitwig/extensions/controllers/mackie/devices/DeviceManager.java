package com.bitwig.extensions.controllers.mackie.devices;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.layer.BrowserConfiguration;
import com.bitwig.extensions.controllers.mackie.layer.BrowserConfiguration.Type;
import com.bitwig.extensions.controllers.mackie.layer.InfoSource;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;

public interface DeviceManager {

	void initiateBrowsing(final BrowserConfiguration browser, Type type);

	void setInfoLayer(DisplayLayer infoLayer);

	void enableInfo(InfoSource type);

	void disableInfo();

	InfoSource getInfoSource();

	Parameter getParameter(int index);

	ParameterPage getParameterPage(int index);

	void navigateDeviceParameters(final int direction);

	void handleResetInvoked(final int index, final ModifierValueObject modifier);

	void setCurrentFollower(final DeviceTypeFollower currentFollower);

	DeviceTypeFollower getCurrentFollower();

	boolean isSpecificDevicePresent();

	int getPageCount();

}
