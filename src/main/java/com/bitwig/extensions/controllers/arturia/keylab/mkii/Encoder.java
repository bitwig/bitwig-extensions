package com.bitwig.extensions.controllers.arturia.keylab.mkii;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.framework.ControlElement;
import com.bitwig.extensions.framework.targets.EncoderTarget;

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
   public void flush(final EncoderTarget target, final MidiOut midiOut)
   {
   }

   private int decodeRelativeCC(int x)
   {
      int increment = x & 0x3f;
      return ((x & 0x40) != 0) ? -increment : increment;
   }

   final int mCC;
}
