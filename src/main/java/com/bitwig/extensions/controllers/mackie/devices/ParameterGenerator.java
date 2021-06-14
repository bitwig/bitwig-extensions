package com.bitwig.extensions.controllers.mackie.devices;

import com.bitwig.extension.controller.api.Parameter;

public interface ParameterGenerator {
	int getPages();

	String getParamName(int page, int index);

	DeviceParameter createDeviceParameter(String pname, Parameter param, int page, int index);

}
