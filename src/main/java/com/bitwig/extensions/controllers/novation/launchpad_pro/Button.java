package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;

final class Button
{
   static final int NO_PULSE = 0;
   static final int PULSE_PLAYING = 88;
   static final int PULSE_RECORDING = 72;
   static final int PULSE_PLAYBACK_QUEUED = 89;
   static final int PULSE_RECORDING_QUEUED = 56;
   static final int PULSE_STOP_QUEUED = 118;

   enum State
   {
      RELEASED, PRESSED, HOLD,
   }

   Button(
      final LaunchpadProControllerExtension driver,
      final String id,
      final MidiIn midiIn,
      final int index,
      final boolean isPressureSensitive,
      final int x,
      final int y)
   {
      assert index >= 0;
      assert index < 100;

      mDriver = driver;

      final HardwareSurface hardwareSurface = driver.mHardwareSurface;
      mIsPressureSensitive = isPressureSensitive;
      mIndex = index;

      final HardwareButton bt = hardwareSurface.createHardwareButton(id);
      bt.isPressed().markInterested();

      if (isPressureSensitive)
      {
         bt.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(0, index));
         bt.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, index));

         mAfterTouch = hardwareSurface.createAbsoluteHardwareKnob(id + "-at");
         mAfterTouch.setAdjustValueMatcher(midiIn.createPolyAftertouchValueMatcher(0, index));
         bt.setAftertouchControl(mAfterTouch);
      }
      else
      {
         bt.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, index, 127));
         bt.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(0, index, 0));

         mAfterTouch = null;
      }

      final MultiStateHardwareLight light = hardwareSurface.createMultiStateHardwareLight(id + "-light");
      light.state().setValue(LedState.OFF);
      light.setColorToStateFunction(color -> new LedState(color));
      light.state().onUpdateHardware(internalHardwareLightState -> mDriver.updateButtonLed(Button.this, (LedState)internalHardwareLightState));
      bt.setBackgroundLight(light);

      mButton = bt;
      mLight = light;
      mX = x;
      mY = y;
   }

   State getButtonState()
   {
      return mButtonState;
   }

   void onButtonPressed(final ControllerHost host)
   {
      mButtonState = State.PRESSED;

      mCancelHoldTask = false;
      final int HOLD_DELAY_MS = 250;
      host.scheduleTask(() -> {
         if (!mCancelHoldTask && mButtonState == State.PRESSED)
            mButtonState = State.HOLD;
      }, HOLD_DELAY_MS);
   }

   void onButtonReleased()
   {
      mButtonState = State.RELEASED;
      mCancelHoldTask = false;
   }

   boolean isPressed()
   {
      return mButtonState == State.PRESSED || mButtonState == State.HOLD;
   }

   public void appendLedUpdate(LedState ledState,
      final StringBuilder ledClear, final StringBuilder ledUpdate, final StringBuilder ledPulseUpdate)
   {
      if (ledState == null)
         ledState = LedState.OFF;

      final Color color = ledState.mColor;
      final int pulse = ledState.mPulse;

      if (pulse == NO_PULSE)
      {
         if (color.isBlack())
            ledClear.append(String.format(" %02x 00", mIndex));
         else
            ledUpdate.append(String.format(" %02x %02x %02x %02x", mIndex, color.mRed, color.mGreen, color.mBlue));
      }
      else
         ledPulseUpdate.append(String.format(" %02x %02x", mIndex, pulse));
   }

   // For debugging
   @Override
   public String toString()
   {
      return mButton.getId() + " [" + mX + "; " + mY + "]";
   }

   private final LaunchpadProControllerExtension mDriver;

   /* Hardware objects */
   final HardwareButton mButton;
   final MultiStateHardwareLight mLight;
   final AbsoluteHardwareKnob mAfterTouch;

   /* State */
   final int mX;
   final int mY;
   final int mIndex;
   final boolean mIsPressureSensitive;

   private State mButtonState = State.RELEASED;
   private boolean mCancelHoldTask = false;
}
