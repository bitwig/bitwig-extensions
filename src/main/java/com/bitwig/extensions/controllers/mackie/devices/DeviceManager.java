package com.bitwig.extensions.controllers.mackie.devices;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mackie.configurations.BrowserConfiguration;
import com.bitwig.extensions.controllers.mackie.configurations.BrowserConfiguration.Type;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.section.InfoSource;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;

public interface DeviceManager {

	void initiateBrowsing(final BrowserConfiguration browser, Type type);

	/**
	 * Add new device to chain after of before the cursor device.
	 *
	 * @param browser browser configuration
	 * @param after   if true device is after else before
	 */
	void addBrowsing(final BrowserConfiguration browser, boolean after);

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
