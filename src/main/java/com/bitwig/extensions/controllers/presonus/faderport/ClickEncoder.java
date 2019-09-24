package com.bitwig.extensions.controllers.presonus.faderport;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.presonus.framework.target.ClickEncoderTarget;
import com.bitwig.extensions.controllers.presonus.framework.ControlElement;

public class ClickEncoder implements ControlElement<ClickEncoderTarget>
{
   public ClickEncoder(final int key, final int CC)
   {
      mKey = key;
      mCC = CC;
   }

   @Override
   public void onMidi(
      final ClickEncoderTarget target, final int status, final int data1, final int data2)
   {
      if (status == 0x90 && data1 == mKey)
      {
         target.click(data2 > 0);
      }
      else if (status == 0xB0 && data1 == mCC)
      {
         int diff = data2 & 0x3f;
         if( (data2 & 0x40) != 0) diff = -diff;
         target.inc(diff);
      }
   }

   @Override
   public void flush(
      final ClickEncoderTarget target, final MidiOut midiOut)
   {
   }

   private final int mKey;
   private final int mCC;
}
