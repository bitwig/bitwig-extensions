package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import java.util.Arrays;

import com.bitwig.extension.api.util.midi.SysexBuilder;
import com.bitwig.extensions.oldframework.ControlElement;
import com.bitwig.extensions.oldframework.LayeredControllerExtension;
import com.bitwig.extensions.oldframework.targets.ButtonTarget;

public class Button extends AbstractButton implements ControlElement<ButtonTarget>, Resetable
{
   public Button(final ButtonId buttonID)
   {
      super(buttonID);
   }

   @Override
   public void flush(final ButtonTarget target, final LayeredControllerExtension extension)
   {
      int intensity = target.get() ? 0x7f : 0x04;

      byte[] sysex = SysexBuilder.fromHex("F0 00 20 6B 7F 42 02 00 10")
         .addByte(mButtonID.getSysexID())
         .addByte(intensity).terminate();

      if (mLastSysex == null || !Arrays.equals(mLastSysex, sysex))
      {
         extension.getMidiOutPort(1).sendSysex(sysex);
         mLastSysex = sysex;
      }
   }

   @Override
   public void reset()
   {
      mLastSysex = null;
   }
}
