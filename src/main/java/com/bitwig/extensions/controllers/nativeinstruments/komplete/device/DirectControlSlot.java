package com.bitwig.extensions.controllers.nativeinstruments.komplete.device;

import com.bitwig.extensions.controllers.nativeinstruments.komplete.KompleteKontrolExtension;

public class DirectControlSlot extends DirectSlot {
    private int initial;
    private double increments = 0;
    private int steps = 0;
    
    public DirectControlSlot(final int index) {
        super(index);
    }
    
    public void apply(final DirectSlot otherSlot) {
        this.paramId = otherSlot.getParamId();
        this.paramName.set(otherSlot.getParamName().get());
        this.paramValue.set(otherSlot.getParamValue().get());
        initial = -1;
        this.value.set(otherSlot.getValue());
        increments = 0;
        steps = 0;
    }
    
    public void notifyIncrement(final double inc) {
        if (initial == -1) {
            initial = value.get();
        } else {
            this.increments += inc;
            //KompleteKontrolExtension.println(" %d %f  in=%d  v=%d", getIndex(), increments, initial, value.get());
            if (Math.abs(increments) > 1 && initial == value.get()) {
                KompleteKontrolExtension.println("SWITCH STEPPED IN %f  %d  %d", inc, initial, value.get());
                steps = 1;
            }
        }
    }
    
    @Override
    public void setValue(final int value) {
        this.value.set(value);
    }
    
    public void resetIncrements() {
        increments = 0;
    }
    
    public boolean isStepped() {
        return steps > 0;
    }
    
}
