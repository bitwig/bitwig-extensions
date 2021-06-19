package com.bitwig.extensions.controllers.mackie.devices;

import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;

public class ParameterSetting {
	private final String pname;
	private final double sensitivity;
	private final RingDisplayType ringType;

	public ParameterSetting(final String pname, final double sensitivity, final RingDisplayType ringType) {
		super();
		this.pname = pname;
		this.sensitivity = sensitivity;
		this.ringType = ringType;
	}

	public String getPname() {
		return pname;
	}

	public double getSensitivity() {
		return sensitivity;
	}

	public RingDisplayType getRingType() {
		return ringType;
	}

}
