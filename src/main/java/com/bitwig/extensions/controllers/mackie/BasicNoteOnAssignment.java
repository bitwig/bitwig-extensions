package com.bitwig.extensions.controllers.mackie;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;

public enum BasicNoteOnAssignment implements NoteAssignment {
   PLAY(94), //
   STOP(93), //
   RECORD(95), //
   REWIND(91), //
   FFWD(92), //
   AUTO_WRITE(75), //
   AUTO_READ_OFF(74), //
   TRIM(76),
   TOUCH(77),
   LATCH(78),
   GROUP(79), //
   SOLO_BASE(8),
   REC_BASE(0),
   MUTE_BASE(16),
   SELECT_BASE(24), //
   ENC_PRESS_BASE(32), //
   SIGNAL_BASE(104), //
   TOUCH_VOLUME(104), //
   SHIFT(70), //
   OPTION(71), //
   CONTROL(72), //
   ALT(73), //
   UNDO(81), //
   SAVE(80), // ICON VST
   CANCEL(82),
   ENTER(83), //
   MARKER(84),
   NUDGE(85),
   CYCLE(86),
   DROP(87),
   REPLACE(88), //
   CLICK(89),
   SOLO(90), //
   FLIP(50), //
   DISPLAY_NAME(52),
   DISPLAY_SMPTE(53), //
   BEATS_MODE(114),
   SMPTE_MODE(113), //
   V_TRACK(40),
   V_SEND(41),
   V_PAN(42),
   V_PLUGIN(43),
   V_EQ(44),
   V_INSTRUMENT(45), //
   F1(54),
   F2(55),
   F3(56),
   F4(57),
   F5(58),
   F6(59),
   F7(60),
   F8(61), //
   CURSOR_UP(96),
   CURSOR_DOWN(97),
   CURSOR_LEFT(98),
   CURSOR_RIGHT(99), //
   ZOOM(100),
   SCRUB(101), //
   BANK_LEFT(46),
   BANK_RIGHT(47), //
   TRACK_LEFT(48), //
   TRACK_RIGHT(49), //
   GLOBAL_VIEW(51), //
   GV_MIDI_LF1(62), //
   GV_INPUTS_LF2(63), //
   GV_AUDIO_LF3(64), //
   GV_INSTRUMENT_LF4(65), //
   GV_AUX_LF5(66), //
   GV_BUSSES_LF6(67), //
   GV_OUTPUTS_LF7(68), //
   GV_USER_LF8(69), //
   GV_USER_LF8_G2(51),
   REDO(71), //
   STEP_SEQ(115), // is only overridden
   CLIP_OVERDUB(116); // is only overidden


   private final int notNr;
   private final int channel;

   BasicNoteOnAssignment(final int noteNo, final int channel) {
      notNr = noteNo;
      this.channel = channel;
   }

   BasicNoteOnAssignment(final int notNo) {
      notNr = notNo;
      channel = 0;
   }

   @Override
   public int getNoteNo() {
      return notNr;
   }

   @Override
   public int getType() {
      return Midi.NOTE_ON | channel;
   }

   @Override
   public int getChannel() {
      return channel;
   }

   @Override
   public void holdActionAssign(final MidiIn midiIn, final HardwareButton button) {
      button.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(channel, notNr));
      button.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(channel, notNr));
   }

   @Override
   public void pressActionAssign(final MidiIn midiIn, final HardwareButton button) {
      button.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(channel, notNr));
   }

   @Override
   public void send(final MidiOut midiOut, final int value) {
      midiOut.sendMidi(Midi.NOTE_ON | channel, notNr, value);
   }

}
