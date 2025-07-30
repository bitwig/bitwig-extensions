package com.bitwig.extensions.controllers.nativeinstruments.komplete.device;

public class DirectControlSlot extends DirectSlot {
    
    private long lastDirectChange = 0;
    
    public DirectControlSlot(final int index) {
        super(index);
    }
    
    public void apply(final DirectSlot otherSlot) {
        this.paramId = otherSlot.getParamId();
        this.paramName.set(otherSlot.getParamName().get());
        this.paramValue.set(otherSlot.getParamValue().get());
        this.value.set(otherSlot.getValue());
    }
    
    public void updateValue(final double v) {
        final long diff = (System.currentTimeMillis() - lastDirectChange);
        if (diff > 50) {
            this.value.set(v);
        }
    }
    
    public void applyValue(final double v) {
        lastDirectChange = System.currentTimeMillis();
        this.value.set(v);
    }
    
    
}
