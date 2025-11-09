package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.layer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.CcConstValues;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlXlHwElements;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchViewControl;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.SegmentDisplayBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchButton;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.DisplayControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.LayoutType;

@Component(tag = "XLModel")
public class XlDawControlLayer extends AbstractDawControlLayer {
    
    private final Layer specLauncherLayer;
    
    public XlDawControlLayer(final Layers layers, final ControllerHost host, final LaunchControlXlHwElements hwElements,
        final LaunchViewControl viewControl, final DisplayControl displayControl,
        final TransportHandler transportHandler) {
        super(layers, hwElements, viewControl, displayControl, transportHandler, host);
        this.specLauncherLayer = new Layer(layers, "SPEC_LAUNCHER");
        deviceRemotes.bind(this, hwElements, displayControl);
        
        bindNavigation(hwElements);
        transportHandler.bindTrackNavigation(this);
        transportHandler.bindControl(this, hwElements, 2);
        transportHandler.bindArrangerLayoutControl(this, hwElements, 2);
        transportHandler.bindLauncherLayoutControl(specLauncherLayer, hwElements, 2);
        transportHandler.getPanelLayout().addValueObserver(this::handlePanelLayoutUpdate);
        transportHandler.setTrackNavigation(this::navigateTracks);
        
        selectTrackBinding =
            new SegmentDisplayBinding("Select Track", cursorTrack.name(), displayControl.getTemporaryDisplay());
        this.addBinding(selectTrackBinding);
    }
    
    
    protected void handlePanelLayoutUpdate(final LayoutType newValue) {
        if (isActive()) {
            specLauncherLayer.setIsActive(newValue == LayoutType.LAUNCHER);
        }
    }
    
    private void bindNavigation(final LaunchControlXlHwElements hwElements) {
        final LaunchButton pageUpButton = hwElements.getButton(CcConstValues.PAGE_UP);
        final LaunchButton pageDownButton = hwElements.getButton(CcConstValues.PAGE_DOWN);
        pageUpButton.bindLight(this, this::canPageNavigateBackward);
        pageDownButton.bindLight(this, this::canPageNavigateForward);
        pageUpButton.bindRepeatHold(this, this::navigateBackward);
        pageDownButton.bindRepeatHold(this, this::navigateForward);
    }
    
    
    private RgbState canPageNavigateBackward() {
        if (shiftState.get()) {
            return cursorDevice.hasPrevious().get() ? RgbState.DIM_WHITE : RgbState.OFF;
        }
        return deviceRemotes.canGoBack() ? RgbState.DIM_WHITE : RgbState.OFF;
    }
    
    private RgbState canPageNavigateForward() {
        if (shiftState.get()) {
            return cursorDevice.hasNext().get() ? RgbState.DIM_WHITE : RgbState.OFF;
        }
        return deviceRemotes.canGoForward() ? RgbState.DIM_WHITE : RgbState.OFF;
    }
    
    private void navigateBackward() {
        if (shiftState.get()) {
            cursorDevice.selectPrevious();
        } else {
            deviceRemotes.selectPreviousPage();
        }
    }
    
    private void navigateForward() {
        if (shiftState.get()) {
            cursorDevice.selectNext();
        } else {
            deviceRemotes.selectNextPage();
        }
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
        specLauncherLayer.setIsActive(transportHandler.getPanelLayout().get() == LayoutType.LAUNCHER);
        deviceRemotes.setActive(true);
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        specLauncherLayer.setIsActive(false);
        deviceRemotes.setActive(false);
    }
}
