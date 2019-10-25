package com.bitwig.extensions.controllers.presonus.faderport;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extensions.framework.ControlElement;
import com.bitwig.extensions.framework.LayeredControllerExtension;
import com.bitwig.extensions.framework.targets.TouchFaderTarget;

public class MotorFader implements ControlElement<TouchFaderTarget>
{
   public MotorFader(final int channel, final AbsoluteHardwareControl hardwareControl)
   {
      mChannel = channel;
      mHardwareControl = hardwareControl;
   }

   @Override
   public void setTarget(final TouchFaderTarget target)
   {
      target.assignToHardwareControl(mHardwareControl);
   }

   @Override
   public void onMidi(
      final TouchFaderTarget target, final ShortMidiMessage data)
   {
      final int status = data.getStatusByte();
      final int data1 = data.getData1();
      final int data2 = data.getData2();

      if (data.getChannel() == mChannel && data.isPitchBend())
      {
         final int value = data1 | (data2 << 7);
         target.set(value, 16384);
      }

      if (status == 0x90 && data1 == (0x68 + mChannel))
      {
         mIsBeingTouched = data2 > 0;
         target.touch(mIsBeingTouched);
      }
   }

   @Override
   public void flush(final TouchFaderTarget target, final LayeredControllerExtension extension)
   {
      final int value = Math.max(0, Math.min(16383, (int)(target.get() * 16384.0)));

      if (mLastSentValue != value)
      {
         extension.getMidiOutPort(0).sendMidi(0xE0 | mChannel, value & 0x7f, value >> 7);
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

   private final AbsoluteHardwareControl mHardwareControl;
}
