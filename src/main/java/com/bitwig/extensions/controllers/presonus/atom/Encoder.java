package com.bitwig.extensions.controllers.presonus.atom;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extensions.framework.ControlElement;
import com.bitwig.extensions.framework.LayeredControllerExtension;
import com.bitwig.extensions.framework.targets.EncoderTarget;

public class Encoder implements ControlElement<EncoderTarget>
{
   public Encoder(final int data1)
   {
      mData1 = data1;
   }

   @Override
   public void onMidi(
      final EncoderTarget target, final ShortMidiMessage data)
   {
      final int status = data.getStatusByte();
      final int data1 = data.getData1();
      final int data2 = data.getData2();

      if (status == 176 && data1 == mData1)
      {
         int diff = data2 & 0x3f;
         if( (data2 & 0x40) != 0) diff = -diff;
         target.inc(diff);
      }
   }

   @Override
   public void flush(
      final EncoderTarget target, final LayeredControllerExtension extension)
   {
   }

   private final int mData1;
}
