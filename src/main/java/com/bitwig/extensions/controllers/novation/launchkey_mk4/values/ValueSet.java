package com.bitwig.extensions.controllers.novation.launchkey_mk4.values;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.callback.StringValueChangedCallback;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer.IncBuffer;
import com.bitwig.extensions.framework.values.BasicStringValue;

public class ValueSet implements ControlValue {
    
    private int currentIndex = 0;
    private final List<StringValueChangedCallback> listeners = new ArrayList<>();
    private final List<Entry> values = new ArrayList<>();
    private final BasicStringValue displayValue = new BasicStringValue();
    private final IncBuffer incBuffer = new IncBuffer(5);
    
    private record Entry(String display, double value) {
        
    }
    
    public void addValueObserver(final StringValueChangedCallback callback) {
        listeners.add(callback);
    }
    
    
    public ValueSet add(final String valStr, final double value) {
        values.add(new Entry(valStr, value));
        displayValue.set(values.get(currentIndex).display);
        return this;
    }
    
    private ValueSet select(final int i) {
        if (i >= 0 && i < values.size()) {
            currentIndex = i;
        }
        return this;
    }
    
    public void setSelectedIndex(final int index) {
        if (index >= 0 && index < values.size()) {
            currentIndex = index;
            displayValue.set(values.get(currentIndex).display);
            listeners.forEach(listener -> listener.valueChanged(values.get(currentIndex).display));
        }
    }
    
    @Override
    public void incrementBy(final int inc, final boolean modifier) {
        final int increment = incBuffer.inc(inc);
        if (increment == 0) {
            return;
        }
        increment(inc);
    }
    
    public void increment(final int inc) {
        setSelectedIndex(currentIndex + inc);
    }
    
    public int size() {
        return values.size();
    }
    
    public int getCurrentIndex() {
        return currentIndex;
    }
    
    @Override
    public BasicStringValue getDisplayValue() {
        return displayValue;
    }
    
    public double getValue() {
        return values.get(currentIndex).value;
    }
}
