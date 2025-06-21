package com.bitwig.extensions.controllers.novation.slmk3.value;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.stream.Stream;

import com.bitwig.extension.callback.StringValueChangedCallback;

public class IntSetValue {
    private final Set<Integer> values = new HashSet<>();
    private final List<IntConsumer> sizeListener = new ArrayList<>();
    private final List<StringValueChangedCallback> callbacks = new ArrayList<>();
    
    public IntSetValue() {
    
    }
    
    public void addSizeValueListener(final IntConsumer listener) {
        sizeListener.add(listener);
    }
    
    public Stream<Integer> stream() {
        return values.stream();
    }
    
    public void forEach(final IntConsumer consumer) {
        values.forEach(consumer::accept);
    }
    
    public void remove(final int index) {
        final int oldSize = values.size();
        values.remove(index);
        final int newSize = values.size();
        if (oldSize != newSize) {
            sizeListener.forEach(l -> l.accept(newSize));
            //fireChanged(convert(newSize));
        }
    }
    
    public void clear() {
        final int oldsize = values.size();
        if (oldsize > 0) {
            values.clear();
            //fireChanged(convert(0));
            sizeListener.forEach(l -> l.accept(0));
        }
    }
    
    public boolean contains(final int value) {
        return values.contains(value);
    }
    
    public void add(final int index) {
        final int oldSize = values.size();
        values.add(index);
        final int newSize = values.size();
        if (oldSize != newSize) {
            sizeListener.forEach(l -> l.accept(newSize));
            //fireChanged(convert(newSize));
        }
    }
    
    private String convert(final int value) {
        if (value == 0) {
            return "[---]";
        }
        return String.format("[ %2d]", value);
    }
    
    public String get() {
        return convert(values.size());
    }
    
    public boolean isEmpty() {
        return values.isEmpty();
    }
}
