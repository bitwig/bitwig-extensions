package com.bitwig.extensions.controllers.mackie.devices;

import java.util.function.Consumer;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mackie.StringUtil;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;

class DeviceParameter {
	final Parameter parameter;
	private final double sensitivity;
	private final String name;
	private final RingDisplayType ringDisplayType;
	private CustomValueConverter customValueConverter = null;
	private Consumer<Parameter> customResetAction = null;

	public DeviceParameter(final String name, final Parameter parameter, final RingDisplayType ringDisplayType,
			final double sensitivity) {
		super();
		this.parameter = parameter;
		this.name = StringUtil.toDisplayName(name);
		this.ringDisplayType = ringDisplayType;
		this.sensitivity = sensitivity;
	}

	public String getName() {
		return name;
	}

	public double getSensitivity() {
		return sensitivity;
	}

	public RingDisplayType getRingDisplayType() {
		return ringDisplayType;
	}

	public void setCustomResetAction(final Consumer<Parameter> customResetAction) {
		this.customResetAction = customResetAction;
	}

	public CustomValueConverter getCustomValueConverter() {
		return customValueConverter;
	}

	public void setCustomValueConverter(final CustomValueConverter customValueConverter) {
		this.customValueConverter = customValueConverter;
	}

	public void doReset() {
		if (customResetAction != null) {
			customResetAction.accept(parameter);
		} else {
			this.parameter.reset();
		}
	}

	public String getStringValue() {
		if (customValueConverter != null) {
			return customValueConverter.convert(this);
		}
		return parameter.displayedValue().get();
	}

}