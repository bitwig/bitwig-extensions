package com.bitwig.extensions.controllers.presonus.atom;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.presonus.framework.targets.ButtonTarget;
import com.bitwig.extensions.controllers.presonus.framework.ControlElement;

public class Button implements ControlElement<ButtonTarget>
{
   public Button(final int data1)
   {
      mData1 = data1;
   }

   @Override
   public void flush(final ButtonTarget target, final MidiOut midiOut)
   {
      boolean value = target.get();
      if (value != mLastValue)
      {
         mLastValue = value;
         midiOut.sendMidi(0xB0, mData1, value ? 127 : 0);
      }
   }

   @Override
   public void onMidi(final ButtonTarget target, final int status, final int data1, final int data2)
   {
      if (status == 176 && data1 == mData1)
      {
         target.set(data2 > 0);
      }
   }

   protected final int mData1;
   private boolean mLastValue;
}
