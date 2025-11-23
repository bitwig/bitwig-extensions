package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.layer;

import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlXlHwElements;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.DisplayControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.LayoutType;

public class SpecControl {
    
    private final Layer specLayer;
    private final Layer specLauncherLayer;
    private final TransportHandler transportHandler;
    private final DisplayControl displayControl;
    
    public SpecControl(final Layers layers, final LaunchControlXlHwElements hwElements,
        final TransportHandler transportHandler, final DisplayControl displayControl) {
        
        specLayer = new Layer(layers, "DAW_SPEC");
        specLauncherLayer = new Layer(layers, "DAW_SPEC_LAUNCHER");
        
        this.transportHandler = transportHandler;
        this.displayControl = displayControl;
        transportHandler.bindControl(specLayer, hwElements, 1);
        transportHandler.bindArrangerLayoutControl(specLayer, hwElements, 1);
        transportHandler.bindLauncherLayoutControl(specLauncherLayer, hwElements, 1);
        transportHandler.getPanelLayout().addValueObserver(this::handlePanelLayoutUpdate);
        transportHandler.assignTransportButtons(hwElements.getButtons(0), specLayer);
    }
    
    protected void handlePanelLayoutUpdate(final LayoutType newValue) {
        if (specLayer.isActive()) {
            specLauncherLayer.setIsActive(specLayer.isActive() && newValue == LayoutType.LAUNCHER);
        }
    }
    
    public void setActive(final boolean active) {
        specLayer.setIsActive(active);
        if (active) {
            displayControl.show2LineTemporary("Button Row 2", "Transport Control");
            specLauncherLayer.setIsActive(transportHandler.getPanelLayout().get() == LayoutType.LAUNCHER);
        } else {
            specLauncherLayer.setIsActive(false);
            displayControl.cancelTemporary();
        }
    }
}
