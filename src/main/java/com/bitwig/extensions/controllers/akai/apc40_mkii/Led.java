package com.bitwig.extensions.controllers.akai.apc40_mkii;

import com.bitwig.extension.controller.api.MidiOut;

public class Led
{
   public void paint(final MidiOut midiOut, final int msg, final int channel, final int data1)
   {
      if (mValue == mDisplayedValue)
         return;

      midiOut.sendMidi((msg << 4) | channel, data1, mValue);
      mDisplayedValue = mValue;
   }

   public void set(final int value)
   {
      mValue = value;
   }

   private int mValue = 0;

   private int mDisplayedValue = -1;
}
