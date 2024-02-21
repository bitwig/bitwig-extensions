package com.bitwig.extensions.controllers.mcu.bindings;

import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.mcu.display.DisplayManager;
import com.bitwig.extensions.framework.Binding;

public class VuMeterBinding extends Binding<Track, DisplayManager> {
    
    private final int index;
    private int lastValue;
    
    private record ExclusivityObject(int index, Track track) {
        
    }
    
    public VuMeterBinding(final DisplayManager target, final Track source, final int index) {
        super(new ExclusivityObject(index, source), source, target);
        this.index = index;
        source.addVuMeterObserver(14, -1, true, value -> {
            updateVuValue(value);
        });
    }
    
    private void updateVuValue(final int value) {
        if (isActive() && value != lastValue) {
            getTarget().sendVuUpdate(index, value);
        }
        lastValue = value;
    }
    
    @Override
    protected void deactivate() {
    }
    
    @Override
    protected void activate() {
        getTarget().sendVuUpdate(index, lastValue);
    }
    
}
