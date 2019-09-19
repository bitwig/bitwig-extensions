package com.bitwig.extensions.controllers.presonus;

import com.bitwig.extension.controller.api.MidiOut;

public class AtomPad implements ColorSettable
{
   public AtomPad(final MidiOut midiOut, final int index)
   {
      mMidiOut = midiOut;
      mIndex = index;
   }

   public void setOn(final boolean on)
   {
      if (mLastOn != on)
      {
         mMidiOut.sendMidi(0x90, 0x24 + mIndex, on ? 127 : 0);
         mLastOn = on;
      }
   }

   @Override
   public void setColor(final float red, final float green, final float blue)
   {
      int r128 = Math.max(0, Math.min((int)(127.0 * red), 127));
      int g128 = Math.max(0, Math.min((int)(127.0 * green), 127));
      int b128 = Math.max(0, Math.min((int)(127.0 * blue), 127));

      mMidiOut.sendMidi(0x91, 0x24 + mIndex, r128);
      mMidiOut.sendMidi(0x92, 0x24 + mIndex, g128);
      mMidiOut.sendMidi(0x93, 0x24 + mIndex, b128);
   }

   private final int mIndex;
   private final MidiOut mMidiOut;
   private boolean mLastOn;
}
