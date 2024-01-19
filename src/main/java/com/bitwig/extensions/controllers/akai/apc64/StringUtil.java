package com.bitwig.extensions.controllers.akai.apc64;

public class StringUtil {
    private static final char[] SPECIALS = {'ä', 'ü', 'ö', 'Ä', 'Ü', 'Ö', 'ß', 'é', 'è', 'ê', 'â', 'á', 'à', //
            'û', 'ú', 'ù', 'ô', 'ó', 'ò'};
    private static final String[] REPLACE = {"a", "u", "o", "A", "U", "O", "ss", "e", "e", "e", "a", "a", "a", //
            "u", "u", "u", "o", "o", "o"};

    public static String nextValue(final String currentValue, final String[] list, final int inc, final boolean wrap) {
        int index = -1;
        for (int i = 0; i < list.length; i++) {
            if (currentValue.equals(list[i])) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            final int next = index + inc;
            if (next >= 0 && next < list.length) {
                return list[next];
            } else if (wrap) {
                index = next < 0 ? list.length - 1 : next >= list.length ? 0 : next;
            }
            return list[index];
        }
        return list[0];
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


}
