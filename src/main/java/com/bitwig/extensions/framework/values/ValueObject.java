package com.bitwig.extensions.framework.values;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A generic value object which tracks changes and handles its string representation.
 *
 * @param <T> Object type
 */
public class ValueObject<T> implements IncrementalValue {
    
    private final List<Consumer<T>> callbacks = new ArrayList<>();
    
    private final IncrementHandler<T> incrementHandler;
    private final StringConverter<T> converter;
    
    private T value;
    
    /**
     * Provides the value object with a custom string representation decoupled from the standard toString method.
     *
     * @param <T> value type
     */
    @FunctionalInterface
    public interface StringConverter<T> {
        String convert(T value);
    }
    
    /**
     * Performs what is considered an increment/decrement on the given value.
     *
     * @param <T> value type
     */
    @FunctionalInterface
    public interface IncrementHandler<T> {
        /**
         * Performs generic increment/decrement.
         *
         * @param value     the object value
         * @param increment the number of steps to increment/decrement (if <0) by
         * @return the new value
         */
        T increment(T value, int increment);
    }
    
    /**
     * Creates the value object without custom increment/decrement or string representation.
     * Thus using the toString method of the object and increment/decrement will have no effect.
     *
     * @param initValue the initial value
     */
    public ValueObject(final T initValue) {
        this.value = initValue;
        this.incrementHandler = null;
        this.converter = null;
    }
    
    /**
     * Creates the value object with a custom increment/decrement and string representation.
     *
     * @param initValue        the initial value
     * @param incrementHandler the increment/decrement behavior
     * @param converter        the custom string representation
     */
    public ValueObject(final T initValue, final IncrementHandler<T> incrementHandler,
        final StringConverter<T> converter) {
        this.value = initValue;
        this.incrementHandler = incrementHandler;
        this.converter = converter;
    }
    
    public void addValueObserver(final Consumer<T> callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }
    
    public void set(final T value) {
        if (this.value == value) {
            return;
        }
        this.value = value;
        for (final Consumer<T> listener : callbacks) {
            listener.accept(value);
        }
    }
    
    public void setDirect(final T value) {
        if (this.value == value) {
            return;
        }
        this.value = value;
    }
    
    @Override
    public void increment(final int inc) {
        if (incrementHandler != null) {
            set(incrementHandler.increment(value, inc));
        }
    }
    
    @Override
    public String displayedValue() {
        if (converter != null) {
            return converter.convert(value);
        }
        return value.toString();
    }
    
    public T get() {
        return value;
    }
    
}
