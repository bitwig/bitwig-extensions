package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;

public class LaunchpadButtonAndLed
{
   LaunchpadButtonAndLed(
      final HardwareSurface hardwareSurface,
      final String id,
      final MidiIn midiIn,
      final int index,
      final boolean isPressureSensitive)
   {
      mLed = new Led(index);

      final HardwareButton bt = hardwareSurface.createHardwareButton(id);

      if (isPressureSensitive)
      {
         bt.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(0, index));
         bt.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, index));

         mAfterTouch = hardwareSurface.createAbsoluteHardwareKnob(id + "-at");
         mAfterTouch.setAdjustValueMatcher(midiIn.createAbsolutePolyATValueMatcher(0, index));
      }
      else
      {
         bt.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, index, 127));
         bt.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(0, index, 0));

         mAfterTouch = null;
      }

      final MultiStateHardwareLight light =
         hardwareSurface.createMultiStateHardwareLight(id + "-light", Led::stateToVisualState);
      light.state().setValueSupplier(mLed::getState);
      bt.setBackgroundLight(light);

      mButton = bt;
      mLight = light;
   }

   public Led getLed()
   {
      return mLed;
   }

   public HardwareButton getButton()
   {
      return mButton;
   }

   public AbsoluteHardwareKnob getAfterTouch()
   {
      return mAfterTouch;
   }

   private final Led mLed;
   private final HardwareButton mButton;
   private final MultiStateHardwareLight mLight;
   private final AbsoluteHardwareKnob mAfterTouch;
}
