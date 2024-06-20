package com.bitwig.extensions.controllers.mcu.value;

@FunctionalInterface
public interface IntValueConverter {
    String convert(int value);
}
