package com.bitwig.extensions.controllers.arturia.keylab.mk3;

public class SysExUtil {
    private SysExUtil() {
    }
    
    public static char convert(final char c) {
        if (c < 128) {
            return c;
        }
        switch (c) {
            case 'Á':
            case 'À':
            case 'Ä':
                return 'A';
            case 'É':
            case 'È':
                return 'E';
            case 'á':
            case 'à':
            case 'ä':
                return 'a';
            case 'Ö':
                return 'O';
            case 'Ü':
                return 'U';
            case 'è':
            case 'é':
                return 'e';
            case 'ö':
                return 'o';
            case 'ü':
                return 'u';
            case 'ß':
                return 's';
        }
        return '?';
    }
}
