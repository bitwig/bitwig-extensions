package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extensions.oldframework.ControlElement;
import com.bitwig.extensions.oldframework.LayeredControllerExtension;
import com.bitwig.extensions.oldframework.targets.TouchFaderTarget;

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
   public void flush(final TouchFaderTarget target, final LayeredControllerExtension extension)
   {

   }

   private final int mChannel;
}
