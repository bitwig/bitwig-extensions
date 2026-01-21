package com.bitwig.extensions.controllers.allenheath.xonek3;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extensions.controllers.allenheath.xonek3.control.XoneRgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Component;

@Component
public class XoneHwElements {
    
    private final List<DeviceHwElements> hwElements = new ArrayList<>();
    private final int deviceCount;
    
    public XoneHwElements(final HardwareSurface surface, final XoneMidiProcessor midiProcessor,
        final XoneK3GlobalStates globalStates) {
        deviceCount = globalStates.getDeviceCount();
        for (int i = 0; i < globalStates.getDeviceCount(); i++) {
            hwElements.add(new DeviceHwElements(i, surface, midiProcessor.getMidiDevice(i), globalStates));
        }
    }
    
    public DeviceHwElements getDeviceElements(final int index) {
        return hwElements.get(index);
    }
    
    public XoneRgbButton getGridButton(final int trackIndex, final int sceneIndex) {
        final int deviceIndex = trackIndex / 4;
        return hwElements.get(deviceIndex).getGridButtons().get(sceneIndex * 4 + (trackIndex % 4));
    }
    
    public void disableKnobButtonSectionRightSide(final Layer layer) {
        if (deviceCount == 1) {
            return;
        }
        for (int i = 1; i < deviceCount; i++) {
            hwElements.get(i).disableKnobButtonSection(layer);
        }
        
    }
    
}
