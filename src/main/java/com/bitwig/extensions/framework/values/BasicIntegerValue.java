package com.bitwig.extensions.framework.values;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.callback.IntegerValueChangedCallback;
import com.bitwig.extension.controller.api.RelativeHardwareControl;
import com.bitwig.extension.controller.api.RelativeHardwareControlBinding;
import com.bitwig.extension.controller.api.SettableIntegerValue;

public class BasicIntegerValue implements SettableIntegerValue {
    private int value;
    private final List<IntegerValueChangedCallback> callbacksList = new ArrayList<>();
    
    @Override
    public void set(final int value) {
        if (this.value != value) {
            this.value = value;
            callbacksList.forEach(callback -> callback.valueChanged(this.value));
        }
    }
    
    @Override
    public void inc(final int inc) {
        this.value += inc;
        callbacksList.forEach(callback -> callback.valueChanged(this.value));
    }
    
    @Override
    public int get() {
        return value;
    }
    
    @Override
    public void addValueObserver(final IntegerValueChangedCallback callback, final int i) {
        callbacksList.add(callback);
    }
    
    @Override
    public RelativeHardwareControlBinding addBindingWithSensitivity(
        final RelativeHardwareControl relativeHardwareControl, final double v) {
        return null;
    }
    
    @Override
    public void markInterested() {
    }
    
    @Override
    public void addValueObserver(final IntegerValueChangedCallback callback) {
        callbacksList.add(callback);
    }
    
    @Override
    public boolean isSubscribed() {
        return false;
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
}
