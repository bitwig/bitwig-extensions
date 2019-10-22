package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.framework.ControlElement;
import com.bitwig.extensions.framework.targets.TouchFaderTarget;

public class Fader implements ControlElement<TouchFaderTarget>
{
   public Fader(final int channel)
   {
      mChannel = channel;
   }

   @Override
   public void onMidi(final TouchFaderTarget target, final ShortMidiMessage data)
   {
      if (data.isPitchBend() && data.getChannel() == mChannel)
      {
         int value = data.getData2() << 7 | data.getData1();

         target.set(value, 16384);
      }
   }

   @Override
   public void flush(final TouchFaderTarget target, final MidiOut midiOut)
   {

   }

   private final int mChannel;
}
