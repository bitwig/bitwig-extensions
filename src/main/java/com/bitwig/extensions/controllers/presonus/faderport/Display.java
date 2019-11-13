package com.bitwig.extensions.controllers.presonus.faderport;

import com.bitwig.extension.api.util.midi.SysexBuilder;
import com.bitwig.extension.controller.api.HardwareTextDisplay;
import com.bitwig.extension.controller.api.MidiOut;

class Display
{
   private static int TEXT_LINES = 7;

   public Display(final int channel, final String sysexHeader, final PresonusFaderPort extension)
   {
      mChannel = channel;
      mSysexHeader = sysexHeader;
      mExtension = extension;
      mTextDisplay = extension.mHardwareSurface.createHardwareTextDisplay("display" + (channel + 1),
         TEXT_LINES);
      mTextDisplay.setBounds(50 + channel * 25, 20, 40, 100);
   }

   public void setDisplayTarget(final DisplayTarget displayTarget)
   {
      assert (mDisplayTarget == null) != (displayTarget == null);

      if (mDisplayTarget != null)
      {
         for (int line = 0; line < TEXT_LINES; line++)
         {
            mTextDisplay.line(line).text().setValueSupplier(null);
         }
      }

      mDisplayTarget = displayTarget;

      if (mDisplayTarget != null)
      {
         for (int line = 0; line < TEXT_LINES; line++)
         {
            final int finalLine = line;
            mTextDisplay.line(line).text().setValueSupplier(() -> displayTarget.getText(finalLine));
         }
      }
   }

   public void updateHardware()
   {
      final DisplayTarget target = mDisplayTarget != null ? mDisplayTarget : NULL_TARGET;

      final int barValue = target.getBarValue();
      final ValueBarMode valueBarMode = target.getValueBarMode();

      final MidiOut midiOutPort = mExtension.getMidiOutPort(0);
      if (valueBarMode != mLastValueBarMode || barValue != mLastBarValue)
      {
         if (mChannel >= 8)
         {
            midiOutPort.sendMidi(0xB0, 0x40 + mChannel - 8, barValue);
            midiOutPort.sendMidi(0xB0, 0x48 + mChannel - 8, valueBarMode.ordinal());
         }
         else
         {
            midiOutPort.sendMidi(0xB0, 0x30 + mChannel, barValue);
            midiOutPort.sendMidi(0xB0, 0x38 + mChannel, valueBarMode.ordinal());
         }
      }

      final DisplayMode mode = target.getMode();

      if (mode != mLastMode)
      {
         final SysexBuilder sb = SysexBuilder.fromHex(mSysexHeader);
         sb.addHex("13");
         sb.addByte(mChannel);
         final int m = mode.ordinal() & 0xF;
         sb.addByte(m);
         midiOutPort.sendSysex(sb.terminate());
      }

      for (int line = 0; line < TEXT_LINES; line++)
      {
         String text = mTextDisplay.line(line).text().currentValue();

         if (text == null)
            text = "";
         int flags = target.getTextAlignment(line);

         if (target.isTextInverted(line))
         {
            flags |= 4;
         }

         if (mLastText[line] == null || !text.equals(mLastText[line]) || flags != mLastFlags[line])
         {
            mLastText[line] = text;
            mLastFlags[line] = flags;

            final SysexBuilder sb = SysexBuilder.fromHex(mSysexHeader);
            sb.addHex("12");
            sb.addByte(mChannel);
            sb.addByte(line);
            sb.addByte(flags);
            sb.addString(text, Math.min(7, text.length()));

            midiOutPort.sendSysex(sb.terminate());
         }
      }

      mLastMode = mode;
      mLastBarValue = barValue;
      mLastValueBarMode = valueBarMode;
   }

   private final int mChannel;

   private DisplayMode mLastMode;

   private int mLastBarValue;

   private ValueBarMode mLastValueBarMode;

   private final String mSysexHeader;

   private final String[] mLastText = new String[TEXT_LINES];

   private int[] mLastFlags = new int[TEXT_LINES];

   private final HardwareTextDisplay mTextDisplay;

   private final PresonusFaderPort mExtension;

   private DisplayTarget mDisplayTarget;

   private static final DisplayTarget NULL_TARGET = new DisplayTarget()
   {
      @Override
      public int getBarValue()
      {
         return 0;
      }
   };
}
