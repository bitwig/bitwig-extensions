package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;

@Component
public class TrackControl {
    
    private final Layer armLayer;
    private final Layer selectLayer;
    private final Mode mode = Mode.ARM;
    private final Layer currentLayer;
    
    private enum Mode {
        ARM,
        SELECT
    }
    
    public TrackControl(final Layers layers, final ViewControl viewControl, final LaunchkeyHwElements hwElements) {
        this.armLayer = new Layer(layers, "ARM_TRACK");
        this.selectLayer = new Layer(layers, "SELECT_TRACK");
        
        this.currentLayer = selectLayer;
    }
    
    @Activate
    public void activate() {
        this.currentLayer.setIsActive(true);
    }
}
