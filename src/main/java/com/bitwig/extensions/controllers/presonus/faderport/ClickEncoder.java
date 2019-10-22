package com.bitwig.extensions.controllers.presonus.faderport;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extensions.framework.LayeredControllerExtension;
import com.bitwig.extensions.framework.targets.ClickEncoderTarget;
import com.bitwig.extensions.framework.ControlElement;

public class ClickEncoder implements ControlElement<ClickEncoderTarget>
{
   public ClickEncoder(final int key, final int CC)
   {
      mKey = key;
      mCC = CC;
   }

   @Override
   public void onMidi(
      final ClickEncoderTarget target, final ShortMidiMessage data)
   {
      final int status = data.getStatusByte();
      final int data1 = data.getData1();
      final int data2 = data.getData2();

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
      final ClickEncoderTarget target, final LayeredControllerExtension extension)
   {
   }

   private final int mKey;
   private final int mCC;
}
