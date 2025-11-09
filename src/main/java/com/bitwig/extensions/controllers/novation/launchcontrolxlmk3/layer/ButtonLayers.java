package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.layer;

import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;

@Component
public class ButtonLayers {
    
    private final Layer selectLayer;
    private final Layer soloLayer;
    private final Layer muteLayer;
    private final Layer armLayer;
    private final Layer buttonOverlayLayer;
    
    public ButtonLayers(final Layers layers) {
        selectLayer = new Layer(layers, "SELECT");
        soloLayer = new Layer(layers, "SOLO");
        armLayer = new Layer(layers, "ARM");
        muteLayer = new Layer(layers, "MUTE");
        buttonOverlayLayer = new Layer(layers, "BUTTON_OVERLAY");
    }
    
    public Layer getSelectLayer() {
        return selectLayer;
    }
    
    public Layer getSoloLayer() {
        return soloLayer;
    }
    
    public Layer getMuteLayer() {
        return muteLayer;
    }
    
    public Layer getArmLayer() {
        return armLayer;
    }
    
    public Layer getButtonOverlayLayer() {
        return buttonOverlayLayer;
    }
}
