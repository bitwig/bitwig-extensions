package com.bitwig.extensions.controllers.novation.launchkey_mk4.values;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class ObservableValue<T> implements IObservableValue<T> {
    
    protected T value;
    protected final List<Consumer<T>> listeners = new ArrayList<>();
    protected final List<Consumer<String>> displayListeners = new ArrayList<>();
    protected final Function<T, String> stringConverter;
    
    public ObservableValue(final T value) {
        this(value, v -> v.toString());
    }
    
    public ObservableValue(final T value, final Function<T, String> stringConverter) {
        this.value = value;
        this.stringConverter = v -> v.toString();
    }
    
    @Override
    public void addValueObserver(final Consumer<T> observer) {
        listeners.add(observer);
    }
    
    @Override
    public void addDisplayObserver(final Consumer<String> observer) {
        displayListeners.add(observer);
    }
    
    @Override
    public T get() {
        return value;
    }
    
    @Override
    public void set(final T value) {
        if (!Objects.equals(this.value, value)) {
            this.value = value;
            listeners.forEach(observer -> observer.accept(value));
            displayListeners.forEach(observer -> observer.accept(stringConverter.apply(value)));
        }
    }
    
    public String getDisplayString() {
        return this.stringConverter.apply(value);
    }
    
}
