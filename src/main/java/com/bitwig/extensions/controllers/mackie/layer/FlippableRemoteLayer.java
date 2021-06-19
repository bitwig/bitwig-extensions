package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extensions.controllers.mackie.devices.DeviceTracker;

/**
 * Flippable Layer that is attached to a device with remote control pages.
 *
 */
public class FlippableRemoteLayer extends FlippableLayer {

	private DeviceTracker device;

	public FlippableRemoteLayer(final ChannelSection section, final String name) {
		super(section, name);
	}

	@Override
	public void navigateLeftRight(final int direction) {
		if (!isActive()) {
			return;
		}
		if (direction < 0) {
			device.selectPreviousParameterPage();
		} else {
			device.selectNextParameterPage();
		}
	}

	@Override
	public void navigateUpDown(final int direction) {
		if (!isActive()) {
			return;
		}
		if (direction < 0) {
			device.selectPreviousDevice();
		} else {
			device.selectNextDevice();
		}
	}

	public void setDevice(final DeviceTracker device) {
		this.device = device;
	}

}
