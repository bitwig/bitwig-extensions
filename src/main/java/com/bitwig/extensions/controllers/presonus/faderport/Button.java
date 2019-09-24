package com.bitwig.extensions.controllers.presonus.faderport;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.presonus.framework.target.ButtonTarget;
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
      int value = target.get() ? 127 : 0;
      if (value != mLastValue)
      {
         mLastValue = value;
         midiOut.sendMidi(0x90, mData1, value);
      }
   }

   @Override
   public void onMidi(final ButtonTarget target, final int status, final int data1, final int data2)
   {
      if (status == 144 && data1 == mData1)
      {
         target.set(data2 > 0);
      }
   }

   protected final int mData1;
   private int mLastValue = -1;
}
