package com.bitwig.extensions.controllers.mcu.value;

public interface IncrementalValue {
    void increment(int inc);

    String displayedValue();
}
