package com.bitwig.extensions.controllers.presonus.atom;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.presonus.framework.ControlElement;
import com.bitwig.extensions.controllers.presonus.framework.EncoderTarget;

public class Encoder implements ControlElement<EncoderTarget>
{
   public Encoder(final int data1)
   {
      mData1 = data1;
   }

   @Override
   public void onMidi(
      final EncoderTarget target, final int status, final int data1, final int data2)
   {
      if (status == 176 && data1 == mData1)
      {
         int diff = data2 & 0x3f;
         if( (data2 & 0x40) != 0) diff = -diff;
         target.inc(diff);
      }
   }

   @Override
   public void flush(
      final EncoderTarget target, final MidiOut midiOut)
   {
   }

   private final int mData1;
}
