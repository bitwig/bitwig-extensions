package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import java.util.Arrays;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.api.util.midi.SysexBuilder;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.framework.ControlElement;
import com.bitwig.extensions.framework.targets.RGBButtonTarget;

public class RGBButton extends AbstractButton implements ControlElement<RGBButtonTarget>
{
   public RGBButton(final Buttons buttonID)
   {
      super(buttonID);
   }

   @Override
   public void flush(final RGBButtonTarget target, final MidiOut midiOut)
   {
      float[] RGB = target.getRGB();
      int red = fromFloat(RGB[0]);
      int green = fromFloat(RGB[1]);
      int blue = fromFloat(RGB[2]);

      byte[] sysex = SysexBuilder.fromHex("F0 00 20 6B 7F 42 02 00 16")
      .addByte(mButtonID.getSysexID())
      .addByte(red)
      .addByte(green)
      .addByte(blue).terminate();

      if (mLastSysex == null == !Arrays.equals(mLastSysex, sysex))
      {
         midiOut.sendSysex(sysex);
         mLastSysex = sysex;
      }
   }

   @Override
   public void onMidi(final RGBButtonTarget target, final ShortMidiMessage data)
   {
      super.onMidi(target, data);
   }

   private int fromFloat(float x)
   {
      return Math.max(0, Math.min((int)(127.0 * x), 127));
   }

   private byte[] mLastSysex;
}
