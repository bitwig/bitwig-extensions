package com.bitwig.extensions.controllers.mackie.devices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extensions.controllers.mackie.VPotMode;

public class DeviceTypeBank {

	public interface ExistanceChangedListener {
		void changed(VPotMode typ, boolean exists);
	}

	private final Map<VPotMode, DeviceTypeFollower> types = new HashMap<>();
	private final Map<VPotMode, DeviceManager> managers = new HashMap<>();
	private final CursorDeviceControl cursorDeviceControl;
	private final List<ExistanceChangedListener> listeners = new ArrayList<>();

	public DeviceTypeBank(final ControllerHost host, final CursorDeviceControl deviceControl) {
		this.cursorDeviceControl = deviceControl;
		addFollower(host, deviceControl, VPotMode.INSTRUMENT, host.createInstrumentMatcher());
		addFollower(host, deviceControl, VPotMode.PLUGIN, host.createAudioEffectMatcher());
		addFollower(host, deviceControl, VPotMode.MIDI_EFFECT, host.createNoteEffectMatcher());
		addFollower(host, deviceControl, VPotMode.EQ, host.createBitwigDeviceMatcher(SpecialDevices.EQ_PLUS.getUuid()));
	}

	private void addFollower(final ControllerHost host, final CursorDeviceControl deviceControl, final VPotMode mode,
			final DeviceMatcher matcher) {
		final DeviceTypeFollower follower = new DeviceTypeFollower(deviceControl, matcher, mode);
		types.put(mode, follower);
		final Device device = follower.getFocusDevice();
		device.exists().addValueObserver(followExists -> listeners.forEach(l -> l.changed(mode, followExists)));
	}

	public DeviceTypeFollower[] getStandardFollowers() {
		final DeviceTypeFollower[] result = new DeviceTypeFollower[3];
		result[0] = types.get(VPotMode.INSTRUMENT);
		result[1] = types.get(VPotMode.PLUGIN);
		result[2] = types.get(VPotMode.MIDI_EFFECT);
		return result;
	}

	public void addListenter(final ExistanceChangedListener listener) {
		listeners.add(listener);
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
