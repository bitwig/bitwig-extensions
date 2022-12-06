package com.bitwig.extensions.controllers.nativeinstruments.komplete;

public class Midi {
	public static final int NOTE_OFF = 128;
	public static final int NOTE_ON = 144;
	public static final int POLY_AT = 160;
	public static final int CC = 176;
	public static final int PROG_CHANGE = 192;
	public static final int CHANNEL_AH = 208;
	public static final int PITCH_BEND = 224;
	public static final int SYS_EX_START = 0xF0;
	public static final int SYS_EX_END = 0XF7;
	public static final int SNYC_CLOCK = 248;
	public static final int SNYC_START = 250;
	public static final int SNYC_STOP = 252;
	public static final int SNYC_CONTINUE = 251;
	public static final int KK_DAW = 0xBF;

	public static final int HELLO = 0x1;
	public static final int GOODBYE = 0x2;
}
