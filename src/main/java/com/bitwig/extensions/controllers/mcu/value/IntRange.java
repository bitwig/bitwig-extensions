package com.bitwig.extensions.controllers.mcu.value;

import java.util.function.IntConsumer;

public interface IntRange {
    void addRangeListener(int range, IntConsumer callback);

    int getIntValue();
}
