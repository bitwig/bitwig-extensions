package com.bitwig.extensions.controllers.novation.launchkey_mk3;

import com.bitwig.extension.controller.api.HardwareActionMatcher;
import com.bitwig.extension.controller.api.MidiIn;

public enum LabelCcAssignments {
   RECORD_ARM_UNDO(1), //
   MUTE_REDO(2), //
   SOLO_CLICK(3), //
   VOLUME(4), //
   PAN(5), //
   SENDS_TAP(6), //
   DEVICE_TEMPO(7), //
   STOP_CLIP_SWING(8), //
   SHIFT(90), //
   TRACK_SEL_1(101), //
   TRACK_SEL_2(102), //
   TRACK_SEL_3(103), //
   TRACK_SEL_4(104), //
   TRACK_SEL_5(105), //
   TRACK_SEL_6(106), //
   TRACK_SEL_7(107), //
   TRACK_SEL_8(107), //
   R1_PATTERNS(89), //
   R2_STEPS(79), //
   R3_PAT_SETTINGS(69), //
   R4_VELOCITY(59), //
   R5_PROBABILITY(49), //
   R6_MUTATION(39), //
   R7_MICROSTEP(29), //
   R8_PRINT_TO_CLIP(19), //
   REC(10), //
   PLAY(20), //
   FIXED_LENGTH(30), //
   QUANTIZE(40), //
   DUPLICATE(50),
   CLEAR(60),
   DOWN(70),
   UP(80),
   LEFT(91), //
   RIGHT(92), //
   SESSION(93), //
   NOTE(94), //
   CHORD(95), //
   CUSTOM(96), //
   SEQUENCER(97), //
   PROJECTS(98);

   private final int ccValue;

   LabelCcAssignments(final int ccValue) {
      this.ccValue = ccValue;
   }

   public int getCcValue() {
      return ccValue;
   }

   public HardwareActionMatcher createMatcher(final MidiIn midiIn, final int matchValue) {
      return midiIn.createCCActionMatcher(0, ccValue, matchValue);
   }
}
