package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.ControllerHost;

final class ButtonState
{
   private final int HOLD_DELAY_MS = 250;

   enum State
   {
      RELEASED, PRESSED, HOLD,
   }

   ButtonState(int x, int y)
   {
      mX = x;
      mY = y;
      mLed = new Led(x, y);
   }

   State getState()
   {
      return mState;
   }

   void setState(final State state)
   {
      mState = state;
   }

   Led getLed()
   {
      return mLed;
   }

   void onButtonPressed(ControllerHost host)
   {
      mState = State.PRESSED;

      final Boolean cancelHoldTask = false;
      host.scheduleTask(() -> {
         if (!cancelHoldTask && mState == ButtonState.State.PRESSED)
            mState = ButtonState.State.HOLD;
      }, HOLD_DELAY_MS);
   }

   void onButtonReleased()
   {
      mState = State.RELEASED;
      mCancelHoldTask = false;
   }

   int getX()
   {
      return mX;
   }

   int getY()
   {
      return mY;
   }

   boolean isPressed()
   {
      return mState == State.PRESSED || mState == State.HOLD;
   }

   final Led mLed;
   State mState = State.RELEASED;
   Boolean mCancelHoldTask = false;
   private final int mX;
   private final int mY;
}
