package com.bitwig.extensions.controllers.presonus.faderport;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.presonus.framework.ControlElement;
import com.bitwig.extensions.controllers.presonus.framework.target.TouchFaderTarget;

public class MotorFader implements ControlElement<TouchFaderTarget>
{
   public MotorFader(final int channel)
   {
      mChannel = channel;
   }

   @Override
   public void onMidi(
      final TouchFaderTarget target, final int status, final int data1, final int data2)
   {
      final int channel = status & 0xf;
      final int msg = (status >> 4) & 0xf;

      if (channel == mChannel && msg == 0xE)
      {
         int value = data1 | (data2 << 7);
         target.set(value, 16384);
      }

      if (status == 0x90 && data1 == (0x68 + mChannel))
      {
         mIsBeingTouched = data2 > 0;
         target.touch(mIsBeingTouched);
      }
   }

   @Override
   public void flush(final TouchFaderTarget target, final MidiOut midiOut)
   {
      int value = Math.max(0, Math.min(16383, (int)(target.get() * 16384.0)));

      if (mLastSentValue != value)
      {
         midiOut.sendMidi(0xE0 | mChannel, value & 0x7f, value >> 7);
         mLastSentValue = value;
      }
   }

   public boolean isBeingTouched()
   {
      return mIsBeingTouched;
   }

   int mLastSentValue = -1;
   private final int mChannel;
   private boolean mIsBeingTouched;
}
