package com.bitwig.extensions.controllers.allenheath.xonek3;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.allenheath.xonek3.color.XoneRgbColor;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public class XoneK3GlobalStates {
    private final BooleanValueObject shiftHeld = new BooleanValueObject();
    private final BooleanValueObject layerHeld = new BooleanValueObject();
    private boolean usesLayers = false;
    private int blinkState = 0;
    private final ControllerHost host;
    private final int deviceCount;
    
    public XoneK3GlobalStates(final ControllerHost host, final int deviceCount) {
        this.host = host;
        this.deviceCount = deviceCount;
    }
    
    public void activate() {
        host.scheduleTask(this::handlePing, 100);
    }
    
    public int getDeviceCount() {
        return deviceCount;
    }
    
    private void handlePing() {
        blinkState = (blinkState + 1) % 128;
        host.scheduleTask(this::handlePing, 40);
    }
    
    public BooleanValueObject getShiftHeld() {
        return shiftHeld;
    }
    
    public BooleanValueObject getLayerHeld() {
        return layerHeld;
    }
    
    public void setUsesLayers(final boolean usesLayers) {
        this.usesLayers = usesLayers;
    }
    
    public boolean usesLayers() {
        return usesLayers;
    }
    
    public XoneRgbColor blinkFast(final XoneRgbColor color) {
        if (blinkState % 4 < 2) {
            return color;
        }
        return XoneRgbColor.OFF;
    }
    
    public XoneRgbColor pulse(final XoneRgbColor color) {
        if (blinkState % 16 < 14) {
            return color;
        }
        return XoneRgbColor.OFF;
    }
    
    public XoneRgbColor blinkMid(final XoneRgbColor color) {
        if (blinkState % 8 < 4) {
            return color;
        }
        return XoneRgbColor.OFF;
    }
    
    
}
