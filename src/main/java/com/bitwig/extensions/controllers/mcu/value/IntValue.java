package com.bitwig.extensions.controllers.mcu.value;

import java.util.function.IntConsumer;

public interface IntValue {
    int getMax();

    int getMin();

    void addIntValueObserver(IntConsumer callback);

    void addRangeObserver(RangeChangedCallback callback);

    int getIntValue();

    String displayedValue();
}
