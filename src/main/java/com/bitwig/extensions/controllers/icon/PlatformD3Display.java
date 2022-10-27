package com.bitwig.extensions.controllers.icon;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Function;

import com.bitwig.extension.api.util.midi.SysexBuilder;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.HardwareTextDisplay;
import com.bitwig.extension.controller.api.MidiOut;

/**
 * External Display that can be plugged into iCON controllers
 *
 * The hardware contains 8 small displays, which share one continuous buffer for their contents.
 */
class PlatformD3Display
{
   public PlatformD3Display(final MidiOut midiOut, final HardwareSurface mHardwareSurface)
   {
      mMidiOut = midiOut;

      for (int i = 0; i < mDisplays.length; ++i)
      {
         final int display = i;
         mDisplays[i] = mHardwareSurface.createHardwareTextDisplay("topDisplay" + i, 2);
         mDisplays[i].onUpdateHardware(() -> {
            setTopDisplayText(display, 0);
            setTopDisplayText(display, 1);
         });
      }
      Arrays.fill(mText, (byte) ' ');
   }

   public void flush()
   {
      // The top display is a single long byte sequence. We can update it in chunks. To update a chunk, we need to
      // transfer 8 + n bytes where n is the number of bytes in the chunk. This means (in general), that sending
      // many small chunks is bad when a lot has changed. On the other hand, sending the entire display is
      // bad when only very little has changed. This function finds chunks that minimize the number of bytes to
      // transfer.
      final Function<Integer, Integer> nextMismatch = (first) -> {
         final int relative = Arrays.mismatch(
            mText,
            first,
            mText.length, mTextBefore,
            first,
            mTextBefore.length);
         return relative == -1 ? -1 : first + relative;
      };
      int first = -1;
      while ((first = nextMismatch.apply(first + 1)) != -1)
      {
         // Find end of chunk
         int last = first;
         for (int i = first; i < mText.length; ++i)
         {
            if (mText[i] != mTextBefore[i])
            {
               last = i;
            }
            else
            {
               if (i - last > 8)
               {
                  break;
               }
            }
         }

         // Send chunk
         final SysexBuilder builder = SysexBuilder.fromHex("f0 00 00 66 14 12").addByte(first);
         for (int i = first; i <= last; ++i)
         {
            builder.addByte(mText[i]);
            mTextBefore[i] = mText[i];
         }
         mMidiOut.sendSysex(builder.terminate());
      }
   }

   public HardwareTextDisplay display(final int i)
   {
      return mDisplays[i];
   }

   private void setTopDisplayText(final int display, final int line)
   {
      final int actualLine = 1 - line; // lines are bottom to top on the hardware

      // Reset display segment
      final int offset = BYTES_PER_LINE * actualLine + BYTES_PER_DISPLAY * display;
      for (int i = 0; i < BYTES_PER_DISPLAY; ++i)
      {
         mText[offset + i] = ' ';
      }

      // Fill with actual info
      final byte[] bytes = mDisplays[display].line(line).text().currentValue().getBytes(StandardCharsets.US_ASCII);
      for (int i = 0; i < bytes.length; ++i)
      {
         if (i == BYTES_PER_DISPLAY)
         {
            return;
         }
         mText[offset + i] = bytes[i];
      }
   }

   private final int BYTES_PER_DISPLAY = 7;
   private final int BYTES_PER_LINE = 8 * BYTES_PER_DISPLAY;

   private MidiOut mMidiOut;

   private final HardwareTextDisplay[] mDisplays = new HardwareTextDisplay[8];

   private final byte[] mText = new byte[2 * BYTES_PER_LINE];
   private final byte[] mTextBefore = new byte[2 * BYTES_PER_LINE];
}
