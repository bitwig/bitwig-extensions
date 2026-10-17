package com.bitwig.extensions.controllers.akai.mpkmk4;

import java.util.Arrays;

/**
 * Incoming SysEx message decoded from the hex string Bitwig delivers (e.g. "f0477f032d000101f7").
 * The leading F0 and trailing F7 are stripped, so index 0 is the manufacturer id.
 * <p>
 * Akai layout (after stripping): 47 [deviceId] [productId] [command] [lenHi] [lenLo] [payload...]
 */
public record SysExMessage(int[] data) {

    public static final int SYSEX_START = 0xF0;
    public static final int SYSEX_END = 0xF7;
    public static final int NO_VALUE = -1;

    public static final int AKAI_ID = 0x47;
    public static final int DEVICE_ID_DAW = 0x7F;
    public static final int DEVICE_ID_NKS = 0x00;

    // Akai message layout (indices after F0 is stripped)
    private static final int IDX_MANUFACTURER = 0;
    private static final int IDX_DEVICE_ID = 1;
    private static final int IDX_PRODUCT_ID = 2;
    private static final int IDX_COMMAND = 3;
    private static final int IDX_LENGTH_HIGH = 4;
    private static final int IDX_LENGTH_LOW = 5;
    private static final int IDX_PAYLOAD = 6;
    private static final int AKAI_HEADER_LENGTH = IDX_PAYLOAD;
    private static final int SEVEN_BIT_SHIFT = 7;

    // Universal identity reply: 7E [channel] 06 02 [manufacturer] [family LSB] [family MSB] [model LSB] ...
    private static final int UNIVERSAL_NON_REALTIME = 0x7E;
    private static final int GENERAL_INFORMATION = 0x06;
    private static final int IDENTITY_REPLY = 0x02;
    private static final int AKAI_FAMILY_MSB = 0x00;
    private static final int AKAI_MODEL_LSB = 0x19;
    private static final int IDX_INQUIRY_SUB_ID1 = 0;
    private static final int IDX_INQUIRY_SUB_ID2 = 2;
    private static final int IDX_INQUIRY_SUB_ID3 = 3;
    private static final int IDX_INQUIRY_MANUFACTURER = 4;
    private static final int IDX_INQUIRY_FAMILY_LSB = 5;
    private static final int IDX_INQUIRY_FAMILY_MSB = 6;
    private static final int IDX_INQUIRY_MODEL_LSB = 7;
    private static final int INQUIRY_MIN_LENGTH = IDX_INQUIRY_MODEL_LSB + 1;

    private static final int HEX_RADIX = 16;
    private static final int NIBBLE_SHIFT = 4;
    private static final int HEX_CHARS_PER_BYTE = 2;

    public static SysExMessage parse(final String hex) {
        final int len = hex.length();
        final int[] buffer = new int[len / HEX_CHARS_PER_BYTE];
        int count = 0;
        int i = 0;
        while (i + 1 < len) {
            final int hi = Character.digit(hex.charAt(i), HEX_RADIX);
            if (hi < 0) { // skip separators such as spaces
                i++;
                continue;
            }
            final int lo = Character.digit(hex.charAt(i + 1), HEX_RADIX);
            buffer[count++] = hi << NIBBLE_SHIFT | lo;
            i += HEX_CHARS_PER_BYTE;
        }
        final int start = count > 0 && buffer[0] == SYSEX_START ? 1 : 0;
        final int end = count > start && buffer[count - 1] == SYSEX_END ? count - 1 : count;
        return new SysExMessage(Arrays.copyOfRange(buffer, start, end));
    }

    public int length() {
        return data.length;
    }

    /**
     * @return unsigned byte at index or -1 if out of range
     */
    public int get(final int index) {
        return index >= 0 && index < data.length ? data[index] : NO_VALUE;
    }

    public boolean startsWith(final int... prefix) {
        if (prefix.length > data.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    // ---- Akai specific view ----

    public boolean isAkai(final int productId) {
        return data.length >= AKAI_HEADER_LENGTH && get(IDX_MANUFACTURER) == AKAI_ID && get(IDX_PRODUCT_ID) == productId;
    }

    /**
     * @return {@link #DEVICE_ID_DAW} for regular DAW messages, {@link #DEVICE_ID_NKS} for NKS messages
     */
    public int deviceId() {
        return get(IDX_DEVICE_ID);
    }

    public int command() {
        return get(IDX_COMMAND);
    }

    public int payloadLength() {
        return get(IDX_LENGTH_HIGH) << SEVEN_BIT_SHIFT | get(IDX_LENGTH_LOW);
    }

    /**
     * @return unsigned payload byte at index or -1 if out of range
     */
    public int payload(final int index) {
        return get(IDX_PAYLOAD + index);
    }

    // ---- Universal device inquiry ----

    /**
     * Universal identity reply: 7E [channel] 06 02 47 [productId] 00 19 ...
     */
    public boolean isDeviceInquiryResponse(final int productId) {
        return data.length >= INQUIRY_MIN_LENGTH //
            && get(IDX_INQUIRY_SUB_ID1) == UNIVERSAL_NON_REALTIME //
            && get(IDX_INQUIRY_SUB_ID2) == GENERAL_INFORMATION //
            && get(IDX_INQUIRY_SUB_ID3) == IDENTITY_REPLY //
            && get(IDX_INQUIRY_MANUFACTURER) == AKAI_ID //
            && get(IDX_INQUIRY_FAMILY_LSB) == productId //
            && get(IDX_INQUIRY_FAMILY_MSB) == AKAI_FAMILY_MSB //
            && get(IDX_INQUIRY_MODEL_LSB) == AKAI_MODEL_LSB;
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof SysExMessage other && Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(data.length * 3);
        for (final int b : data) {
            sb.append("%02X ".formatted(b));
        }
        return sb.toString().trim();
    }
}
