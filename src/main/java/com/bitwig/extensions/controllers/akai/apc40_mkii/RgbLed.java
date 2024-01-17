package com.bitwig.extensions.controllers.akai.apc40_mkii;

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

      mLight = surface.createMultiStateHardwareLight(button.getId() + "-light");
      mLight.setColorToStateFunction(RGBLedState::getBestStateForColor);
      mLight.state().onUpdateHardware(state -> sendLightState(midiOut, (RGBLedState)state));
      button.setBackgroundLight(mLight);
   }

   public MultiStateHardwareLight getLight()
   {
       return mLight;
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

   private final MultiStateHardwareLight mLight;

   private final int mMessage, mData1;
}
