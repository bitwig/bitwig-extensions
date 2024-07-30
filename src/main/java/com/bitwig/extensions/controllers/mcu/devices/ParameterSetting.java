package com.bitwig.extensions.controllers.mcu.devices;


import com.bitwig.extensions.controllers.mcu.control.RingDisplayType;

public class ParameterSetting {
    private final String parameterName;
    private final double sensitivity;
    private final RingDisplayType ringType;

    public ParameterSetting(final String name, final double sensitivity, final RingDisplayType ringType) {
        this.parameterName = name;
        this.sensitivity = sensitivity;
        this.ringType = ringType;
    }

    public String getParameterName() {
        return parameterName;
    }

    public double getSensitivity() {
        return sensitivity;
    }

    public RingDisplayType getRingType() {
        return ringType;
    }

}
