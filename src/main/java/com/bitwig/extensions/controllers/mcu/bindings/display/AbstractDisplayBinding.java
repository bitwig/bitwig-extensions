package com.bitwig.extensions.controllers.mcu.bindings.display;

import com.bitwig.extensions.controllers.mcu.display.DisplayManager;
import com.bitwig.extensions.controllers.mcu.layer.ControlMode;
import com.bitwig.extensions.framework.Binding;

public abstract class AbstractDisplayBinding<T> extends Binding<T, DisplayManager> {
    protected ControlMode controlMode;
    protected String lastValue = "";
    protected boolean exists = false;
    protected DisplayTarget targetIndex;
    
    public AbstractDisplayBinding(final DisplayManager target, final ControlMode mode, final DisplayTarget targetIndex,
        final T value) {
        super(targetIndex, value, target);
        this.targetIndex = targetIndex;
        this.controlMode = mode;
        this.exists = true;
    }
    
    //    public void setControlMode(ControlMode controlMode) {
    //        this.controlMode = controlMode;
    //    }
    
    @Override
    protected void deactivate() {
    }
    
    protected void handleExists(final boolean exists) {
        this.exists = exists;
        if (isActive()) {
            updateDisplay();
        }
    }
    
    protected void updateDisplay() {
        if (exists) {
            getTarget().sendText(controlMode, targetIndex.rowIndex(), targetIndex.cellIndex(), lastValue);
        } else {
            getTarget().sendText(controlMode, targetIndex.rowIndex(), targetIndex.cellIndex(), "");
        }
    }
    
    @Override
    protected void activate() {
        updateDisplay();
    }
    
}
