package com.bitwig.extensions.controllers.allenheath.xonek3.layer;

import java.util.List;

import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extensions.controllers.allenheath.xonek3.DeviceHwElements;
import com.bitwig.extensions.controllers.allenheath.xonek3.TrackSpecControl;
import com.bitwig.extensions.controllers.allenheath.xonek3.ViewControl;
import com.bitwig.extensions.controllers.allenheath.xonek3.XoneHwElements;
import com.bitwig.extensions.controllers.allenheath.xonek3.XoneK3GlobalStates;
import com.bitwig.extensions.controllers.allenheath.xonek3.control.XoneRgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class SingleRemotesControlLayer extends Layer {
    
    private final XoneK3GlobalStates globalStates;
    private final Layer shiftLayer;
    
    public SingleRemotesControlLayer(final Layers layers, final ViewControl viewControl,
        final XoneHwElements hwElements, final XoneK3GlobalStates globalStates) {
        super(layers, "SINGLE_REMOTES");
        this.globalStates = globalStates;
        shiftLayer = new Layer(layers, "SINGLE_REMOTES_SHIFT_LAYER");
        this.globalStates.getShiftHeld().addValueObserver(active -> {
            if (isActive()) {
                shiftLayer.setIsActive(active);
            }
        });
        final int trackCount = globalStates.getDeviceCount() * 4;
        final List<TrackSpecControl> specControls = viewControl.getSpecControls();
        for (int i = 0; i < trackCount; i++) {
            final TrackSpecControl specControl = specControls.get(i);
            bind(i % 4, specControl, hwElements.getDeviceElements(i / 4));
        }
    }
    
    private void bind(final int index, final TrackSpecControl control, final DeviceHwElements hwElements) {
        final CursorRemoteControlsPage remotes = control.getTrackRemotes();
        final List<AbsoluteHardwareControl> knobs = hwElements.getKnobs();
        final List<XoneRgbButton> buttons = hwElements.getKnobButtons();
        
        this.bind(knobs.get(index), remotes.getParameter(0));
        this.bind(knobs.get(4 + index), remotes.getParameter(1));
        this.bind(knobs.get(8 + index), remotes.getParameter(2));
        shiftLayer.bind(knobs.get(index), remotes.getParameter(3));
        shiftLayer.bind(knobs.get(index + 4), remotes.getParameter(4));
        shiftLayer.bind(knobs.get(index + 8), remotes.getParameter(5));
        for (int rowIndex = 0; rowIndex < 3; rowIndex++) {
            bindParamButton(this, rowIndex, buttons.get(index + 4 * rowIndex), remotes.getParameter(rowIndex), control);
            final int shiftOffset = rowIndex + 3;
            bindParamButton(
                shiftLayer, shiftOffset, buttons.get(index + 4 * rowIndex), remotes.getParameter(shiftOffset), control);
        }
    }
    
    private void bindParamButton(final Layer layer, final int index, final XoneRgbButton button,
        final RemoteControl parameter, final TrackSpecControl control) {
        parameter.markInterested();
        parameter.value().markInterested();
        parameter.discreteValueCount().markInterested();
        parameter.exists().markInterested();
        button.bindPressed(layer, () -> RemotesLayer.toggleParameter(parameter));
        button.bindLight(layer, () -> RemotesLayer.getParamState(index, parameter, control));
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        shiftLayer.setIsActive(false);
    }
}
