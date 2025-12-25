package com.bitwig.extensions.controllers.akai.mpkmk4.display;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StringUtil {
    private static final char[] SPECIALS = {
        'ä', 'ü', 'ö', 'Ä', 'Ü', 'Ö', 'ß', 'é', 'è', 'ê', 'â', 'á', 'à', //
        'û', 'ú', 'ù', 'ô', 'ó', 'ò'
    };
    private static final String[] REPLACE = {
        "a", "u", "o", "A", "U", "O", "ss", "e", "e", "e", "a", "a", "a", //
        "u", "u", "u", "o", "o", "o"
    };
    
    public static String toAsciiDisplayFill(final String name, final int len) {
        final StringBuilder b = new StringBuilder();
        for (int i = 0; i < name.length() && b.length() < len; i++) {
            final char c = name.charAt(i);
            //            if (c == 32) {
            //                continue;
            //            }
            if (c < 128) {
                b.append(c);
            } else {
                final int replacement = getReplace(c);
                if (replacement >= 0) {
                    b.append(REPLACE[replacement]);
                }
            }
        }
        return b.toString();
    }
    
    
    public static String toAsciiDisplay(final String name, final int maxLen) {
        final StringBuilder b = new StringBuilder();
        for (int i = 0; i < name.length() && b.length() < maxLen; i++) {
            final char c = name.charAt(i);
            //            if (c == 32) {
            //                continue;
            //            }
            if (c < 128) {
                b.append(c);
            } else {
                final int replacement = getReplace(c);
                if (replacement >= 0) {
                    b.append(REPLACE[replacement]);
                }
            }
        }
        return b.toString();
    }
    
    private static int getReplace(final char c) {
        for (int i = 0; i < SPECIALS.length; i++) {
            if (c == SPECIALS[i]) {
                return i;
            }
        }
        return -1;
    }
    
    public static String sysExString(byte[] data) {
        List<String> dataString = new ArrayList<>();
        for(byte b : data) {
            dataString.add("%02X".formatted(b));
        }
        return dataString.stream().collect(Collectors.joining(" "));
    }
    
}
