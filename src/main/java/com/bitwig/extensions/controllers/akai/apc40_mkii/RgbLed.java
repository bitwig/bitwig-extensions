package com.bitwig.extensions.controllers.akai.apc40_mkii;

import com.bitwig.extension.controller.api.ColorValue;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;

class RgbLed
{


   protected RgbLed(
      final HardwareButton button,
      final HardwareSurface surface,
      final int message,
      final int data1,
      final MidiOut midiOut)
   {
      super();
      mMessage = message;
      mData1 = data1;

      final MultiStateHardwareLight hardwareLight = surface.createMultiStateHardwareLight(button.getId() + "-light");
      hardwareLight.state().setValueSupplier(this::getState);
      hardwareLight.setColorToStateFunction(RGBLedState::getBestStateForColor);
      hardwareLight.state().onUpdateHardware(state -> sendLightState(midiOut, (RGBLedState)state));
      button.setBackgroundLight(hardwareLight);
   }

   private void sendLightState(final MidiOut midiOut, RGBLedState state)
   {
      if (state == null)
         state = RGBLedState.OFF_STATE;
         
      final var color = state.getColor();
      final var blinkColor = state.getBlinkColor();
      final var blinkType = state.getBlinkType();
      
      midiOut.sendMidi(mMessage << 4, mData1, color);

      if (blinkType != RGBLedState.BLINK_NONE)
      {
         midiOut.sendMidi(mMessage << 4, mData1, blinkColor);
         midiOut.sendMidi((mMessage << 4) | blinkType, mData1, color);
      }
      else
      {
         midiOut.sendMidi(mMessage << 4, mData1, color);
      }
   }

   public void setColor(final float red, final float green, final float blue)
   {
      setState(RGBLedState.getBestStateForColor(red, green, blue));
   }

   public RGBLedState getState()
   {
      return mState;
   }

   public void setState(final RGBLedState state)
   {
      mState = state;
   }

   private final int mMessage, mData1;

   private RGBLedState mState;
}
