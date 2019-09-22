package com.bitwig.extensions.controllers.presonus.atom;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.presonus.ColorSettable;
import com.bitwig.extensions.controllers.presonus.Flushable;

public class AtomPad implements ColorSettable, Flushable
{
   public AtomPad(final MidiOut midiOut, final int index)
   {
      mMidiOut = midiOut;
      mIndex = index;
   }

   public void setOn(final boolean on)
   {
      mOn = on;
   }

   public void setHasChain(final boolean hasChain)
   {
      mHasChain = hasChain;
   }

   @Override
   public void setColor(final float red, final float green, final float blue)
   {
      int r128 = Math.max(0, Math.min((int)(127.0 * red), 127));
      int g128 = Math.max(0, Math.min((int)(127.0 * green), 127));
      int b128 = Math.max(0, Math.min((int)(127.0 * blue), 127));

      mPadColor[0] = r128;
      mPadColor[1] = g128;
      mPadColor[2] = b128;
   }

   private int computeColorChannel(int x)
   {
      if (mOn)
         return 127;

      return x;
   }

   @Override
   public void flush(final MidiOut midiOut)
   {
      final int[] values = new int[4];
      values[0] = mHasChain ? 127 : 0;
      values[1] = computeColorChannel(mPadColor[0]);
      values[2] = computeColorChannel(mPadColor[1]);
      values[3] = computeColorChannel(mPadColor[2]);

      for(int i=0; i<4; i++)
      {
         if (values[i] != mLastSent[i])
         {
            mMidiOut.sendMidi(0x90 + i, 0x24 + mIndex, values[i]);
            mLastSent[i] = values[i];
         }
      }
   }

   private int[] mPadColor = new int[3];
   private int[] mLastSent = new int[4];
   private final int mIndex;
   private final MidiOut mMidiOut;
   private boolean mOn;
   private boolean mHasChain;
}
