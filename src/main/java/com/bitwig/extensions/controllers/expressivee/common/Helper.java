package com.bitwig.extensions.controllers.expressivee.common;

import java.text.Normalizer;

public class Helper {
    public static int toTwosComplement(int value, int bits) {
        int mask = (1 << bits) - 1;
        return value & mask;
    }

    public static int fromTwosComplement(int rawValue, int bits) {
        int signBit = 1 << (bits - 1);
        int mask = (1 << bits) - 1;
        rawValue &= mask;

        // Si le bit de signe est activé, c'est une valeur négative
        if ((rawValue & signBit) != 0) {
            return rawValue - (1 << bits);
        } else {
            return rawValue;
        }
    }

    public static String normalize(String value) {
        return Normalizer
                .normalize(value, Normalizer.Form.NFD) // Decompose accented characters
                .replaceAll("[^\\p{ASCII}]", "") // Remove non-ASCII characters
                .toLowerCase()
                .replaceAll("[^a-z0-9 ()+,\\-./_\\[\\]]", " ") // Enforce Osmose font whitelist
                .replaceAll("\\s+", " ") // Collapse multiple spaces
                .trim();
    }
}
