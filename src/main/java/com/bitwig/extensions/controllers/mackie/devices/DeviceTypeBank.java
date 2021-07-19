package com.bitwig.extensions.controllers.mackie.devices;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.mackie.VPotMode;

public class DeviceTypeBank {
	private final Map<VPotMode, DeviceTypeFollower> types = new HashMap<>();
	private final Map<VPotMode, DeviceManager> managers = new HashMap<>();
	private final CursorDeviceControl cursorDeviceControl;

	public DeviceTypeBank(final ControllerHost host, final CursorDeviceControl deviceControl) {
		this.cursorDeviceControl = deviceControl;
		types.put(VPotMode.INSTRUMENT,
				new DeviceTypeFollower(deviceControl, host.createInstrumentMatcher(), VPotMode.INSTRUMENT));
		types.put(VPotMode.PLUGIN,
				new DeviceTypeFollower(deviceControl, host.createAudioEffectMatcher(), VPotMode.PLUGIN));
		types.put(VPotMode.MIDI_EFFECT,
				new DeviceTypeFollower(deviceControl, host.createNoteEffectMatcher(), VPotMode.MIDI_EFFECT));
		types.put(VPotMode.EQ, new DeviceTypeFollower(deviceControl,
				host.createBitwigDeviceMatcher(SpecialDevices.EQ_PLUS.getUuid()), VPotMode.EQ));
	}

	public DeviceTypeFollower[] getStandardFollowers() {
		final DeviceTypeFollower[] result = new DeviceTypeFollower[3];
		result[0] = types.get(VPotMode.INSTRUMENT);
		result[1] = types.get(VPotMode.PLUGIN);
		result[2] = types.get(VPotMode.MIDI_EFFECT);
		return result;
	}

	public DeviceManager getDeviceManager(final VPotMode mode) {
		DeviceManager manager = managers.get(mode);
		if (manager == null) {
			switch (mode) {
			case EQ:
				manager = new EqDevice(cursorDeviceControl, types.get(mode));
				managers.put(VPotMode.EQ, manager);
				break;
			default:
				manager = new CursorDeviceTracker(cursorDeviceControl, types.get(VPotMode.PLUGIN));
				managers.put(VPotMode.INSTRUMENT, manager);
				managers.put(VPotMode.MIDI_EFFECT, manager);
				managers.put(VPotMode.PLUGIN, manager);
				break;
			}
		}
		return manager;
	}

	public DeviceTypeFollower getFollower(final VPotMode mode) {
		return types.get(mode);
	}
}
