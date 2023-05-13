package com.bitwig.extensions.controllers.akai.apcmk2.layer;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.akai.apcmk2.ControlMode;
import com.bitwig.extensions.controllers.akai.apcmk2.ViewControl;
import com.bitwig.extensions.controllers.akai.apcmk2.control.Encoder;
import com.bitwig.extensions.controllers.akai.apcmk2.control.HardwareElementsApc;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.PostConstruct;

public class EncoderLayer extends AbstractControlLayer {
    
    public EncoderLayer(Layers layers) {
        super(layers);
    }
    
    @PostConstruct
    public void init(HardwareElementsApc hwElements, ViewControl viewControl) {
        trackBank = viewControl.getTrackBank();
        final PinnableCursorDevice cursorDevice = viewControl.getCursorDevice();
        parameterBank = cursorDevice.createCursorRemoteControlsPage(8);
        
        for (int i = 0; i < 8; i++) {
            final Encoder encoder = hwElements.getEncoder(i);
            final Track track = trackBank.getItemAt(i);
            final SendBank sendBank = track.sendBank();
            sendBank.itemCount().markInterested();
            sendBank.scrollPosition().markInterested();
            encoder.bindParameter(layerMap.get(ControlMode.VOLUME), track.volume(), 1);
            encoder.bindParameter(layerMap.get(ControlMode.PAN), track.pan(), 1);
            encoder.bind(layerMap.get(ControlMode.SEND), sendBank.getItemAt(0).value(), 1);
            Parameter parameter = parameterBank.getParameter(i);
            encoder.bindParameter(layerMap.get(ControlMode.DEVICE), parameter, 1);
        }
    }
    
}
