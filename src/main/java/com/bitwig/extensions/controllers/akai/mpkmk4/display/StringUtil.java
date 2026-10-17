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
        for (byte b : data) {
            dataString.add("%02X".formatted(b));
        }
        return dataString.stream().collect(Collectors.joining(" "));
    }
    
    
    public static String shorten(String input, int maxLength) {
        if (input == null || input.length() <= maxLength) {
            return input;
        }
        
        // Step 1: Remove spaces and hyphens
        String s1 = input.replaceAll("[\\s\\-]", "");
        if (s1.length() <= maxLength) {
            return s1;
        }
        
        // Step 2: Remove non-word-starting vowels one at a time, back to front
        // Work on original input to preserve word boundaries, strip spaces after each removal
        StringBuilder working = new StringBuilder(input);
        int i = working.length() - 1;
        while (i >= 0 && working.length() > maxLength) {
            char c = working.charAt(i);
            if ("aeiouAEIOU".indexOf(c) >= 0) {
                // Check if this vowel starts a word (i==0 or preceded by a space/hyphen)
                boolean startsWord = i == 0 || working.charAt(i - 1) == ' ' || working.charAt(i - 1) == '-';
                if (!startsWord) {
                    working.deleteCharAt(i);
                }
            }
            i--;
        }
        // Now remove spaces/hyphens and recheck
        String s2 = working.toString().replaceAll("[\\s\\-]", "");
        if (s2.length() <= maxLength) {
            return s2;
        }
        
        // Step 3: Truncate
        return s2.substring(0, maxLength);
    }
}
