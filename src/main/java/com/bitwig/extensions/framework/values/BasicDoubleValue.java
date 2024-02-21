package com.bitwig.extensions.framework.values;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.callback.DoubleValueChangedCallback;
import com.bitwig.extension.controller.api.SettableDoubleValue;

public class BasicDoubleValue implements SettableDoubleValue {
    
    private double value;
    private final List<DoubleValueChangedCallback> changeListeners = new ArrayList<>();
    
    @Override
    public double get() {
        return value;
    }
    
    @Override
    public void markInterested() {
    
    }
    
    @Override
    public void addValueObserver(final DoubleValueChangedCallback doubleValueChangedCallback) {
        changeListeners.add(doubleValueChangedCallback);
    }
    
    @Override
    public boolean isSubscribed() {
        return true;
    }
    
    @Override
    public void setIsSubscribed(final boolean b) {
    }
    
    @Override
    public void subscribe() {
    }
    
    @Override
    public void unsubscribe() {
    }
    
    @Override
    public void set(final double v) {
        if (v != this.value) {
            this.value = v;
            changeListeners.forEach(callback -> callback.valueChanged(v));
        }
    }
    
    @Override
    public void inc(final double v) {
        final double newValue = this.value + v;
        set(newValue);
    }
}
