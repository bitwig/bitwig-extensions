package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BooleanValueObject;

@Component
public class LayerPool {
    private final Layer sceneControlSession;
    private final Layer sceneControlSequencer;
    private final BooleanValueObject sequencerControl = new BooleanValueObject();
    
    public LayerPool(final Layers layers) {
        this.sceneControlSequencer = new Layer(layers, "SCENE_SEQUENCER");
        this.sceneControlSession = new Layer(layers, "SCENE_SESSION");
    }
    
    public Layer getSceneControlSequencer() {
        return sceneControlSequencer;
    }
    
    public Layer getSceneControlSession() {
        return sceneControlSession;
    }
    
    public BooleanValueObject getSequencerControl() {
        return sequencerControl;
    }
    
    public void activate() {
        sceneControlSequencer.setIsActive(sequencerControl.get());
        sceneControlSession.setIsActive(!sequencerControl.get());
    }
    
    public void deactivate() {
        sceneControlSequencer.setIsActive(false);
        sceneControlSession.setIsActive(false);
    }
}
