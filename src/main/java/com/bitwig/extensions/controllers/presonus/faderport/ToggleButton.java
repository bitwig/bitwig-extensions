package com.bitwig.extensions.controllers.presonus;

import java.util.function.BooleanSupplier;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.MidiOut;

public class ToggleButton implements Flushable, MidiReceiver
{
   public ToggleButton(final int ID)
   {
      mID = ID;
   }

   @Override
   public void flush(final MidiOut midiOut)
   {
      if (mBooleanSupplier != null)
      {
         boolean value = mBooleanSupplier.getAsBoolean();

         if (value != mLastSentValue)
         {
            midiOut.sendMidi(0x90, mID, value ? 0x7F : 0);
            mLastSentValue = value;
         }
      }
   }

   @Override
   public void onMidi(final int status, final int data1, final int data2)
   {
      if (status == 0x90 && data1 == mID && mRunnable != null && data2 > 0)
      {
         mRunnable.run();
      }
   }

   public void setBooleanValue(final BooleanValue value)
   {
      mBooleanSupplier = () -> value.get();
   }

   public void setBooleanSupplier(final BooleanSupplier value)
   {
      mBooleanSupplier = value;
   }

   public void setRunnable(final Runnable runnable)
   {
      mRunnable = runnable;
   }

   private final int mID;
   private BooleanSupplier mBooleanSupplier;
   private Runnable mRunnable;
   private boolean mLastSentValue;
}
