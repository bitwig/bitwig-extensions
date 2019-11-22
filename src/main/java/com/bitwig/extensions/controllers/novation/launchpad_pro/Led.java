package com.bitwig.extensions.controllers.novation.launchpad_pro;


import com.bitwig.extension.controller.api.ColorValue;
import com.bitwig.extension.controller.api.HardwareLightVisualState;

final class Led
{
   public static final int NO_PULSE = 0;
   public static final int PULSE_PLAYING = 88;
   public static final int PULSE_RECORDING = 72;
   public static final int PULSE_PLAYBACK_QUEUED = 89;
   public static final int PULSE_RECORDING_QUEUED = 56;
   public static final int PULSE_STOP_QUEUED = 118;

   public Led(final int ledIndex)
   {
      mLedIndex = ledIndex;

      assert mLedIndex < 100;
      assert mLedIndex >= 0;
   }

   public Led(final int x, final int y)
   {
      this(x + 10 * y);
   }

   public String updateClearSysex()
   {
      if (mColor.equals(mDisplayedColor) || !mColor.isBlack())
         return "";

      mDisplayedColor.set(mColor);
      return String.format(" %02x 00", mLedIndex);
   }

   public String updateLightLEDSysex()
   {
      if ((mColor.equals(mDisplayedColor) && mPulseColor == mDisplayedPulseColor) || mColor.isBlack())
         return "";

      mDisplayedColor.set(mColor);
      return String.format(" %02x %02x %02x %02x", mLedIndex, mColor.getRed(), mColor.getGreen(), mColor.getBlue());
   }

   public String updatePulseSysex()
   {
      if (mPulseColor == mDisplayedPulseColor)
         return "";

      mDisplayedPulseColor = mPulseColor;
      if (mPulseColor == 0)
         return "";
      return String.format(" %02x %02x", mLedIndex, mPulseColor);
   }

   public void setColor(final float red, final float green, final float blue)
   {
      mColor.set(red, green, blue);
   }

   public void setColor(final Color color)
   {
      mColor.set(color);
   }

   public void setColor(final ColorValue color)
   {
      assert color.isSubscribed();

      setColor(color.red(), color.green(), color.blue());
   }

   public void clear()
   {
      mColor.set(0, 0, 0);
   }

   public void setPulse(final int pulseColor)
   {
      mPulseColor = pulseColor;
   }

   public int getState()
   {
      return mColor.toInt24();
   }

   public static HardwareLightVisualState stateToVisualState(final int state)
   {
      final Color color = Color.fromInt24(state);
      return HardwareLightVisualState.createForColor(color.toApiColor());
   }

   private final int mLedIndex;
   private final Color mColor = new Color(); // The color we want
   private final Color mDisplayedColor = new Color(); // The color currently displayed
   private int mPulseColor = 0;
   private int mDisplayedPulseColor = 0;
}
