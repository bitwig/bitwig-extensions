package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ColorValue;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;

class Button
{
   private final int HOLD_DELAY_MS = 250;

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
      final HardwareSurface hardwareSurface,
      final String id,
      final MidiIn midiIn,
      final int index,
      final boolean isPressureSensitive,
      final int x,
      final int y)
   {
      assert index >= 0;
      assert index < 100;

      mIsPressureSensitive = isPressureSensitive;
      mIndex = index;

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

      final MultiStateHardwareLight light = hardwareSurface.createMultiStateHardwareLight(id + "-light");
      light.state().currentValue();
      bt.setBackgroundLight(light);

      mButton = bt;
      mLight = light;
      mX = x;
      mY = y;
   }

   public HardwareButton getButton()
   {
      return mButton;
   }

   public AbsoluteHardwareKnob getAfterTouch()
   {
      return mAfterTouch;
   }

   State getButtonState()
   {
      return mButtonState;
   }

   void setButtonState(final State buttonState)
   {
      mButtonState = buttonState;
   }

   void onButtonPressed(ControllerHost host)
   {
      mButtonState = State.PRESSED;

      final Boolean cancelHoldTask = false;
      host.scheduleTask(() -> {
         if (!cancelHoldTask && mButtonState == State.PRESSED)
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

   public int getX()
   {
      return mX;
   }

   public int getY()
   {
      return mY;
   }

   public void setColor(final float red, final float green, final float blue)
   {
      mDesiredLedState.setColor(red, green, blue);
   }

   public void setColor(final ColorValue color)
   {
      assert color.isSubscribed();

      setColor(color.red(), color.green(), color.blue());
   }

   void setColor(final Color c)
   {
      mDesiredLedState.setColor(c);
   }

   public String updateClearSysex()
   {
      final Color color = mDesiredLedState.getColor();
      if (color.equals(mCurrentLedState.getColor()) || !color.isBlack())
         return "";

      mCurrentLedState.set(mDesiredLedState);
      return String.format(" %02x 00", mIndex);
   }

   public String updateLightLEDSysex()
   {
      final Color color = mDesiredLedState.getColor();
      if (mCurrentLedState.getColor().equals(color) || color.isBlack())
         return "";

      mCurrentLedState.getColor().set(color);
      return String.format(" %02x %02x %02x %02x", mIndex, color.getRed(), color.getGreen(), color.getBlue());
   }

   public String updatePulseSysex()
   {
      final int pulseColor = mDesiredLedState.getPulseColor();
      if (pulseColor == mCurrentLedState.getPulseColor())
         return "";

      mCurrentLedState.setPulseColor(pulseColor);
      if (pulseColor == 0)
         return "";
      return String.format(" %02x %02x", mIndex, pulseColor);
   }

   /* Hardware objects */
   private final HardwareButton mButton;
   private final MultiStateHardwareLight mLight;
   private final AbsoluteHardwareKnob mAfterTouch;

   /* State */
   private final int mX;
   private final int mY;
   private final int mIndex;
   private final boolean mIsPressureSensitive;
   private LedState mDesiredLedState;
   private LedState mCurrentLedState;

   private State mButtonState = State.RELEASED;
   private Boolean mCancelHoldTask = false;
}
