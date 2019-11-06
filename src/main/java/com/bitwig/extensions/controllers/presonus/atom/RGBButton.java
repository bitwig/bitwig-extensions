package com.bitwig.extensions.controllers.presonus.atom;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extensions.oldframework.ControlElement;
import com.bitwig.extensions.oldframework.LayeredControllerExtension;
import com.bitwig.extensions.oldframework.targets.ButtonTarget;
import com.bitwig.extensions.oldframework.targets.RGBButtonTarget;

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
   public void flush(final RGBButtonTarget target, final LayeredControllerExtension extension)
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
            extension.getMidiOutPort(0).sendMidi(0xB0 + i, mData1, values[i]);
            mLastSent[i] = values[i];
         }
      }
   }

   @Override
   public void onMidi(final RGBButtonTarget target, final ShortMidiMessage data)
   {
      if (data.getStatusByte() == 176 && data.getData1() == mData1)
      {
         target.set(data.getData2() > 0);
      }
   }

   private int[] mLastSent = new int[] {-1, -1, -1, -1};

   protected final int mData1;
}
