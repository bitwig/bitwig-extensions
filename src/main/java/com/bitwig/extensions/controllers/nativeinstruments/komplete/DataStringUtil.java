package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StringUtil {
    public static String toString(final byte[] data) {
        final List<String> values = new ArrayList<>();
        for (final byte d : data) {
            values.add("%02x".formatted(d));
        }
        return values.stream().collect(Collectors.joining());
    }
}
