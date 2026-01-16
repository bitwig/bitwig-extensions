package com.bitwig.extensions.controllers.expressivee.common;

public class Constant {
    // ===== MIDI PROTOCOL =====
    final static int MIDI_CHANNEL_1 = 0; // MIDI Channel Number 1 = MIDI Channel ID 0 (in code)
    final static int MIDI_CC_STATUS_BYTE = 0xB0;
    final static int MIDI_SYSEX_END = 0xF7;
    final static int MIDI_VALUE_OFF = 0;
    final static int MIDI_VALUE_ON = 127;
    final static int MIDI_VALUE_MAX = 127;
    // ===== CC ASSIGNMENTS =====
    final static int PLAYHEAD_CC = 16;
    final static int MARKER_MOVE_CC = 17;
    final static int LOOP_ENABLE_CC = 18;
    final static int LOOP_LENGTH_CC = 19;
    final static int LOOP_START_CC = 20;
    final static int TRACK_NAVIGATION_CC = 21;
    final static int SCENE_NAVIGATION_CC = 22;
    final static int LAUNCHING_CLIP_CC = 23;
    final static int TRACK_VOLUME_CC = 24;
    final static int TRACK_PAN_CC = 25;
    final static int TRACK_MUTE_CC = 26;
    final static int TRACK_SOLO_CC = 27;
    final static int TRACK_SEND_A_CC = 28;
    final static int TRACK_SEND_B_CC = 29;
    final static int MIDI_CAPTURE_CC = 30;
    final static int UNDO_CC = 31;
    final static int REDO_CC = 32;
    final static int QUANTIZE_CC = 33;
    final static int BPM_VALUE_CC = 34;
    final static int BPM_TAP_CC = 35;
    final static int TRANSPORT_RECORD_CC = 36;
    final static int TRANSPORT_PLAY_PAUSE_CC = 37;
    final static int TRANSPORT_STOP_CC = 38;
    final static int TRANSPORT_METRONOME_ENABLE_CC = 39;
    final static int OPEN_GUI_CC = 40;
    final static int MACRO1_CC = 41;
    final static int MACRO2_CC = 42;
    final static int MACRO3_CC = 43;
    final static int MACRO4_CC = 44;
    final static int MACRO5_CC = 45;
    final static int MACRO6_CC = 46;
    final static int MACRO7_CC = 47;
    final static int MACRO8_CC = 48;
    final static int ADD_MARKER_CC = 50;
    final static int MASTER_VOLUME_CC = 51;
    final static int DEVICE_NAVIGATION_CC = 52;
    final static int CTRL_E_DEVICE_ON_TRACK_CC = 53;
    // ===== SYSEX PROTOCOL =====
    final static byte[] SYSEX_HEADER = new byte[] { (byte) 0xf0, 0x00, 0x21, 0x26 };
    final static int SYSEX_STRING_TERMINATOR = 0;
    // ===== SYSEX ASSIGNMENT =====
    final static int SYSEX_CONNECTION = 1;
    final static int SYSEX_BPM_VALUE = 2;
    final static int SYSEX_TRACK_NAME = 3;
    final static int SYSEX_TRACK_VOLUME = 4;
    final static int SYSEX_TRACK_PAN = 5;
    final static int SYSEX_TRACK_SEND_A = 6;
    final static int SYSEX_TRACK_SEND_B = 7;
    final static int SYSEX_PLUGIN_NAME = 8;
    final static int SYSEX_MACRO_NAME = 9;
    final static int SYSEX_MACRO_VALUE = 10;
    // ===== ENCODING =====
    final static int TWOS_COMPLEMENT_7BIT = 7; // Standard MIDI data resolution
    final static double MIDI_TO_NORMALIZED = 1.0 / 127.0; // Convert MIDI to 0.0-1.0
    // ===== BITWIG CLIP GRID =====
    final static int CLIP_GRID_WIDTH = 16; // 16 steps = 1 measure 4/4 in 1/16th notes
    final static int CLIP_GRID_HEIGHT = 128; // Full MIDI note range (0-127)
    // ===== QUANTIZATION =====
    final static double QUANTIZE_FULL = 1.0; // 100% quantization
    // ===== REMOTE CONTROLS =====
    final static int MACRO_BANK_MAIN = 0;
    final static int MACRO_COUNT = 8; // Standard 8 macro controls
    // ===== DAW CONNECTION =====
    final static String DAW_NAME_BITWIG = "Bitwig";
    final static int DAW_PROTOCOL_VERSION = 1;
}
