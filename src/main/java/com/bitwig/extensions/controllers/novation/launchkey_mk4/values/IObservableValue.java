package com.bitwig.extensions.controllers.novation.launchkey_mk4.values;

import java.util.function.Consumer;

public interface IObservableValue<T> {
    void addValueObserver(final Consumer<T> observer);
    
    void addDisplayObserver(Consumer<String> observer);
    
    T get();
    
    void set(final T value);
    
    String getDisplayString();
}
