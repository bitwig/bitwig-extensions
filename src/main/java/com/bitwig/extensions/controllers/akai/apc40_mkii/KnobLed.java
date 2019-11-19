package com.bitwig.extensions.controllers.akai.apc40_mkii;

import com.bitwig.extension.controller.api.MidiOut;

public class KnobLed
{
   public static final int RING_INIT = -1;

   public static final int RING_OFF = 0;

   public static final int RING_SINGLE = 1;

   public static final int RING_VOLUME = 2;

   public static final int RING_PAN = 3;

   public void flush(final MidiOut midiOut, final int msg, final int channel, final int data1)
   {
      if (mRing != mDisplayedRing)
      {
         assert mRing >= 0;
         assert mRing < 128;

         midiOut.sendMidi((msg << 4) | channel, data1 + 8, mRing);
         mDisplayedRing = mRing;
      }

      if (mValue != mDisplayedValue)
      {
         assert mValue >= 0;
         assert mValue < 128;

         midiOut.sendMidi((msg << 4) | channel, data1, mValue);
         mDisplayedValue = mValue;
      }
   }

   public boolean wantsFlush()
   {
      return mRing != mDisplayedRing || mValue != mDisplayedValue;
   }

   public void set(final int value)
   {
      assert value >= 0;
      assert value < 128;

      if (value > 127)
         mValue = 127;
      else if (value < 0)
         mValue = 0;
      else
         mValue = value;
   }

   public void setDisplayedValue(final int value)
   {
      assert value >= 0;
      assert value < 128;

      mValue = value;
      mDisplayedValue = value;
   }

   public void setRing(final int ring)
   {
      mRing = ring;
   }

   private int mValue = 0;

   private int mDisplayedValue = -1;

   private int mRing = RING_OFF;

   private int mDisplayedRing = RING_INIT;
}
