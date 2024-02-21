package com.bitwig.extensions.controllers.mcu.value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.bitwig.extension.controller.api.ObjectProxy;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.IntValueObject;

public class SettableEnumValueSelect implements IEnumDisplayValue {
    
    private final SettableEnumValue value;
    private final List<Value> knownValues = new ArrayList<>();
    private final BasicStringValue displayValue = new BasicStringValue("");
    private final IntValueObject ringValue = new IntValueObject(0, 0, 11);
    private boolean valueExists = false;
    
    public record Value(String value, String displayValue, int ringValue) {
        //
    }
    
    public SettableEnumValueSelect(final SettableEnumValue value, final ObjectProxy existsRef,
        final Value... knownValues) {
        this(value, knownValues);
        existsRef.exists().addValueObserver(exists -> {
            this.valueExists = exists;
            updateDisplay(getIndex(value.get()));
        });
    }
    
    public SettableEnumValueSelect(final SettableEnumValue value, final Value... knownValues) {
        this.value = value;
        value.markInterested();
        displayValue.set(value.get());
        Collections.addAll(this.knownValues, knownValues);
        valueExists = true;
        this.value.addValueObserver(newValue -> {
            final Integer index = getIndex(newValue);
            updateDisplay(index);
        });
    }
    
    private void updateDisplay(final Integer index) {
        if (valueExists) {
            if (index != -1) {
                displayValue.set(this.knownValues.get(index).displayValue);
                ringValue.set(this.knownValues.get(index).ringValue);
            }
        } else {
            displayValue.set("");
            ringValue.set(-1);
        }
    }
    
    public int getIndex(final String value) {
        for (int i = 0; i < knownValues.size(); i++) {
            if (knownValues.get(i).value.equals(value)) {
                return i;
            }
        }
        return -1;
    }
    
    public SettableEnumValue getValue() {
        return value;
    }
    
    @Override
    public BasicStringValue getDisplayValue() {
        return displayValue;
    }
    
    @Override
    public IntValueObject getRingValue() {
        return ringValue;
    }
    
    @Override
    public void stepRoundRobin() {
        final int index = getIndex(value.get());
        if (index != -1) {
            final int nextIndex = (index + 1) % knownValues.size();
            value.set(knownValues.get(nextIndex).value);
        }
    }
    
    @Override
    public void increment(final int inc) {
        final int index = getIndex(value.get());
        if (index != -1) {
            final int nextIndex = index + inc;
            if (nextIndex >= 0 && nextIndex < knownValues.size()) {
                value.set(knownValues.get(nextIndex).value);
            }
        }
    }
    
    @Override
    public String getEnumValue() {
        return value.get();
    }
    
    @Override
    public void reset() {
        setIndex(0);
    }
    
    @Override
    public void setIndex(final int index) {
        if (index >= 0 && index < knownValues.size()) {
            value.set(knownValues.get(index).value);
        }
    }
}
