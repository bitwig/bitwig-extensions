package com.bitwig.extensions.controllers.mackie.devices;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.SpecificBitwigDevice;
import com.bitwig.extensions.controllers.mackie.configurations.BrowserConfiguration;
import com.bitwig.extensions.controllers.mackie.configurations.BrowserConfiguration.Type;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.section.InfoSource;

/**
 * Fully Customized control of the Bitwig EQ+ Device Missing Parameters:
 *
 * OUTPUT_GAIN GLOBAL_SHIFT BAND SOLO
 *
 * ADAPTIVE_Q DECIBEL_RANGE
 *
 */
public abstract class SpecificDevice implements DeviceManager {

	protected final SpecificBitwigDevice bitwigDevice;
	protected int pageIndex = 0;

	protected DisplayLayer infoLayer;
	protected InfoSource infoSource;

	protected final DeviceTypeFollower deviceFollower;
	protected final CursorDeviceControl cursorDeviceControl;

	public SpecificDevice(final SpecialDevices type, final CursorDeviceControl cursorDeviceControl,
			final DeviceTypeFollower deviceFollower) {
		this.cursorDeviceControl = cursorDeviceControl;
		final PinnableCursorDevice cursorDevice = cursorDeviceControl.getCursorDevice();
		bitwigDevice = cursorDevice.createSpecificBitwigDevice(type.getUuid());
		this.deviceFollower = deviceFollower;
	}

	@Override
	public void setInfoLayer(final DisplayLayer infoLayer) {
		this.infoLayer = infoLayer;
	}

	@Override
	public void enableInfo(final InfoSource type) {
		infoSource = type;
		if (infoSource == InfoSource.NAV_VERTICAL) {
			infoLayer.setMainText(getDeviceInfo(), "", false);
		} else if (infoSource == InfoSource.NAV_HORIZONTAL) {
			infoLayer.setMainText(getPageInfo(), "", false);
		}
	}

	@Override
	public void disableInfo() {
		infoSource = null;
	}

	@Override
	public InfoSource getInfoSource() {
		return infoSource;
	}

	public abstract String getDeviceInfo();

	public abstract String getPageInfo();

	public abstract String getParamName(final int page, final int index);

	public abstract void triggerUpdate();

	public abstract int getPages();

	public abstract DeviceParameter createDeviceParameter(final int page, final int index);

	public int getCurrentPage() {
		return pageIndex;
	}

	public BooleanValue exists() {
		return deviceFollower.getFocusDevice().exists();
	}

	@Override
	public boolean isSpecificDevicePresent() {
		return deviceFollower.getFocusDevice().exists().get();
	}

	@Override
	public void initiateBrowsing(final BrowserConfiguration browser, final Type type) {
		if (browser.isBrowserActive()) {
			browser.forceClose();
		}
		browser.setBrowsingInitiated(true, type);
		deviceFollower.initiateBrowsing();
	}

	@Override
	public void addBrowsing(final BrowserConfiguration browser, final boolean after) {
		browser.setBrowsingInitiated(true, Type.DEVICE);
		if (after) {
			deviceFollower.addNewDeviceAfter();
		} else {
			deviceFollower.addNewDeviceBefore();
		}
	}

	@Override
	public void setCurrentFollower(final DeviceTypeFollower currentFollower) {
	}

	@Override
	public DeviceTypeFollower getCurrentFollower() {
		return deviceFollower;
	}
}
