package com.bitwig.extensions.controllers.novation.slmk3.value;

import java.util.ArrayList;
import java.util.function.IntConsumer;

public class IntValue {
    
    private int value;
    private Integer min;
    private Integer max;
    private final ArrayList<IntConsumer> observers = new ArrayList<>();
    
    public IntValue() {
        this(0, null, null);
    }
    
    public IntValue(final int value, final Integer min, final Integer max) {
        this.value = value;
        this.min = min;
        this.max = max;
    }
    
    public int get() {
        return value;
    }
    
    public void addValueObserver(final IntConsumer observer) {
        observers.add(observer);
    }
    
    public void set(final int value) {
        if (this.value != value) {
            this.value = value;
            observers.forEach(observer -> observer.accept(value));
        }
    }
    
    public void setMax(final int max) {
        this.max = max;
        if (max != -1 && this.value > max) {
            set(max);
        }
    }
    
    public void setMin(final int min) {
        this.min = min;
        if (this.value < min) {
            set(min);
        }
    }
    
    public void inc(final int amount) {
        final int newValue = this.value + amount;
        if (max != null && newValue > max) {
            set(max);
        } else if (min != null && newValue < min) {
            set(min);
        }
        set(newValue);
    }
    
}
