package com.bitwig.extensions.controllers.akai.mpkmk4.bindings;

import com.bitwig.extensions.controllers.akai.mpkmk4.controls.Encoder;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.TemporaryLineValue;
import com.bitwig.extensions.framework.Binding;

public class ParameterContextBinding extends Binding<Encoder, TemporaryLineValue> {
    
    public ParameterContextBinding(Encoder encoder, TemporaryLineValue line) {
        super(line, encoder, line);
        encoder.getEncoder().isUpdatingTargetValue().addValueObserver(this::handleUpdating);
    }
    
    private void handleUpdating(boolean updating) {
        if (!isActive()) {
            return;
        }
        if (updating) {
            getTarget().update();
        }
    }
    
    @Override
    protected void deactivate() {
        
    }
    
    @Override
    protected void activate() {
        
    }
}
