package com.bitwig.extensions.controllers.presonus.atom;

import java.util.function.Consumer;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.presonus.ButtonTarget;
import com.bitwig.extensions.controllers.presonus.ColorSettable;
import com.bitwig.extensions.controllers.presonus.Target;

public class RGBButton extends Button implements ColorSettable
{
   public RGBButton(final int data1)
   {
      super(data1);
   }

   @Override
   public void setColor(final float red, final float green, final float blue)
   {
      int r128 = Math.max(0, Math.min((int)(127.0 * red), 127));
      int g128 = Math.max(0, Math.min((int)(127.0 * green), 127));
      int b128 = Math.max(0, Math.min((int)(127.0 * blue), 127));

      /*mMidiOut.sendMidi(0xB1, mData1, r128);
      mMidiOut.sendMidi(0xB2, mData1, g128);
      mMidiOut.sendMidi(0xB3, mData1, b128);*/
   }

   @Override
   public void flush(
      final ButtonTarget target, final MidiOut midiOut)
   {
      super.flush(target, midiOut);
   }
}
