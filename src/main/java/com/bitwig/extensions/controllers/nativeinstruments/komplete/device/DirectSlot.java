package com.bitwig.extensions.controllers.nativeinstruments.komplete.device;

import com.bitwig.extensions.controllers.nativeinstruments.komplete.KompleteKontrolExtension;
import com.bitwig.extensions.framework.values.BasicIntegerValue;
import com.bitwig.extensions.framework.values.BasicStringValue;

public class DirectSlot {
    private final static String ID_PREFIX = "CONTENTS/";
    private final BasicStringValue paramName = new BasicStringValue();
    private final BasicStringValue paramValue = new BasicStringValue();
    private final BasicIntegerValue value = new BasicIntegerValue();
    private String paramId;
    private final int index;
    private int initial;
    private double increments = 0;
    private int steps = 0;
    
    public DirectSlot(final int index) {
        this.index = index;
    }
    
    public void apply(final DirectSlot otherSlot) {
        this.paramId = otherSlot.getParamId();
        this.paramName.set(otherSlot.getParamName().get());
        this.paramValue.set(otherSlot.getParamValue().get());
        initial = -1;
        this.value.set(otherSlot.getValue().get());
        increments = 0;
        steps = 0;
    }
    
    public BasicStringValue getParamName() {
        return paramName;
    }
    
    public BasicStringValue getParamValue() {
        return paramValue;
    }
    
    public void setValueString(final String value) {
        this.paramValue.set(value);
    }
    
    public void setValue(final int value) {
        this.value.set(value);
    }
    
    public BasicIntegerValue getValue() {
        return value;
    }
    
    public void setParamId(final String paramId) {
        this.paramId = paramId;
    }
    
    public String getParamId() {
        return paramId;
    }
    
    public String getInParamId() {
        if (paramId != null && paramId.startsWith(ID_PREFIX)) {
            return "%sROOT_GENERIC_MODULE/%s".formatted(ID_PREFIX, paramId.substring(ID_PREFIX.length()));
        }
        return paramId;
    }
    
    public boolean exists() {
        return paramId != null;
    }
    
    public int getIndex() {
        return index;
    }
    
    public void notifyIncrement(final double inc) {
        if (initial == -1) {
            initial = value.get();
        } else {
            this.increments += inc;
            if (Math.abs(increments) > 3 && initial == value.get()) {
                KompleteKontrolExtension.println(" IN %f  %d  %d", inc, initial, value.get());
                steps = 1;
            }
        }
    }
    
    public double getIncrements() {
        return increments;
    }
    
    public void resetIncrements() {
        increments = 0;
    }
    
    public int getSteps() {
        return steps;
    }
}
