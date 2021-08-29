package com.bitwig.extensions.controllers.mackie.devices;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;
import com.bitwig.extensions.remoteconsole.RemoteConsole;

public class ArpeggiatorDevice extends SpecificDevice {

	public ArpeggiatorDevice(final CursorDeviceControl cursorDeviceControl, final DeviceTypeFollower deviceFollower) {
		super(SpecialDevices.ARPEGGIATOR, cursorDeviceControl, deviceFollower);
		// final Just code to final list all parameters final of device
		final PinnableCursorDevice device = cursorDeviceControl.getCursorDevice();
		device.addDirectParameterIdObserver(allp -> {
			for (final String pname : allp) {
				RemoteConsole.out.println("[{}]", pname);
			}
		});
	}

	@Override
	public int getPages() {
		return 0;
	}

	@Override
	public DeviceParameter createDeviceParameter(final int page, final int index) {
		return null;
	}

	@Override
	public Parameter getParameter(final int index) { // TODO Auto-generated method stub
		return null;
	}

	@Override
	public ParameterPage getParameterPage(final int index) { // TODO Auto-generated method stub
		return null;
	}

	@Override
	public void navigateDeviceParameters(final int direction) {
		// TODO Auto-generated method stub
	}

	@Override
	public void handleResetInvoked(final int index, final ModifierValueObject modifier) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getPageCount() {
		return 0;
	}

	@Override
	public String getDeviceInfo() {
		return "Arpeggiator";
	}

	@Override
	public String getPageInfo() {
		return null;
	}

	@Override
	public String getParamName(final int page, final int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void triggerUpdate() {
		// TODO Auto-generated method stub
	}

}
