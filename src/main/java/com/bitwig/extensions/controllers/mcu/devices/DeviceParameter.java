package com.bitwig.extensions.controllers.mcu.devices;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mcu.StringUtil;
import com.bitwig.extensions.controllers.mcu.control.RingDisplayType;

import java.util.function.Consumer;

class DeviceParameter {
    private final Parameter parameter;
    private final double sensitivity;
    private final String name;
    private final RingDisplayType ringDisplayType;
    private CustomValueConverter customValueConverter = null;
    private Consumer<Parameter> customResetAction = null;

    public DeviceParameter(final String name, final Parameter parameter, final RingDisplayType ringDisplayType,
                           final double sensitivity) {
        super();
        this.parameter = parameter;
        this.parameter.displayedValue().markInterested();
        this.parameter.value().markInterested();
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

    public Parameter getParameter() {
        return parameter;
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
            return customValueConverter.convert(parameter.value().get());
        }
        return parameter.displayedValue().get();
    }

    public int getRingValue() {
        return (int) Math.round(parameter.get() * 10);
    }
}