package com.bitwig.extensions.controllers.novation.common;

import com.bitwig.extension.controller.api.MidiOut;

public class SimpleLed
{
   public SimpleLed(final int status, final int data1)
   {
      mStatus = status;
      mData1 = data1;
   }

   public void setColor(final int color)
   {
      if (mColor != color)
      {
         mColor = color;
         mNeedFlush = true;
      }
   }

   public void setColor(final SimpleLedColor color)
   {
      if (mColor != color.value())
      {
         mColor = color.value();
         mNeedFlush = true;
      }
   }

   public void flush(final MidiOut out, final int channel)
   {
      if (mNeedFlush)
      {
         out.sendMidi(mStatus | channel, mData1, mColor);
         mNeedFlush = false;
      }
   }

   public void flush(StringBuilder sb)
   {
      if (mNeedFlush)
      {
         sb.append(" ");
         if (mData1 < 16)
            sb.append('0');
         sb.append(Integer.toHexString(mData1));
         sb.append(" ");
         if (mColor < 16)
            sb.append('0');
         sb.append(Integer.toHexString(mColor));

      }
   }

   public int getStatus()
   {
      return mStatus;
   }

   public int getData1()
   {
      return mData1;
   }

   public void invalidate()
   {
      mNeedFlush = true;
   }

   private final int mStatus;
   private final int mData1;
   private int mColor = 0;
   private boolean mNeedFlush = true;
}
