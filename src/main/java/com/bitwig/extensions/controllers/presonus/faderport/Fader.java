package com.bitwig.extensions.controllers.presonus.faderport;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.presonus.framework.MotorFaderControlElement;
import com.bitwig.extensions.controllers.presonus.framework.MotorFaderTarget;

public class Fader implements MotorFaderControlElement
{
   public Fader(final int channel)
   {
      mChannel = channel;
   }

   @Override
   public void onMidi(
      final MotorFaderTarget target, final int status, final int data1, final int data2)
   {
      final int channel = status & 0xf;
      final int msg = (status >> 4) & 0xf;

      if (channel == mChannel)
      {
         if (msg == 0xE)
         {
            int value = data1 | (data2 << 7);
            target.set(value, 16384);
         }
         else if (msg == 0x9 && data1 == (0x68 + mChannel))
         {
            target.touch(data2 > 0);
         }
      }
   }

   @Override
   public void flush(final MotorFaderTarget target, final MidiOut midiOut)
   {
      int value = Math.max(0, Math.min(16383, (int)(target.get() * 16384.0)));

      if (mLastSentValue != value)
      {
         midiOut.sendMidi(0xE0 | mChannel, value & 0x7f, value >> 7);
         mLastSentValue = value;
      }
   }

   /*
      @Override
      public void onMidi(final Target, final int status, final int data1, final int data2)
      {

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
   */
   int mLastSentValue = -1;
   private final int mChannel;
}
