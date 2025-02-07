package com.bitwig.extensions.controllers.novation.slmk3.value;

import java.util.Objects;

public class BufferedObservableValue<T> extends ObservableValue<T> {
    
    private T stashedValue;
    private final T defaultValue;
    private T previousValue;
    
    public BufferedObservableValue(final T value) {
        super(value);
        this.defaultValue = value;
    }
    
    
    public void set(final T value) {
        if (!Objects.equals(this.value, value)) {
            this.previousValue = this.value;
            this.value = value;
            listeners.forEach(observer -> observer.accept(value));
        }
    }
    
    public void restorePrevious() {
        set(this.previousValue);
    }
    
    public void stash() {
        this.stashedValue = value;
    }
    
    public T getStashedValue() {
        return stashedValue;
    }
    
    public void clearStash() {
        this.stashedValue = defaultValue;
    }
    
    public void restoreFromStash() {
        if (!Objects.equals(this.stashedValue, defaultValue)) {
            set(stashedValue);
            this.stashedValue = defaultValue;
        }
    }
    
}
