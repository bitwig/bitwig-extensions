package com.bitwig.extensions.controllers.presonus;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Parameter;

public class Fader implements Flushable, MidiReceiver
{
   public Fader(final int channel)
   {
      mChannel = channel;
   }

   public void setTarget(Parameter target)
   {
      mTarget = target;
   }

   @Override
   public void onMidi(final int status, final int data1, final int data2)
   {
      final int channel = status & 0xf;
      final int msg = (status >> 4) & 0xf;

      if (channel == mChannel)
      {
         if (msg == 0xE)
         {
            int value = data1 | (data2 << 7);
            mTarget.set(value, 16384);
         }
         else if (msg == 0x9 && data1 == (0x68 + mChannel))
         {
            mTarget.touch(data2 > 0);
         }
      }
   }

   @Override
   public void flush(final MidiOut midiOut)
   {
      if (mTarget == null) return;

      int value = Math.max(0, Math.min(16383, (int)(mTarget.get() * 16384.0)));

      if (mLastSentValue != value)
      {
         midiOut.sendMidi(0xE0 | mChannel, value & 0x7f, value >> 7);
         mLastSentValue = value;
      }
   }

   Parameter mTarget;
   int mLastSentValue = -1;
   private final int mChannel;
}
