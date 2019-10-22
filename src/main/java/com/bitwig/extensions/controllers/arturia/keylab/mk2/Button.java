package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import com.bitwig.extensions.framework.ControlElement;
import com.bitwig.extensions.framework.LayeredControllerExtension;
import com.bitwig.extensions.framework.targets.ButtonTarget;

public class Button extends AbstractButton implements ControlElement<ButtonTarget>
{
   public Button(final Buttons buttonID)
   {
      super(buttonID);
   }

   @Override
   public void flush(final ButtonTarget target, final LayeredControllerExtension extension)
   {
      int newState = target.get() ? 127 : 0;

      if (mLastButtonState != newState)
      {
         extension.getMidiOutPort(1).sendMidi(0x90, mButtonID.getKey(), newState);
         mLastButtonState = newState;
      }
   }
}
