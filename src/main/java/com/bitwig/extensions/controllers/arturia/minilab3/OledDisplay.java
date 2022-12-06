package com.bitwig.extensions.controllers.arturia.minilab3;

import com.bitwig.extension.controller.api.ControllerHost;

public class OledDisplay {

    private static final int OLED_TRANSIENT_DELAY = 2000;
    private static final String SYSEX_HEADER_COMP = "F0 00 20 6B 7F 42 04 02 60";
    private static final String SYSEX_END = "F7";
    public static final String SYSEX_FORMAT_OLED_PICT_UPDATE_ID_07 = "%s 1F 07 01 %02X %02X 01 00 01 %s00 02 %s00 %s";
    public static final int MAX_SLIDER_VALUE = 126;
    private Runnable updateAction;

    public enum Pict {
        NONE(0),
        HEART(1),
        PLAY(2),
        REC(3),
        ARMED(4),
        SHIFT(5);

        Pict(final int code) {
            this.code = code;
        }

        private final int code;

        public int getCode() {
            return code;
        }
    }


    private final SysExHandler sysExHandler;
    private final ControllerHost host;
    private DisplayMode acceptValue;
    private DisplayMode mainDisplayMode;
    private long acceptTime;

    public OledDisplay(final SysExHandler sysExHandler, final ControllerHost host) {
        this.sysExHandler = sysExHandler;
        this.host = host;
        acceptValue = DisplayMode.INIT;
        mainDisplayMode = acceptValue;
        acceptTime = System.currentTimeMillis();
    }

    public void notifyInit() {
        //if (acceptValue == DisplayMode.INIT) {
        host.println(" >>>>>>>>> INIT >>>>>>>>>>>> ");
        sendText(DisplayMode.INIT, "Bitwig Studio", "Connected");
        //}
    }

    public void handleTransient() {
        if (acceptValue != mainDisplayMode && (System.currentTimeMillis() - acceptTime) > OLED_TRANSIENT_DELAY) {
            acceptValue = mainDisplayMode;
            updateAction.run();
        }
    }

    public void setMainMode(final DisplayMode displayMode, final Runnable updateAction) {
        mainDisplayMode = displayMode;
        this.updateAction = updateAction;
        if (acceptValue != DisplayMode.INIT) {
            enableValues(DisplayMode.STATE_INFO);
            sendText(DisplayMode.STATE_INFO, "Window", displayMode.getText());
        }
    }

    public void enableValues(final DisplayMode mode) {
        if (acceptValue != DisplayMode.INIT) {
            acceptValue = mode;
            acceptTime = System.currentTimeMillis();
        }
    }

    public void disableValues() {
        acceptValue = mainDisplayMode;
    }

    public void clearText() {
        acceptValue = DisplayMode.INIT;
        final String express = String.format(SYSEX_FORMAT_OLED_PICT_UPDATE_ID_07, SYSEX_HEADER_COMP,
                Pict.NONE.getCode(), Pict.NONE.getCode(), toSysEx(""), toSysEx(""), SYSEX_END);
        sysExHandler.sendSysex(express);
    }

    public void sendText(final DisplayMode mode, final String text1, final String text2) {
        enableValues(mode);
        final String express = String.format(SYSEX_FORMAT_OLED_PICT_UPDATE_ID_07, SYSEX_HEADER_COMP,
                Pict.NONE.getCode(), Pict.NONE.getCode(), toSysEx(text1), toSysEx(text2), SYSEX_END);
        sysExHandler.sendSysex(express);
    }

    public void sendTextCond(final DisplayMode mode, final String text1, final String text2) {
        if (acceptValue != mode) {
            return;
        }
        final String express = String.format(SYSEX_FORMAT_OLED_PICT_UPDATE_ID_07, SYSEX_HEADER_COMP,
                Pict.NONE.getCode(), Pict.NONE.getCode(), toSysEx(text1), toSysEx(text2), SYSEX_END);
        sysExHandler.sendSysex(express);
    }


    public void sendTextInfo(final DisplayMode mode, final String text1, final String text2,
                             final boolean isTransient) {
        if (acceptValue != mode) {
            return;
        }
        final String express = String.format("%s 01 %s00 02 %s%s", SYSEX_HEADER_COMP, toSysEx(text1), toSysEx(text2),
                SYSEX_END);
        sysExHandler.sendSysex(express);
    }

    public void sendPictogramInfo(final DisplayMode mode, final Pict pic1, final Pict pic2, final String title,
                                  final String text2) {
        if (mode != acceptValue) {
            return;
        }
        final String express = String.format("%s 1F 07 02 %02X %02X 01 00 01 %s00 02 %s00 %s", SYSEX_HEADER_COMP,
                pic1.getCode(), pic2.getCode(), toSysEx(title), toSysEx(text2), SYSEX_END);
        sysExHandler.sendSysex(express);
    }

    public void sendPadInfo(final double value, final String text1, final String text2) {
        final int val = (int) (value * MAX_SLIDER_VALUE);
        final String express = String.format("%s 1F 05 00 %02X 00 00 01 %s00 02 %s%s", SYSEX_HEADER_COMP, val,
                toSysEx(text1), toSysEx(text2), SYSEX_END);
        sysExHandler.sendSysex(express);
    }

    public void sendSliderInfo(final DisplayMode mode, final double value, final String text1, final String text2) {
        if (acceptValue != mode) {
            return;
        }
        final int val = (int) (value * MAX_SLIDER_VALUE);
        final String express = String.format("%s 1F 04 02 %02X 00 00 01 %s00 02 %s%s", SYSEX_HEADER_COMP, val,
                toSysEx(text1), toSysEx(text2), SYSEX_END);
        sysExHandler.sendSysex(express);
    }

    public void sendEncoderInfo(final DisplayMode mode, final double value, final String text1, final String text2) {
        if (acceptValue != mode) {
            return;
        }
        final int val = (int) (value * MAX_SLIDER_VALUE);
        final String express = String.format("%s 1F 03 02 %02X 00 00 01 %s00 02 %s00 %s", SYSEX_HEADER_COMP, val,
                toSysEx(text1), toSysEx(text2), SYSEX_END);
        sysExHandler.sendSysex(express);
    }

    public void sendScrollInfo(final int pos, final int len, final String text1, final String text2) {
        final String express = String.format("%s 1F 06 00 %02X 00 %02X 00 00 01 %s00 02 %s00 %s", SYSEX_HEADER_COMP,
                pos, len, toSysEx(text1), toSysEx(text2), SYSEX_END);
        sysExHandler.sendSysex(express);
    }

    public static String toSysEx(final String text) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            final char c = convert(text.charAt(i));
            final String hexValue = Integer.toHexString((byte) c);
            sb.append(hexValue.length() < 2 ? "0" + hexValue : hexValue);
            sb.append(" ");
        }
        return sb.toString();
    }

    private static char convert(final char c) {
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
