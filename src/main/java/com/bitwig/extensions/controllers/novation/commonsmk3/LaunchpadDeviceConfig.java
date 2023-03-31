package com.bitwig.extensions.controllers.novation.commonsmk3;

public class LaunchpadDeviceConfig {
    private final int sysExId;
    private final int sliderValueStatus;
    private final int sliderColorStatus;
    private final boolean miniVersion;
    private final String deviceId;

    public LaunchpadDeviceConfig(final String deviceId, final int sysExId, final int sliderValueStatus,
                                 final int sliderColorStatus, final boolean miniVersion) {
        this.deviceId = deviceId;
        this.sysExId = sysExId;
        this.sliderValueStatus = sliderValueStatus;
        this.sliderColorStatus = sliderColorStatus;
        this.miniVersion = miniVersion;
    }

    public int getSysExId() {
        return sysExId;
    }

    public int getSliderValueStatus() {
        return sliderValueStatus;
    }

    public int getSliderColorStatus() {
        return sliderColorStatus;
    }

    public boolean isMiniVersion() {
        return miniVersion;
    }

    public String getDeviceId() {
        return deviceId;
    }
}
