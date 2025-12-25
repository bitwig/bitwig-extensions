package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

public class RemotesLayerHandler {
    
    private final RemotesControlLayer control;
    
    public RemotesLayerHandler(RemotesControlLayer deviceControl) {
        this.control = deviceControl;
    }
    
    public boolean canNavigateLeft() {
        return  this.control.canScrollLeft();
    }
    
    public boolean canNavigateRight() {
        return  this.control.canScrollRight();
    }
    
    public void navigateLeft() {
        this.control.navigateLeft();
    }
    
    public void navigateRight() {
        this.control.navigateRight();
    }
}
