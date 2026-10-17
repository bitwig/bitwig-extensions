package com.bitwig.extensions.controllers.akai.mpkmk4;

import com.bitwig.extension.api.Color;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkDisplayFont;

public final class MpkSysExCommands {
    public static final int CMD_DISPLAY_STRING = 0x10;
    public static final int CMD_DISPLAY_COLOR = 0x11;
    public static final int CMD_DISPLAY_ROW = 0x14;
    public static final int CMD_SCREEN_OWNER = 0x1C;
    public static final int CMD_PAD_MODE = 0x2A;
    public static final int CMD_PADS_DISABLED = 0x2B;
    public static final int CMD_PLUGIN_DAW = 0x2D;
    public static final int CMD_PAD_NOTES = 0x2E;

    private static final int HEADER_LENGTH = 7; // F0 47 00 <pid> <cmd> <lenMsb> <lenLsb>

    public final byte productId;

    public final String deviceResponseHeader;
    public final String inSysexHeader;

    public final String inScreenStatus;
    public final String inNksSysex;

    public final byte[] setScreenOwner;
    public final byte[] setScreenFw;
    public final byte[] getPadMode;
    public final byte[] disablePads;

    public MpkSysExCommands(byte productId) {
        this.productId = productId;
        setScreenOwner = command(CMD_SCREEN_OWNER, 0x02);
        setScreenFw = command(CMD_SCREEN_OWNER, 0x00);
        getPadMode = command(CMD_PAD_MODE);
        disablePads = command(CMD_PADS_DISABLED, 0x01);

        deviceResponseHeader = "f07e7f060247%02x0019".formatted(productId);
        inSysexHeader = "f0477f%02x".formatted(productId);

        inScreenStatus = inSysexHeader + "190011";
        String nksHeader = "f04700%02x19".formatted(productId);
        this.inNksSysex = nksHeader + "0011";
        // f0477f032d000101f7
        //1900110200000000000000000000000000000000f7
    }

    /**
     * Allocates a complete message F0 47 00 pid cmd 00 len [payload] F7 with the payload left zeroed.
     */
    private byte[] allocate(final int commandId, final int payloadLength) {
        final byte[] data = new byte[HEADER_LENGTH + payloadLength + 1];
        data[0] = (byte) 0xF0;
        data[1] = (byte) 0x47;
        data[2] = (byte) 0x00;
        data[3] = productId;
        data[4] = (byte) commandId;
        data[5] = (byte) ((payloadLength >> 7) & 0x7F);
        data[6] = (byte) (payloadLength & 0x7F);
        data[data.length - 1] = (byte) 0xF7;
        return data;
    }

    public byte[] command(final int commandId, final int... payload) {
        final byte[] data = allocate(commandId, payload.length);
        for (int i = 0; i < payload.length; i++) {
            data[HEADER_LENGTH + i] = (byte) (payload[i] & 0x7F);
        }
        return data;
    }

    public byte[] setPadMode(final int mode) {
        return command(CMD_PAD_MODE, mode);
    }

    public byte[] setDisplayColor(final int line, final int color) {
        return command(CMD_DISPLAY_COLOR, line, color);
    }

    public byte[] setPadNotesEnabled(final boolean enabled) {
        return command(CMD_PAD_NOTES, enabled ? 0x00 : 0x01);
    }

    /**
     * @param asciiText text already reduced to 7-bit ASCII
     */
    public byte[] setDisplayString(final int line, final MpkDisplayFont font, final String asciiText) {
        final int length = asciiText.length();
        final byte[] data = allocate(CMD_DISPLAY_STRING, length + 3);
        data[HEADER_LENGTH] = (byte) font.getValue();
        data[HEADER_LENGTH + 1] = (byte) line;
        for (int i = 0; i < length; i++) {
            data[HEADER_LENGTH + 2 + i] = (byte) (asciiText.charAt(i) & 0x7F);
        }
        // trailing 00 terminator is already zero
        return data;
    }

    public byte[] configureLine(final MpkDisplayFont font, final int lineIndex, final int justification,
        final Color foreGround, final Color background) {
        return command(CMD_DISPLAY_ROW, //
            font.getValue(), lineIndex, justification, //
            foreGround.getRed255() >> 3, foreGround.getGreen255() >> 2, foreGround.getBlue255() >> 3, //
            background.getRed255() >> 3, background.getGreen255() >> 2, background.getBlue255() >> 3);
    }
}
