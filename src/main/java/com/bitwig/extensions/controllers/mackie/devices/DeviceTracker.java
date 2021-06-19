package com.bitwig.extensions.controllers.mackie.devices;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;

public class DeviceTracker {
	private final Device device;
	private final CursorRemoteControlsPage remote;
	private final DeviceBank deviceBank;

	public DeviceTracker(final MackieMcuProExtension driver, final DeviceMatcher matcher) {
		deviceBank = driver.getCursorTrack().createDeviceBank(1);
		deviceBank.setDeviceMatcher(matcher);
		device = deviceBank.getItemAt(0);
		remote = device.createCursorRemoteControlsPage(8);
	}

	public Device getDevice() {
		return device;
	}

	public CursorRemoteControlsPage getRemote() {
		return remote;
	}

	public void selectPreviousDevice() {
		deviceBank.scrollBackwards();
	}

	public void selectNextDevice() {
		deviceBank.scrollForwards();
	}

	public void selectPreviousParameterPage() {
		remote.selectPrevious();
	}

	public void selectNextParameterPage() {
		remote.selectNext();
	}

	public Parameter getParameter(final int i) {
		return remote.getParameter(i);
	}
}
