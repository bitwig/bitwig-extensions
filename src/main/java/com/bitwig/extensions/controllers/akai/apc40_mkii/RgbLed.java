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
      final int data1)
   {
      super();
      mMessage = message;
      mData1 = data1;

      MultiStateHardwareLight hardwareLight = surface.createMultiStateHardwareLight(button.getId() + "-light");
      hardwareLight.state().setValueSupplier(this::getState);
      button.setBackgroundLight(hardwareLight);
   }

   public void paint(final MidiOut midiOut)
   {
      if (mColor != mDisplayedColor || mBlinkColor != mDisplayedBlinkColor
         || mBlinkType != mDisplayedBlinkType)
      {
         midiOut.sendMidi(mMessage << 4, mData1, mColor);

         if (mBlinkType != RGBLedState.BLINK_NONE)
         {
            midiOut.sendMidi(mMessage << 4, mData1, mBlinkColor);
            midiOut.sendMidi((mMessage << 4) | mBlinkType, mData1, mColor);
         }
         else
         {
            midiOut.sendMidi(mMessage << 4, mData1, mColor);
         }

         mDisplayedColor = mColor;
         mDisplayedBlinkColor = mBlinkColor;
         mDisplayedBlinkType = mBlinkType;
      }
   }

   public void setColor(final float red, final float green, final float blue)
   {
      final int r8 = (int)(red * 255);
      final int g8 = (int)(green * 255);
      final int b8 = (int)(blue * 255);
      final int total = (r8 << 16) | (g8 << 8) | b8;

      mColor = RGBLedState.getColorValueForRGB(total);
   }

   public void setColor(final int color)
   {
      mColor = color;
   }

   public void setColor(final ColorValue color)
   {
      setColor(color.red(), color.green(), color.blue());
   }

   public void setBlinkType(final int blinkType)
   {
      mBlinkType = blinkType;
   }

   public void setBlinkColor(final int blinkColor)
   {
      mBlinkColor = blinkColor;
   }

   public RGBLedState getState()
   {
      return new RGBLedState(mDisplayedColor, mDisplayedBlinkColor, mDisplayedBlinkType);
   }

   private final int mMessage, mData1;

   private int mColor = RGBLedState.COLOR_NONE;

   private int mDisplayedColor = -1;

   private int mBlinkColor = RGBLedState.COLOR_NONE;

   private int mDisplayedBlinkColor = -1;

   private int mBlinkType = RGBLedState.BLINK_NONE;

   private int mDisplayedBlinkType = -1;
}
