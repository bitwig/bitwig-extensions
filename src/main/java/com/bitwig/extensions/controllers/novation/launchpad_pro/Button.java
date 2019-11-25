package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;

public class Button
{
   private final int HOLD_DELAY_MS = 250;

   enum State
   {
      RELEASED, PRESSED, HOLD,
   }

   Button(
      final HardwareSurface hardwareSurface,
      final String id,
      final MidiIn midiIn,
      final int index,
      final boolean isPressureSensitive,
      final int x,
      final int y)
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
      mX = x;
      mY = y;
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

   State getState()
   {
      return mState;
   }

   void setState(final State state)
   {
      mState = state;
   }

   void onButtonPressed(ControllerHost host)
   {
      mState = State.PRESSED;

      final Boolean cancelHoldTask = false;
      host.scheduleTask(() -> {
         if (!cancelHoldTask && mState == State.PRESSED)
            mState = State.HOLD;
      }, HOLD_DELAY_MS);
   }

   void onButtonReleased()
   {
      mState = State.RELEASED;
      mCancelHoldTask = false;
   }

   boolean isPressed()
   {
      return mState == State.PRESSED || mState == State.HOLD;
   }

   public int getX()
   {
      return mX;
   }

   public int getY()
   {
      return mY;
   }

   private final Led mLed;
   private final HardwareButton mButton;
   private final MultiStateHardwareLight mLight;
   private final AbsoluteHardwareKnob mAfterTouch;
   private final int mX;
   private final int mY;

   State mState = State.RELEASED;
   Boolean mCancelHoldTask = false;
}
