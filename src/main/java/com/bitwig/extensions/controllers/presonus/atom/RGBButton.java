package com.bitwig.extensions.controllers.presonus.atom;

import java.util.function.Consumer;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.presonus.ColorSettable;

public class AtomRGBButton extends AtomButton implements ColorSettable
{
   public AtomRGBButton(
      final MidiOut midiOut, final int data1, final Consumer<Boolean> booleanConsumer)
   {
      super(midiOut, data1, booleanConsumer);
   }

   @Override
   public void setColor(final float red, final float green, final float blue)
   {
      int r128 = Math.max(0, Math.min((int)(127.0 * red), 127));
      int g128 = Math.max(0, Math.min((int)(127.0 * green), 127));
      int b128 = Math.max(0, Math.min((int)(127.0 * blue), 127));

      mMidiOut.sendMidi(0xB1, mData1, r128);
      mMidiOut.sendMidi(0xB2, mData1, g128);
      mMidiOut.sendMidi(0xB3, mData1, b128);
   }
}
