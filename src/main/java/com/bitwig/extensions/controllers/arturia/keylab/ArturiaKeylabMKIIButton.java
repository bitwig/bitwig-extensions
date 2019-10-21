package com.bitwig.extensions.controllers.arturia.keylab;

public enum ArturiaKeylabMKIIButton
{
   CHORD(0x12),
   TRANS(0x13),
   OCT_MINUS(0x10),
   OCT_PLUS(0x11),
   PAD(0x17),
   CHORD_MEMORY(0x16),
   CHORD_TRANSPOSE(0x15),
   MIDI_CH(0x14),
   PAD1(0x70),
   PAD2(0x71),
   PAD3(0x72),
   PAD4(0x73),
   PAD5(0x74),
   PAD6(0x75),
   PAD7(0x76),
   PAD8(0x77),
   PAD9(0x78),
   PAD10(0x79),
   PAD11(0x7A),
   PAD12(0x7B),
   PAD13(0x7C),
   PAD14(0x7D),
   PAD15(0x7E),
   PAD16(0x7F),
   SOLO(0x60),
   MUTE(0x61),
   RECORD_ARM(0x62),
   READ(0x63),
   WRITE(0x64),
   SAVE(0x65),
   IN(0x66),
   OUT(0x67),
   METRO(0x68),
   UNDO(0x69),
   REWIND(0x6A),
   FORWARD(0x6B),
   STOP(0x6C),
   PLAY_OR_PAUSE(0x6D),
   RECORD(0x6E),
   LOOP(0x6F),
   CATEGORY(0x18),
   PRESET(0x19),
   PRESET_PREVIOUS(0x1A),
   PRESET_NEXT(0x1B),
   ANALOG_LAB(0x1C),
   DAW(0x1D),
   USER(0x1E),
   NEXT(0x1F),
   PREVIOUS(0x20),
   BANK(0x21),
   SELECT1(0x22),
   SELECT2(0x23),
   SELECT3(0x24),
   SELECT4(0x25),
   SELECT5(0x26),
   SELECT6(0x27),
   SELECT7(0x28),
   SELECT8(0x29),
   SELECT_MULTI(0x2A);

   ArturiaKeylabMKIIButton(final int ID)
   {
      mID = ID;
   }

   public int getID()
   {
      return mID;
   }

   private final int mID;
}
