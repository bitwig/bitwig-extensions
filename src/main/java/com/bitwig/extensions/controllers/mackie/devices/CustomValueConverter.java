package com.bitwig.extensions.controllers.mackie.devices;

public interface CustomValueConverter {
	String convert(int value);

	default String convert(final DeviceParameter parameter) {
		final int intValue = (int) (parameter.parameter.value().get() * getIntRange());
		return convert(intValue);
	};

	int getIntRange();
}
