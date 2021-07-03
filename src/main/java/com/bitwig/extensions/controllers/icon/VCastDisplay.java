package com.bitwig.extensions.controllers.icon;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

import com.bitwig.extension.api.util.midi.SysexBuilder;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.HardwareTextDisplay;

/**
 * Built-in display of the VCast and VCast Pro controllers
 */
class VCastDisplay
{
   public VCastDisplay(final ControllerHost host, final HardwareSurface hardwareSurface)
   {
      mHost = host;
      mHardwareSurface = hardwareSurface;

      mCharset = getCharset();

      mDisplay = mHardwareSurface.createHardwareTextDisplay("leftDisplay", 5);

      final var midiOut = host.getMidiOutPort(0);
      for (int i = 0; i < 5; ++i)
      {
         final int line = i;
         mDisplay.line(line)
            .text()
            .onUpdateHardware((text) -> midiOut.sendSysex(sysexMessage(line, text)));
      }
   }

   public HardwareTextDisplay display()
   {
      return mDisplay;
   }

   private Charset getCharset()
   {
      try
      {
         final Charset charset = Charset.forName("GB2312");
         mHost.println("Got GB2312 charset");
         return charset;
      }
      catch (final UnsupportedCharsetException e)
      {
         mHost.println("Falling back to ASCII charset");
         return StandardCharsets.US_ASCII;
      }
   }

   private byte[] sysexMessage(final int line, final String text)
   {
      final int nBytes = line == 0 ? 20 : line == 1 ? 7 : 6;
      final byte[] bytes = convertForSysex(text, nBytes);
      final String[] sysexOffsets = {"22 00", "32 00", "52 00", "42 00", "42 06"};
      return SysexBuilder.fromHex("f0 00 00 66 14").addHex(sysexOffsets[line]).add(bytes).terminate();
   }

   private byte[] convertForSysex(final String s, final int nBytes)
   {
      final byte[] src = s.getBytes(mCharset);
      final byte[] dst = new byte[nBytes];

      int dstI = 0;
      for (final byte b : src)
      {
         if ((b & 0x80) == 0)
         {
            if (dst.length <= dstI)
            {
               break;
            }
            dst[dstI++] = b;
         }
         else
         {
            if (dst.length <= dstI + 1)
            {
               break;
            }
            dst[dstI++] = (byte) ((b & 0xF0) >> 4);
            dst[dstI++] = (byte) ((b & 0x0F));
         }
      }

      for (; dstI < dst.length; ++dstI)
      {
         dst[dstI] = (byte) ' ';
      }

      return dst;
   }

   private ControllerHost mHost;
   private HardwareSurface mHardwareSurface;

   private Charset mCharset;

   private HardwareTextDisplay mDisplay;
}
