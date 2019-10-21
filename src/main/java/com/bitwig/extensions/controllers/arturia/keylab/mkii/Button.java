package com.bitwig.extensions.controllers.arturia.keylab.mkii;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.presonus.framework.ControlElement;
import com.bitwig.extensions.controllers.presonus.framework.targets.ButtonTarget;

public class Button implements ControlElement<ButtonTarget>
{
   public Button(final Buttons buttonID)
   {
      mButtonID = buttonID;
   }

   @Override
   public void onMidi(final ButtonTarget target, final int status, final int data1, final int data2)
   {
      ShortMidiMessage data = new ShortMidiMessage(status, data1, data2);

      if (data.isNoteOn() && data.getChannel() == 0)
      {
         final boolean on = data.getData2() >= 64;
         final int key = data.getData1();

         if (key == mButtonID.getKey())
         {
            target.set(on);
         }
      }
   }

   @Override
   public void flush(final ButtonTarget target, final MidiOut midiOut)
   {
      int newState = target.get() ? 127 : 0;

      if (mLastButtonState != newState)
      {
         midiOut.sendMidi(0x90, mButtonID.getKey(), newState);
         mLastButtonState = newState;
      }
   }

   private final Buttons mButtonID;
   private int mLastButtonState = -1;
}
