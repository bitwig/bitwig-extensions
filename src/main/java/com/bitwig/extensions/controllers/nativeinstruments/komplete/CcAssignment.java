package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.api.HardwareActionMatcher;
import com.bitwig.extension.controller.api.MidiIn;

import java.util.Optional;

/**
 * Midi CC assignment constants.
 */
public enum CcAssignment {
   PLAY(0x10), //
   RESTART(0x11), //
   REC(0x12), //
   COUNT_IN(0x13), //
   STOP(0x14), //
   CLEAR(0x15), //
   LOOP(0x16), //
   METRO(0x17), //
   TAP_TEMPO(0x18), //
   UNDO(0x20), //
   REDO(0x21), //
   QUANTIZE(0x22), //
   AUTO(0x23), //
   PRESS_4D_KNOB(0x60), //
   PRESS_4D_KNOB_SHIFT(0x61), //
   VOLUME_CURRENT(0x64), //
   PAN_CURRENT(0x65), //
   MUTE_CURRENT(0x66), //
   SOLO_CURRENT(0x67);
   private int stateId;

   CcAssignment(final int stateId) {
      this.stateId = stateId;
   }

   public int getStateId() {
      return stateId;
   }

   public Optional<CcAssignment> fromMidi(final int value) {
      for (final CcAssignment cc : CcAssignment.values()) {
         if (cc.stateId == value) {
            return Optional.of(cc);
         }
      }
      return Optional.empty();
   }

   public HardwareActionMatcher createActionMatcher(final MidiIn midiIn, final int matchvalue) {
      return midiIn.createCCActionMatcher(15, stateId, matchvalue);
   }

}
