package com.bitwig.extensions.controllers.mcu.value;

@FunctionalInterface
public interface DoubleValueConverter {
    String convert(double v);
}
