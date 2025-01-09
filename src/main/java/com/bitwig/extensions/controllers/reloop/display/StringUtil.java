package com.bitwig.extensions.controllers.reloop.display;

public class StringUtil {
    private static final char[] SPECIALS = {
        'ä', 'ü', 'ö', 'Ä', 'Ü', 'Ö', 'ß', 'é', 'è', 'ê', 'â', 'á', 'à', //
        'û', 'ú', 'ù', 'ô', 'ó', 'ò'
    };
    private static final char[] REPLACE = {
        'a', 'u', 'o', 'A', 'U', 'O', 'S', 'e', 'e', 'e', 'a', 'a', 'a', //
        'u', 'u', 'u', 'o', 'o', 'o'
    };
    
    public static int toValue(final char c) {
        if (c >= 0 && c < 128) {
            return c;
        }
        for (int i = 0; i < SPECIALS.length; i++) {
            if (c == SPECIALS[i]) {
                return REPLACE[i];
            }
        }
        return 32;
    }
    
}
