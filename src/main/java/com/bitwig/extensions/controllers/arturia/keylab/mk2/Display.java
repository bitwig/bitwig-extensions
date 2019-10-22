package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import java.util.Arrays;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.api.util.midi.SysexBuilder;
import com.bitwig.extensions.framework.ControlElement;
import com.bitwig.extensions.framework.LayeredControllerExtension;

public class Display implements ControlElement<DisplayTarget>
{
   @Override
   public void onMidi(final DisplayTarget target, final ShortMidiMessage data)
   {
   }

   @Override
   public void flush(final DisplayTarget target, final LayeredControllerExtension extension)
   {
      String upper = target.getUpperText();
      String lower = target.getLowerText();

      byte[] data = SysexBuilder.fromHex("F0 00 20 6B 7F 42 04 00 60 01 ")
         .addString(upper, 16)
         .addHex("00 02")
         .addString(lower, 16)
         .addHex(" 00")
         .terminate();

      if (mLastData == null || !Arrays.equals(data, mLastData))
      {
         mLastData = data;
         extension.getMidiOutPort(1).sendSysex(data);
      }
   }
   private byte[] mLastData;

   public void reset()
   {
      mLastData = null;
   }
}
