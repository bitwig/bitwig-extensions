package com.bitwig.extensions.controllers.mackie.devices;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.SpecificBitwigDevice;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;

public class EqDevice {

	private final SpecificBitwigDevice bitwigDevice;
	private final Device device;
	private final List<ParameterPage> eqBands = new ArrayList<>();

	public EqDevice(final MackieMcuProExtension driver) {
		bitwigDevice = driver.getCursorDevice().createSpecificBitwigDevice(Devices.EQ_PLUS.getUuid());
		final DeviceMatcher eq5Matcher = driver.getHost().createBitwigDeviceMatcher(Devices.EQ_PLUS.getUuid());

		final DeviceBank eqPlusDeviceBank = driver.getCursorTrack().createDeviceBank(1);
		eqPlusDeviceBank.setDeviceMatcher(eq5Matcher);
		device = eqPlusDeviceBank.getItemAt(0);

		final ParameterGenerator paramGen = new EqPlusBandParameterGenerator();

		for (int i = 0; i < 8; i++) {
			eqBands.add(new ParameterPage(i, bitwigDevice, paramGen));
		}
// Just code to list all parameters of device
//		device.addDirectParameterIdObserver(allp -> {
//			for (final String pname : allp) {
//				RemoteConsole.out.println("[{}]", pname);
//			}
//		});
		device.exists().addValueObserver(v -> {
		});
	}

	public Device getDevice() {
		return device;
	}

	public List<ParameterPage> getEqBands() {
		return eqBands;
	}

	public boolean exists() {
		return device.exists().get();
	}

	public void triggerUpdate() {
		eqBands.forEach(ParameterPage::triggerUpdate);
	}

}
