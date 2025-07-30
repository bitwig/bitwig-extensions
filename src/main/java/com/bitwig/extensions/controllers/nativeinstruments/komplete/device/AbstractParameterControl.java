package com.bitwig.extensions.controllers.nativeinstruments.komplete.device;

import com.bitwig.extensions.framework.Layer;

public abstract class AbstractParameterControl {
    private boolean active;
    protected final Layer layer;
    
    public AbstractParameterControl(final Layer layer) {
        this.layer = layer;
    }
    
    public void setActive(final boolean active) {
        this.active = active;
        layer.setIsActive(active);
    }
    
    public boolean isActive() {
        return active;
    }
    
    public abstract void navigateLeft();
    
    public abstract void navigateRight();
    
    public abstract void setFineTune(final boolean fineTune);
    
    public abstract boolean canScrollRight();
    
    public abstract boolean canScrollLeft();
}
