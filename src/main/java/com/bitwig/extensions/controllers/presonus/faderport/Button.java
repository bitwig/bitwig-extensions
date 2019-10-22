package com.bitwig.extensions.controllers.presonus.faderport;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extensions.framework.LayeredControllerExtension;
import com.bitwig.extensions.framework.targets.ButtonTarget;
import com.bitwig.extensions.framework.ControlElement;

public class Button implements ControlElement<ButtonTarget>
{
   public Button(final int data1)
   {
      mData1 = data1;
   }

   @Override
   public void flush(final ButtonTarget target, final LayeredControllerExtension extension)
   {
      int value = target.get() ? 127 : 0;
      if (value != mLastValue)
      {
         mLastValue = value;
         extension.getMidiOutPort(0).sendMidi(0x90, mData1, value);
      }
   }

   @Override
   public void onMidi(final ButtonTarget target, final ShortMidiMessage data)
   {
      if (data.getStatusByte() == 144 && data.getData1() == mData1)
      {
         target.set(data.getData2() > 0);
      }
   }

   protected final int mData1;
   private int mLastValue = -1;
}
