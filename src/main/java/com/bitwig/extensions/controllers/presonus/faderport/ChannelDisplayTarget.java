package com.bitwig.extensions.controllers.presonus.faderport;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.Track;

class ChannelDisplayTarget implements DisplayTarget
{
   public ChannelDisplayTarget(final Track track, final BooleanValue isSelected, final MotorFader motorFader)
   {
      mTrack = track;
      mIsSelected = isSelected;
      mMotorFader = motorFader;
   }

   @Override
   public ValueBarMode getValueBarMode()
   {
      if (!mTrack.exists().get())
         return ValueBarMode.Off;

      return ValueBarMode.Bipolar;
   }

   @Override
   public int getBarValue()
   {
      final double pan = mTrack.pan().get();
      return Math.max(0, Math.min(127, (int)(pan * 127.0)));
   }

   @Override
   public DisplayMode getMode()
   {
      return DisplayMode.MixedText;
   }

   @Override
   public boolean isTextInverted(final int line)
   {
      if (mIsSelected.get() && line == 1 && mTrack.exists().get())
      {
         return true;
      }

      return false;
   }

   @Override
   public String getText(final int line)
   {
      if (!mTrack.exists().get())
         return "";

      final String trackNumber = Integer.toString(mTrack.position().get() + 1);

      if (line == 0)
      {
         final String fullname = mTrack.name().get();

         if (fullname.endsWith(trackNumber))
            return mTrack.name().getLimited(8 + trackNumber.length()).replace(trackNumber, "");

         final String limited = mTrack.name().getLimited(8);

         return limited;
      }

      if (line == 1)
      {
         return trackNumber;
      }

      if (line == 2)
      {
         if (mMotorFader.isBeingTouched())
            return getMainControl().displayedValue().getLimited(10).replace(" dB", "");

         return getLabelControl().name().getLimited(10);
      }

      return "";
   }

   protected Parameter getMainControl()
   {
      return mTrack.volume();
   }

   protected Parameter getLabelControl()
   {
      return mTrack.pan();
   }

   protected final Track mTrack;

   private final BooleanValue mIsSelected;

   private final MotorFader mMotorFader;
}
