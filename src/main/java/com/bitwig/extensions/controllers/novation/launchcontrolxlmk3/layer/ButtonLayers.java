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
    
    public ButtonLayers(final Layers layers) {
        selectLayer = new Layer(layers, "SELECT");
        soloLayer = new Layer(layers, "SOLO");
        armLayer = new Layer(layers, "ARM");
        muteLayer = new Layer(layers, "MUTE");
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
}
