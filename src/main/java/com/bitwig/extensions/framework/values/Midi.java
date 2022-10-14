package com.bitwig.extensions.framework.values;

public class Midi {
    public static final int NOTE_OFF = 128;  //0x80
    public static final int NOTE_ON = 144;  // 0x90
    public static final int POLY_AT = 160;  // 0xA0
    public static final int CC = 176;    // 0xB0
    public static final int PROG_CHANGE = 192; // 0xC0
    public static final int CHANNEL_AT = 208; // 0xD0
    public static final int PITCH_BEND = 224;  //0xE0
    public static final int SYS_EX_START = 0xF0;
    public static final int SYS_EX_END = 0XF7;
    public static final int SNYC_CLOCK = 248;
    public static final int SNYC_START = 250;
    public static final int SNYC_STOP = 252;
    public static final int SNYC_CONTINUE = 251;
}
