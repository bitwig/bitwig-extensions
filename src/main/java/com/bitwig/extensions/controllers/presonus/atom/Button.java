package com.bitwig.extensions.controllers.presonus.atom;

import java.util.function.Consumer;

import com.bitwig.extension.controller.api.MidiOut;

public class AtomButton
{
   public AtomButton(
      final MidiOut midiOut, final int data1, final Consumer<Boolean> booleanConsumer)
   {
      mMidiOut = midiOut;
      mData1 = data1;
      mBooleanConsumer = booleanConsumer;
   }

   enum State
   {
      OFF,
      DIMMED,
      ON,
   }

   void setState(State state)
   {
      switch (state)
      {
         case OFF:
            mMidiOut.sendMidi(0xB0, mData1, 0);
            break;
         case DIMMED:
            mMidiOut.sendMidi(0xB0, mData1, 0x02);
            break;
         case ON:
            mMidiOut.sendMidi(0xB0, mData1, 127);
            break;
      }

   }

   protected final MidiOut mMidiOut;
   protected final int mData1;
   private final Consumer<Boolean> mBooleanConsumer;

   public void onMidi(final int status, final int data1, final int data2)
   {
      if (status == 176 && data1 == mData1)
      {
         mBooleanConsumer.accept(data2 > 0);
      }
   }
}
