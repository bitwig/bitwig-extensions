package com.bitwig.extensions.controllers.presonus.atom;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.framework.ControlElement;
import com.bitwig.extensions.framework.targets.RGBButtonTarget;

public class Pad implements ControlElement<RGBButtonTarget>
{
   public Pad(final int index)
   {
      mIndex = index;
   }

   private int fromFloat(float x)
   {
      return Math.max(0, Math.min((int)(127.0 * x), 127));
   }
   @Override
   public void onMidi(final RGBButtonTarget target, final int status, final int data1, final int data2)
   {
      if (status == 0x90 && data1 == (0x24 + mIndex))
      {
         target.set(data2 > 0);
      }
   }

   @Override
   public void flush(
      final RGBButtonTarget target, final MidiOut midiOut)
   {
      float[] RGB = target.getRGB();
      final int[] values = new int[4];
      values[0] = target.get() ?  127 : 0;
      values[1] = fromFloat(RGB[0]);
      values[2] = fromFloat(RGB[1]);
      values[3] = fromFloat(RGB[2]);

      for(int i=0; i<4; i++)
      {
         if (values[i] != mLastSent[i])
         {
            midiOut.sendMidi(0x90 + i, 0x24 + mIndex, values[i]);
            mLastSent[i] = values[i];
         }
      }
   }

   private int[] mLastSent = new int[] {-1, -1, -1, -1};
   private final int mIndex;
}
