package com.bitwig.extensions.controllers.presonus.faderport;

import com.bitwig.extension.api.util.midi.SysexBuilder;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.presonus.framework.ControlElement;

public class Display implements ControlElement<DisplayTarget>
{
   private static int TEXT_LINES = 7;
   public Display(final int channel, final String sysexHeader)
   {
      mChannel = channel;
      mSysexHeader = sysexHeader;
   }

   @Override
   public void onMidi(final DisplayTarget target, final int status, final int data1, final int data2)
   {
   }

   @Override
   public void flush(final DisplayTarget target, final MidiOut midiOut)
   {
      int barValue = target.getBarValue();
      ValueBarMode valueBarMode = target.getValueBarMode();

      if (valueBarMode != mLastValueBarMode || barValue != mLastBarValue)
      {
         if (mChannel >= 8)
         {
            midiOut.sendMidi(0xB0, 0x40 + mChannel - 8, barValue);
            midiOut.sendMidi(0xB0, 0x48 + mChannel - 8, valueBarMode.ordinal());
         }
         else
         {
            midiOut.sendMidi(0xB0, 0x30 + mChannel, barValue);
            midiOut.sendMidi(0xB0, 0x38 + mChannel, valueBarMode.ordinal());
         }
      }

      DisplayMode mode = target.getMode();

      if (mode != mLastMode)
      {
         SysexBuilder sb = SysexBuilder.fromHex(mSysexHeader);
         sb.addHex("13");
         sb.addByte(mChannel);
         int m = mode.ordinal() & 0xF;
         sb.addByte(m);
         midiOut.sendSysex(sb.terminate());
      }

      for(int line = 0; line< TEXT_LINES; line++)
      {
         String text = target.getText(line);
         int flags = target.getTextAlignment(line);

         if (target.isTextInverted(line))
         {
            flags |= 4;
         }

         if (mLastText[line] == null || !text.equals(mLastText[line]) || flags != mLastFlags[line])
         {
            mLastText[line] = text;
            mLastFlags[line] = flags;

            SysexBuilder sb = SysexBuilder.fromHex(mSysexHeader);
            sb.addHex("12");
            sb.addByte(mChannel);
            sb.addByte(line);
            sb.addByte(flags);
            sb.addString(text, Math.min(7, text.length()));

            midiOut.sendSysex(sb.terminate());
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
}
