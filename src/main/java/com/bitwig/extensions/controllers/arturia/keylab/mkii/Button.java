package com.bitwig.extensions.controllers.arturia.keylab.mkii;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.framework.ControlElement;
import com.bitwig.extensions.framework.targets.ButtonTarget;

public class Button extends AbstractButton implements ControlElement<ButtonTarget>
{
   public Button(final Buttons buttonID)
   {
      super(buttonID);
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

   private int mLastButtonState = -1;
}
