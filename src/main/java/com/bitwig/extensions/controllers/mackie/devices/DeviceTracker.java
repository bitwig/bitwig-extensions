package com.bitwig.extensions.controllers.mackie.devices;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;

public class DeviceTracker {
	private final Device device;
	private final CursorRemoteControlsPage remote;
	private final DeviceBank deviceBank;

	public DeviceTracker(final MackieMcuProExtension driver, final DeviceMatcher matcher) {
		deviceBank = driver.getCursorTrack().createDeviceBank(1);
		deviceBank.setDeviceMatcher(matcher);
		device = deviceBank.getItemAt(0);
		remote = device.createCursorRemoteControlsPage(8);
		device.exists().markInterested();
		device.isEnabled().markInterested();
	}

	public void handleReset(final int index, final Parameter parameter, final ModifierValueObject modifier) {
		if (modifier.isOptionSet()) {
			device.isEnabled().toggle();
		} else if (modifier.isControlSet()) {
			parameter.restoreAutomationControl();
		} else {
			parameter.reset();
		}
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

	public boolean exists() {
		return device.exists().get();
	}
}