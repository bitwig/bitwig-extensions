package com.bitwig.extensions.controllers.arturia.keylab.mk2;

public enum ButtonId
{
   CHORD(0x12), TRANS(0x13), OCT_MINUS(0x10), OCT_PLUS(0x11), PAD(0x17), CHORD_MEMORY(0x16), CHORD_TRANSPOSE(
      0x15), MIDI_CH(0x14), PAD1(0x70, 36, 9), PAD2(0x71, 37, 9), PAD3(0x72, 38, 9), PAD4(0x73, 39,
         9), PAD5(0x74, 40, 9), PAD6(0x75, 41, 9), PAD7(0x76, 42, 9), PAD8(0x77, 43, 9), PAD9(0x78, 44,
            9), PAD10(0x79, 45, 9), PAD11(0x7A, 46, 9), PAD12(0x7B, 47, 9), PAD13(0x7C, 48, 9), PAD14(0x7D,
               49, 9), PAD15(0x7E, 50, 9), PAD16(0x7F, 51, 9), SOLO(0x60, 0x08), MUTE(0x61,
                  0x10), RECORD_ARM(0x62, 0x00), READ(0x63, 0x38), WRITE(0x64, 0x39), SAVE(0x65,
                     0x4A), PUNCH_IN(0x66, 0x57), PUNCH_OUT(0x67, 0x58), METRO(0x68, 0x59), UNDO(0x69,
                        0x51), REWIND(0x6A, 0x5B), FORWARD(0x6B, 0x5C), STOP(0x6C, 0x5D), PLAY_OR_PAUSE(0x6D,
                           0x5E), RECORD(0x6E, 0x5F), LOOP(0x6F, 0x56), CATEGORY(0x18, 0x65), PRESET(0x19,
                              0x64), PRESET_PREVIOUS(0x1A, 0x62), PRESET_NEXT(0x1B, 0x63), WHEEL_CLICK(0,
                                 0x54), ANALOG_LAB(0x1C), DAW(0x1D), USER(0x1E), NEXT(0x1F,
                                    0x31), PREVIOUS(0x20, 0x30), BANK(0x21, 0x21), SELECT1(0x22,
                                       0x18), SELECT2(0x23, 0x19), SELECT3(0x24, 0x1A), SELECT4(0x25,
                                          0x1B), SELECT5(0x26, 0x1C), SELECT6(0x27, 0x1D), SELECT7(0x28,
                                             0x1E), SELECT8(0x29, 0x1F), SELECT_MULTI(0x2A, 0x33);

   ButtonId(final int SysexId)
   {
      mSysexId = SysexId;
      mKey = 0;
      mChannel = 0;
   }

   ButtonId(final int SysexId, final int noteOrCC)
   {
      mSysexId = SysexId;
      mKey = noteOrCC;
      mChannel = 0;
   }

   ButtonId(final int SysexId, final int noteOrCC, final int channel)
   {
      mSysexId = SysexId;
      mKey = noteOrCC;
      mChannel = channel;
   }

   public static ButtonId drumPad(final int index)
   {
      return values()[PAD1.ordinal() + index];
   }

   public static ButtonId select(final int index)
   {
      return values()[SELECT1.ordinal() + index];
   }

   public int getSysexID()
   {
      return mSysexId;
   }

   public int getKey()
   {
      return mKey;
   }

   public int getChannel()
   {
      return mChannel;
   }

   public boolean isRGB()
   {
      final int ordinal = ordinal();

      return ordinal >= SELECT1.ordinal() && ordinal <= SELECT_MULTI.ordinal()
         || ordinal >= PAD1.ordinal() && ordinal <= PAD16.ordinal();
   }

   private final int mSysexId;

   private final int mKey;

   private final int mChannel;
}
