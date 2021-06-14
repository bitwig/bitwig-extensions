package com.bitwig.extensions.controllers.mackie.devices;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mackie.RingDisplayType;

class DeviceParameter {
	final Parameter parameter;
	final String name;
	final double sensitivity;
	final RingDisplayType ringDisplayType;

	public DeviceParameter(final String name, final Parameter parameter, final RingDisplayType ringDisplayType,
			final double sensitivity) {
		super();
		this.parameter = parameter;
		this.name = name;
		this.ringDisplayType = ringDisplayType;
		this.sensitivity = sensitivity;
	}

}