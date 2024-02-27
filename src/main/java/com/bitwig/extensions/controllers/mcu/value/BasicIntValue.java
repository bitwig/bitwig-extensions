package com.bitwig.extensions.controllers.mcu.value;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class BasicIntValue {
    private int value;
    private final List<IntConsumer> listeners = new ArrayList<>();
    
    public void set(final int value) {
        if (value != this.value) {
            this.value = value;
            this.listeners.forEach(listener -> listener.accept(this.value));
        }
    }
    
    public void addListener(final IntConsumer listener) {
        this.listeners.add(listener);
    }
    
    public void setImmediately(final int value) {
        this.value = value;
    }
    
    public int get() {
        return value;
    }
    
}
