package com.bitwig.extensions.controllers.akai.apcmk2.layer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.akai.apcmk2.ControlMode;
import com.bitwig.extensions.controllers.akai.apcmk2.ViewControl;
import com.bitwig.extensions.controllers.akai.apcmk2.HardwareElementsApc;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.PostConstruct;

public class SliderLayer extends AbstractControlLayer {

    public SliderLayer(Layers layers) {
        super(layers);
    }

    @PostConstruct
    public void init(HardwareElementsApc hwElements, ViewControl viewControl) {
        trackBank = viewControl.getTrackBank();
        final PinnableCursorDevice cursorDevice = viewControl.getCursorDevice();
        parameterBank = cursorDevice.createCursorRemoteControlsPage(8);

        for (int i = 0; i < 8; i++) {
            final HardwareSlider slider = hwElements.getSlider(i);
            final Track track = trackBank.getItemAt(i);
            final SendBank sendBank = track.sendBank();
            sendBank.itemCount().markInterested();
            sendBank.scrollPosition().markInterested();
            layerMap.get(ControlMode.VOLUME).bind(slider, track.volume());
            layerMap.get(ControlMode.PAN).bind(slider, track.pan());
            layerMap.get(ControlMode.SEND).bind(slider, sendBank.getItemAt(0).value());
            Parameter parameter = parameterBank.getParameter(i);
            layerMap.get(ControlMode.DEVICE).bind(slider, parameter);
        }
    }

}
