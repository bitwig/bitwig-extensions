package com.bitwig.extensions.controllers.mackie.devices;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;

public class DeviceTracker implements DeviceManager {
	private final Device device;
	private final CursorRemoteControlsPage remote;
	private final DeviceBank deviceBank;
	private final BooleanValue cursorOnDevice;

	public DeviceTracker(final MackieMcuProExtension driver, final DeviceMatcher matcher) {
		deviceBank = driver.getCursorTrack().createDeviceBank(1);

		deviceBank.setDeviceMatcher(matcher);
		device = deviceBank.getItemAt(0);
		cursorOnDevice = device.createEqualsValue(driver.getCursorDevice());
		remote = device.createCursorRemoteControlsPage(8);
		device.name().markInterested();
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

	@Override
	public Device getDevice() {
		return device;
	}

	@Override
	public BooleanValue getCursorOnDevice() {
		return cursorOnDevice;
	}

	@Override
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
