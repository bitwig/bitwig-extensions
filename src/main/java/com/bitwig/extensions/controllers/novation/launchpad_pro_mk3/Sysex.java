package com.bitwig.extensions.controllers.novation.launchpad_pro_mk3;

public class Sysex {
    // SYSEX
    public final String DAW_MODE = "F0 00 20 29 02 0E 10 01 F7";
    public final String STANDALONE_MODE = "F0 00 20 29 02 0E 10 00 F7";

    public final String SESSION_LAYOUT = "F0 00 20 29 02 0E 00 00 00 00 F7";
    public final String SESSION_MODE_PREFIX = "f0002029020e00000000";

    public final String PRINT_TO_CLIP_ON = "F0 00 20 29 02 0E 18 01 F7";
    public final String PRINT_TO_CLIP_OFF = "F0 00 20 29 02 0E 18 00 F7";

    public final String DAW_VOLUME_FADER = "F0 00 20 29 02 0E 01 00 00 00 00 00 00 01 00 01 00 02 00 02 00 03 00 03 00 04 00 04 00 05 00 05 00 06 00 06 00 07 00 07 00 F7";
    public final String DAW_PAN_FADER = "F0 00 20 29 02 0E 01 01 01 00 01 08 00 01 01 09 00 02 01 0A 00 03 01 0B 00 04 01 0C 00 05 01 0D 00 06 01 0E 00 07 01 0F 00 F7";
    public final String DAW_SENDS_FADER = "F0 00 20 29 02 0E 01 02 00 00 00 10 00 01 00 11 00 02 00 12 00 03 00 13 00 04 00 14 00 05 00 15 00 06 00 16 00 07 00 17 00 F7";
    public final String DAW_DEVICE_FADER = "F0 00 20 29 02 0E 01 03 00 00 00 18 00 01 00 19 00 02 00 1A 00 03 00 1B 00 04 00 1C 00 05 00 1D 00 06 00 1E 00 07 00 1F 00 F7";

    public final String DAW_FADER_ON = "F0 00 20 29 02 0E 00 01";
    public final String DAW_FADER_OFF = "F0 00 20 29 02 0E 00 00";

    public final String DAW_VOLUME = " 00 00 F7";
    public final String DAW_PAN = " 01 00 F7";
    public final String DAW_SENDS = " 02 00 F7";
    public final String DAW_DEVICE = " 03 00 F7";

    public final String DAW_DRUM = "F0 00 20 29 02 0E 00 02 F7";
    public final String DAW_NOTE = "F0 00 20 29 02 0E 00 01 F7";
    public final String CHORD_MODE_PREFIX = "f0002029020e00020000";
    public final String NOTE_MODE_PREFIX = "f0002029020e00040000";

    public final String PRINT_TO_CLIP_PREFIX = "f0002029020e0302000000000000000";

    public Sysex() {

    }

}
