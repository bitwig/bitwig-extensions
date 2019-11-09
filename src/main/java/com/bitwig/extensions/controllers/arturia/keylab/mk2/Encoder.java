package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extensions.oldframework.ControlElement;
import com.bitwig.extensions.oldframework.LayeredControllerExtension;
import com.bitwig.extensions.oldframework.targets.EncoderTarget;

public class Encoder implements ControlElement<EncoderTarget>
{
   public Encoder(final int cc)
   {
      mCC = cc;
   }

   @Override
   public void onMidi(final EncoderTarget target, final ShortMidiMessage data)
   {
      if (data.isControlChange() && data.getChannel() == 0)
      {
         final int CC = data.getData1();
         final int increment = decodeRelativeCC(data.getData2());

         if (CC == mCC)
         {
            target.inc(increment);
         }
      }
   }

   @Override
   public void flush(final EncoderTarget target, final LayeredControllerExtension extension)
   {
   }

   private int decodeRelativeCC(int x)
   {
      int increment = x & 0x3f;
      return ((x & 0x40) != 0) ? -increment : increment;
   }

   final int mCC;
}
