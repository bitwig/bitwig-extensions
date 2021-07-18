package com.bitwig.extensions.controllers.mackie.devices;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extensions.controllers.mackie.VPotMode;

public class DeviceTypeFollower {

	private final DeviceBank deviceBank;
	private final VPotMode potMode;
	private final Device focusDevice;
	private final CursorDeviceControl cursorDeviceControl;
	private final BooleanValue cursorOnDevice;
	private int chainIndex = -1;

	public DeviceTypeFollower(final CursorDeviceControl cursorDeviceControl, final DeviceMatcher matcher,
			final VPotMode potMode) {
		this.cursorDeviceControl = cursorDeviceControl;

		final CursorTrack cursorTrack = cursorDeviceControl.getCursorTrack();
		deviceBank = cursorTrack.createDeviceBank(1);
		this.potMode = potMode;
		deviceBank.setDeviceMatcher(matcher);

		focusDevice = deviceBank.getItemAt(0);
		focusDevice.exists().markInterested();
		final PinnableCursorDevice cursorDevice = cursorDeviceControl.getCursorDevice();
		cursorOnDevice = focusDevice.createEqualsValue(cursorDevice);
		cursorOnDevice.addValueObserver(equalsCursor -> {
			if (equalsCursor) {
				chainIndex = cursorDevice.position().get();
			}
		});
		cursorDevice.position().addValueObserver(position -> {
			if (cursorOnDevice.get()) {
				chainIndex = position;
			}
		});
	}

	public Device getFocusDevice() {
		return focusDevice;
	}

	public VPotMode getPotMode() {
		return potMode;
	}

	public void initiateBrowsing() {
		if (focusDevice.exists().get()) {
			focusDevice.replaceDeviceInsertionPoint().browse();
		} else {
			deviceBank.browseToInsertDevice(0);
		}
	}

	public void ensurePosition() {
		final int currentDiveChainLocation = cursorDeviceControl.getCursorDevice().position().get();
		if (!cursorOnDevice.get()) {
			deviceBank.scrollBy(currentDiveChainLocation - chainIndex);
		}
	}

}
