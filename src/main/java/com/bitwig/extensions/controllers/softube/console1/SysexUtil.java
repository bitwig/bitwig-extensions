package com.bitwig.extensions.controllers.softube.console1;

import java.io.ByteArrayOutputStream;

public class SysexUtil {
    //private static final int[] PREFIX_MATCH = {0x7d, 0x73, 0x74, 0x63, 0x31};
    private static final String MATCH = "}stc1";
    
    public static final String INIT_HANDSHAKE = """
        {
            "handshake": {
                "dawName": "Bitwig",
                "protocolVersion":  [1,2]
            }
        }
        """;

    public static final String RESET_CMD = "{\"cmd\":\"RESET\"}";

    private static void appendByte(final StringBuilder sb, final char c) {
        if (c < 127) {
            final int b1 = c >> 4;
            final int b2 = c & 0xf;
            final char c1 = (char) (b1 < 10 ? b1 + '0' : (b1 - 10) + 'A');
            final char c2 = (char) (b2 < 10 ? b2 + '0' : (b2 - 10) + 'A');
            sb.append(c1);
            sb.append(c2);
            sb.append(' ');
        }
    }

    private static void appendByte(final StringBuilder sb, final String s) {
        for (int i = 0; i < s.length(); i++) {
            appendByte(sb, s.charAt(i));
        }
    }

    private static void appendByte(final ByteArrayOutputStream bos, final String s) {
        for (int i = 0; i < s.length(); i++) {
            bos.write(s.charAt(i));
        }
    }

    public static String toSysexStringAcii(final String s) {
        final StringBuilder sb = new StringBuilder("F0 7D ");
        appendByte(sb, "stc1");
        boolean inQuote = false;
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c > 127) {
                appendByte(sb, "\\u" + Integer.toHexString(c | 0x10000).substring(1));
            } else if (c == 10 || (c == 32 && !inQuote)) {
                //
            } else if (c == 34) {
                appendByte(sb, c);
                inQuote = !inQuote;
            } else {
                appendByte(sb, c);
            }
        }
        sb.append("F7");
        return sb.toString();
    }

    public static byte[] toJsonSysEx(final String s) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(0xF0);
        bos.write(0x7d);
        appendByte(bos, "stc1");
        boolean inQuote = false;
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);

            if (c > 127) {
                appendByte(bos, "\\u" + Integer.toHexString(c | 0x10000).substring(1));
            } else if (c == 10 || (c == 32 && !inQuote)) {
                //
            } else if (c == 34) {
                bos.write(c);
                inQuote = !inQuote;
            } else {
                bos.write(c);
            }
        }
        bos.write(0xF7);
        return bos.toByteArray();
    }

    public static String toSysExJson(final String s) {
        final StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c > 127) {
                sb.append("\\u" + Integer.toHexString(c | 0x10000).substring(1));
            } else if (c == 10 || (c == 32 && !inQuote)) {
                //
            } else if (c == 34) {
                sb.append(c);
                inQuote = !inQuote;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static int toInt(char c) {
        if (c <= '9') {
            return c - '0';
        }
        return c - 'a' + 10;
    }
    
    
    public  static String toJson(String sysEx) {
        StringBuilder sb = new StringBuilder();
        for (int pos = 0; pos < sysEx.length(); pos += 2) {
            int value = (SysexUtil.toInt(sysEx.charAt(pos)) << 4) + SysexUtil.toInt(sysEx.charAt(pos + 1));
            sb.append((char) value);
        }
        final int index = sb.lastIndexOf(MATCH);
        if(index != -1) {
            return sb.substring(index);
        }
        return sb.toString();
    }
}
