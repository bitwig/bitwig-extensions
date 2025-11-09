package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.layer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlXlHwElements;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchViewControl;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.SegmentDisplayBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.DisplayControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public abstract class AbstractDawControlLayer extends Layer {
    
    protected final CursorTrack cursorTrack;
    protected final Remotes deviceRemotes;
    protected final PinnableCursorDevice cursorDevice;
    protected final BooleanValueObject shiftState;
    protected final TransportHandler transportHandler;
    protected final DisplayControl displayControl;
    protected final LaunchViewControl viewControl;
    
    protected final SegmentDisplayBinding deviceDisplayBinding;
    protected SegmentDisplayBinding selectTrackBinding;
    
    protected BasicStringValue deviceName = new BasicStringValue("");
    
    public AbstractDawControlLayer(final Layers layers, final LaunchControlXlHwElements hwElements,
        final LaunchViewControl viewControl, final DisplayControl displayControl,
        final TransportHandler transportHandler, final ControllerHost host) {
        super(layers, "DAW_CONTROL");
        this.transportHandler = transportHandler;
        this.viewControl = viewControl;
        this.displayControl = displayControl;
        shiftState = hwElements.getShiftState();
        this.cursorTrack = viewControl.getCursorTrack();
        
        cursorDevice = viewControl.getCursorDevice();
        cursorDevice.name().addValueObserver(deviceName::set);
        cursorDevice.hasPrevious().markInterested();
        cursorDevice.hasNext().markInterested();
        deviceRemotes = new Remotes(cursorDevice);
        
        deviceDisplayBinding = new SegmentDisplayBinding(
            this.deviceName, deviceRemotes.getDevicePageName(),
            displayControl.getFixedDisplay());
        this.addBinding(deviceDisplayBinding);
    }
    
    protected void navigateTracks(final int inc) {
        if (inc > 0) {
            cursorTrack.selectNext();
        } else {
            cursorTrack.selectPrevious();
        }
        selectTrackBinding.blockUpdate();
        deviceDisplayBinding.blockUpdate();
    }
    
    
}
