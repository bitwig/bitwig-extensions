package com.bitwig.extensions.controllers.mackie.devices;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.SpecificBitwigDevice;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.layer.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.layer.InfoSource;

public class ArpeggiatorDevice implements ControlDevice, DeviceManager {

	private final SpecificBitwigDevice bitwigDevice;
	private final BooleanValue cursorOnDevice;
	private final Device device;

	public ArpeggiatorDevice(final MackieMcuProExtension driver, final DeviceMatcher matcher) {
		bitwigDevice = driver.getCursorDevice().createSpecificBitwigDevice(Devices.ARPEGGIATOR.getUuid());

		final DeviceBank eqPlusDeviceBank = driver.getCursorTrack().createDeviceBank(1);
		eqPlusDeviceBank.setDeviceMatcher(matcher);
		device = eqPlusDeviceBank.getItemAt(0);

		cursorOnDevice = device.createEqualsValue(driver.getCursorDevice());

	}

	@Override
	public BooleanValue getCursorOnDevice() {
		return cursorOnDevice;
	}

	@Override
	public Device getDevice() {
		return device;
	}

	@Override
	public CursorRemoteControlsPage getRemote() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isCanTrackMultiple() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void moveDeviceToLeft() {
		// TODO Auto-generated method stub
	}

	@Override
	public void moveDeviceToRight() {
		// TODO Auto-generated method stub
	}

	@Override
	public void removeDevice() {
		// TODO Auto-generated method stub
	}

	@Override
	public void setInfoLayer(final DisplayLayer infoLayer) {
		// TODO Auto-generated method stub
	}

	@Override
	public void enableInfo(final InfoSource type) {
		// TODO Auto-generated method stub
	}

	@Override
	public void disableInfo() {
		// TODO Auto-generated method stub
	}

	@Override
	public InfoSource getInfoSource() {
		return null;
	}

	@Override
	public int getCurrentPage() {
		return 0;
	}

	@Override
	public int getPages() {
		return 0;
	}

	@Override
	public void navigateNext() {
		// TODO Auto-generated method stub
	}

	@Override
	public void navigatePrevious() {
		// TODO Auto-generated method stub
	}

	@Override
	public BooleanValue exists() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DeviceParameter createDeviceParameter(final int page, final int index) {
		// TODO Auto-generated method stub
		return null;
	}

}
