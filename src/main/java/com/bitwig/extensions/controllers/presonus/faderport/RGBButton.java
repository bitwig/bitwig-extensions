package com.bitwig.extensions.controllers.presonus.faderport;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.presonus.framework.ControlElement;
import com.bitwig.extensions.controllers.presonus.framework.target.RGBButtonTarget;

public class RGBButton implements ControlElement<RGBButtonTarget>
{
   public RGBButton(final int data1)
   {
      mData1 = data1;
   }

   private int fromFloat(float x)
   {
      return Math.max(0, Math.min((int)(127.0 * x), 127));
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
            midiOut.sendMidi(0x90 + i, mData1, values[i]);
            mLastSent[i] = values[i];
         }
      }
   }

   @Override
   public void onMidi(final RGBButtonTarget target, final int status, final int data1, final int data2)
   {
      if (status == 0x90 && data1 == mData1)
      {
         target.set(data2 > 0);
      }
   }

   private int[] mLastSent = new int[] {-1, -1, -1, -1};
   private final int mData1;
}
