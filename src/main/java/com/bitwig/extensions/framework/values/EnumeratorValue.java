package com.bitwig.extensions.framework.values;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class EnumeratorValue<T> {
    
    private List<T> elements = new ArrayList<>();
    private final List<Consumer<T>> callbacks = new ArrayList<>();
    private int index;
    
    public EnumeratorValue(final T[] values) {
        this.elements = Arrays.stream(values).toList();
    }
    
    public EnumeratorValue(final List<T> values) {
        this.elements = values.stream().toList();
    }
    
    public void addValueObserver(final Consumer<T> callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }
    
    public void set(final T value) {
        final int setIndex = elements.indexOf(value);
        if (setIndex == -1 || setIndex == index) {
            return;
        }
        this.index = setIndex;
        for (final Consumer<T> listener : callbacks) {
            listener.accept(value);
        }
    }
    
    public T get() {
        return elements.get(index);
    }
    
    public void increment(final int inc, final boolean roundRobin) {
        int next = index + inc;
        if (roundRobin) {
            if (next < 0) {
                next = elements.size() - 1;
            } else if (next >= elements.size()) {
                next = 0;
            }
        } else {
            if (next < 0 || next >= elements.size()) {
                return;
            }
        }
        this.index = next;
        for (final Consumer<T> listener : callbacks) {
            listener.accept(elements.get(index));
        }
    }
}
