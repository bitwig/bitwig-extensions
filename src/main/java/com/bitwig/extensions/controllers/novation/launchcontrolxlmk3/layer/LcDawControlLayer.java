package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.layer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.CcConstValues;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlMk3Extension;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlXlHwElements;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchViewControl;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.SegmentDisplayBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchButton;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.DisplayControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.LayoutType;

@Component(tag = "LCModel")
public class LcDawControlLayer extends AbstractDawControlLayer {
    
    private final Layer specLayer;
    private final Layer specLauncherLayer;
    private final LaunchControlXlHwElements hwElements;
    private Layer specButtonLayer;
    
    public LcDawControlLayer(final Layers layers, final LaunchControlXlHwElements hwElements,
        final LaunchViewControl viewControl, final DisplayControl displayControl,
        final TransportHandler transportHandler, final ControllerHost host) {
        super(layers, hwElements, viewControl, displayControl, transportHandler, host);
        this.hwElements = hwElements;
        specLayer = new Layer(layers, "DAW_SPEC");
        specLauncherLayer = new Layer(layers, "DAW_SPEC_LAUNCHER");
        deviceRemotes.bind(this, hwElements, displayControl);
        final LaunchButton lcButton = hwElements.getButton(CcConstValues.DAW_SPEC);
        lcButton.bindIsPressed(this, this::handleSpecButton);
        selectTrackBinding =
            new SegmentDisplayBinding("Select Track", cursorTrack.name(), displayControl.getTemporaryDisplay());
        this.addBinding(selectTrackBinding);
        
        bindNavigation(hwElements);
        transportHandler.bindControl(specLayer, hwElements, 1);
        transportHandler.bindArrangerLayoutControl(specLayer, hwElements, 1);
        transportHandler.bindLauncherLayoutControl(specLauncherLayer, hwElements, 1);
        transportHandler.getPanelLayout().addValueObserver(this::handlePanelLayoutUpdate);
        transportHandler.setTrackNavigation(this::navigateTracks);
    }
    
    public void initButtonOverlay(final Layer overlay) {
        LaunchControlMk3Extension.println(" >> ");
        this.specButtonLayer = overlay;
        for (int i = 0; i < 8; i++) {
            final LaunchButton button = hwElements.getRowButtons(0, i);
            button.bindLight(overlay, () -> RgbState.TURQUOISE);
            button.bindPressed(overlay, () -> {});
        }
    }
    
    private void handleSpecButton(final Boolean pressed) {
        specLayer.setIsActive(pressed);
        specButtonLayer.setIsActive(pressed);
        if (pressed) {
            displayControl.show2LineTemporary("Row 2", "Transport Control");
            if (transportHandler.getPanelLayout().get() == LayoutType.LAUNCHER) {
                specLauncherLayer.setIsActive(true);
            }
        } else {
            displayControl.cancelTemporary();
            displayControl.updateStatic();
            specLauncherLayer.setIsActive(false);
        }
    }
    
    private void bindNavigation(final LaunchControlXlHwElements hwElements) {
        final LaunchButton trackLeftButton = hwElements.getButton(CcConstValues.TRACK_LEFT);
        final LaunchButton trackRightButton = hwElements.getButton(CcConstValues.TRACK_RIGHT);
        
        trackLeftButton.bindLight(this, () -> viewControl.canNavLeft() ? RgbState.WHITE : RgbState.OFF);
        trackRightButton.bindLight(
            this, () -> viewControl.canNavRight(hwElements.getShiftState().get()) ? RgbState.WHITE : RgbState.OFF);
        trackRightButton.bindRepeatHold(this, this::navTrackRight);
        trackLeftButton.bindRepeatHold(this, this::navTrackLeft);
        
        final LaunchButton pageUpButton = hwElements.getButton(CcConstValues.PAGE_UP);
        final LaunchButton pageDownButton = hwElements.getButton(CcConstValues.PAGE_DOWN);
        pageUpButton.bindLight(this, this::canPageNavigateBackward);
        pageDownButton.bindLight(this, this::canPageNavigateForward);
        pageUpButton.bindRepeatHold(this, this::navigateBackward);
        pageDownButton.bindRepeatHold(this, this::navigateForward);
    }
    
    protected void handlePanelLayoutUpdate(final LayoutType newValue) {
        if (isActive()) {
            specLauncherLayer.setIsActive(specLayer.isActive() && newValue == LayoutType.LAUNCHER);
        }
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
        displayControl.cancelTemporary();
    }
    
    private void navigateForward() {
        if (shiftState.get()) {
            cursorDevice.selectNext();
        } else {
            deviceRemotes.selectNextPage();
        }
        displayControl.cancelTemporary();
    }
    
    public void navTrackRight() {
        if (shiftState.get()) {
            viewControl.navigateCursorBy(8);
        } else {
            viewControl.navigateCursorBy(1);
        }
        deviceDisplayBinding.blockUpdate();
    }
    
    public void navTrackLeft() {
        if (shiftState.get()) {
            viewControl.navigateCursorBy(-8);
        } else {
            viewControl.navigateCursorBy(-1);
        }
        deviceDisplayBinding.blockUpdate();
    }
    
    
    @Override
    protected void onActivate() {
        super.onActivate();
        deviceRemotes.setActive(true);
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        deviceRemotes.setActive(false);
        specLauncherLayer.setIsActive(false);
        specLayer.setIsActive(false);
        specButtonLayer.setIsActive(false);
    }
    
}