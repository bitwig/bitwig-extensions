package com.bitwig.extensions.controllers.novation.slmk3.value;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.bitwig.extensions.framework.values.BasicStringValue;

public class StringValueSet {
    private final List<Entry> values = new ArrayList<>();
    private final BasicStringValue displayValue = new BasicStringValue("");
    private int currentIndex = 0;
    private Consumer<String> valueReceiver;
    
    public StringValueSet add(final String value, final String showValue) {
        values.add(new Entry(value, showValue));
        if (values.size() == 1) {
            displayValue.set(showValue);
        }
        return this;
    }
    
    public StringValueSet add(final String value) {
        values.add(new Entry(value, value));
        return this;
    }
    
    public StringValueSet setValueReceiver(final Consumer<String> valueReceiver) {
        this.valueReceiver = valueReceiver;
        return this;
    }
    
    private record Entry(String value, String displayValue) {
    
    }
    
    private int indexOfValue(final String value) {
        for (int i = 0; i < values.size(); i++) {
            if (value.equals(values.get(i).value())) {
                return i;
            }
        }
        return -1;
    }
    
    
    public void setToValue(final String value) {
        final int index = indexOfValue(value);
        if (index != -1) {
            this.currentIndex = index;
            displayValue.set(values.get(currentIndex).displayValue());
        }
    }
    
    public void incrementBy(final int inc) {
        final int newIndex = inc + currentIndex;
        if (newIndex >= 0 && newIndex < values.size()) {
            currentIndex = newIndex;
            displayValue.set(values.get(currentIndex).displayValue());
            if (valueReceiver != null) {
                valueReceiver.accept(values.get(currentIndex).value());
            }
        }
    }
    
    public BasicStringValue getDisplayValue() {
        return displayValue;
    }
}
