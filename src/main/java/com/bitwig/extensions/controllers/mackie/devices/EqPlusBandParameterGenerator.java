package com.bitwig.extensions.controllers.mackie.devices;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;

public class EqPlusBandParameterGenerator implements ParameterGenerator {
	private final static String[] PNAMES = { "TYPE", "FREQ", "GAIN", "Q" };
	private final static RingDisplayType[] RING_TYPES = { RingDisplayType.FILL_LR, RingDisplayType.SINGLE,
			RingDisplayType.FILL_LR, RingDisplayType.FILL_LR };
	private final static double[] SENSITIVITIES = { 2, 0.25, 0.25, 0.25 };

	@Override
	public String getParamName(final int page, final int index) {
		return PNAMES[index % 4] + Integer.toString(1 + index / 4 + page * 2);
	}

	@Override
	public int getPages() {
		return 4;
	}

	@Override
	public DeviceParameter createDeviceParameter(final String pname, final Parameter param, final int page,
			final int index) {
		return new DeviceParameter(pname, param, RING_TYPES[index % 4], SENSITIVITIES[index % 4]);
	}
}
