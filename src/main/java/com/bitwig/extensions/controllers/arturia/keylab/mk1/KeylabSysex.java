package com.bitwig.extensions.controllers.arturia.keylab.mk1;

import com.bitwig.extension.api.util.midi.SysexBuilder;
import com.bitwig.extension.controller.api.MidiOut;
import static com.bitwig.extensions.controllers.arturia.keylab.mk1.ArturiaKeylabControllerExtension.*;

public class KeylabSysex
{
   static void configureDeviceUsingSysex(final MidiOut midiOut, final boolean is88)
   {
      for (int i = 0; i < 10; i++)
      {
         configureEncoder(midiOut, ENCODER1_SYSEX[i], ENCODER1_CCS[i], true);
         configureEncoder(midiOut, ENCODER2_SYSEX[i], ENCODER2_CCS[i], true);
      }
      // Volume Encoder to relative:
      configureEncoder(midiOut, 0x30, 7, true);
      // Param Encoder to relative:
      configureEncoder(midiOut, 0x31, 112, true);
      // Value Encoder to relative
      configureEncoder(midiOut, 0x33, 114, true);

      // Set global Relative Mode:
      SysexBuilder.fromHex("F0 00 20 6B 7F 42 02 00 40 02 7F F7");

      if (is88)
      {
         // CCs doesn't seem to work for some unknown reason
         /*configureControls(midiOut, 0x5B, new int[] {0x08, 0x00, 0x20, 0x00, 0x7F, 0x1});
         configureControls(midiOut, 0x5C, new int[] {0x08, 0x00, 0x21, 0x00, 0x7F, 0x1});
         configureControls(midiOut, 0x59, new int[] {0x08, 0x00, 0x22, 0x00, 0x7F, 0x1});
         configureControls(midiOut, 0x58, new int[] {0x08, 0x00, 20, 0x00, 0x7F, 0x1});
         configureControls(midiOut, 0x5A, new int[] {0x08, 0x00, 21, 0x00, 0x7F, 0x1});
         configureControls(midiOut, 0x5D, new int[] {0x08, 0x00, 0x37, 0x00, 0x7F, 0x1});*/

         // setup MMC instead
         configureControls(midiOut, 0x5B, new int[] {0x07, 0x00, 0x05, 0x00, 0x7F, 0x1});
         configureControls(midiOut, 0x5C, new int[] {0x07, 0x00, 0x04, 0x00, 0x7F, 0x1});
         configureControls(midiOut, 0x59, new int[] {0x07, 0x00, 0x01, 0x00, 0x7F, 0x1});
         configureControls(midiOut, 0x58, new int[] {0x07, 0x00, 0x02, 0x00, 0x7F, 0x1});
         configureControls(midiOut, 0x5A, new int[] {0x07, 0x00, 0x06, 0x00, 0x7F, 0x1});
         configureControls(midiOut, 0x5D, new int[] {0x07, 0x00, 0x0B, 0x00, 0x7F, 0x1});
      }
      else
      {
         // Transport:
         // Rewind to CC
         configureControls(midiOut, 0x5B, new int[] {0x01, 0x00, 0x20, 0x00, 0x7F});
         // Fast Forward to CC
         configureControls(midiOut, 0x5C, new int[] {0x01, 0x00, 0x21, 0x00, 0x7F});
         // Stop to CC
         configureControls(midiOut, 0x59, new int[] {0x01, 0x00, 0x22, 0x00, 0x7F});
         // Play to CC
         configureControls(midiOut, 0x58, new int[] {0x01, 0x00, 20, 0x00, 0x7F});
         // Record to CC
         configureControls(midiOut, 0x5A, new int[] {0x01, 0x00, 21, 0x00, 0x7F});
         // Loop to CC 55
         configureControls(midiOut, 0x5D, new int[] {0x01, 0x00, 0x37, 0x00, 0x7F});
      }

      // Button Row:
      // Button Prog. Chng.
      configureControls(midiOut, 0x12, new int[]{0x05, 0x00, 0x16, 0x16, 0x68});
      // Button Recall
      configureControls(midiOut, 0x13, new int[]{0x05, 0x00, 0x17, 0x17, 0x69});
      // Button Store
      configureControls(midiOut, 0x14, new int[]{0x05, 0x00, 0x18, 0x18, 0x6A});
      // Button Global
      configureControls(midiOut, 0x15, new int[]{0x05, 0x00, 0x19, 0x19, 0x6B});
      // Button Curve
      configureControls(midiOut, 0x16, new int[]{0x05, 0x00, 0x1A, 0x1A, 0x6C});
      // Button Mode
      configureControls(midiOut, 0x17, new int[]{0x05, 0x00, 0x1B, 0x1B, 0x6D});
      // Button Midi Ch.
      configureControls(midiOut, 0x18, new int[]{0x05, 0x00, 0x1C, 0x1C, 0x6E});
      // Button CC
      configureControls(midiOut, 0x19, new int[]{0x05, 0x00, 0x1D, 0x1D, 0x6F});
      // Button Min LSB
      configureControls(midiOut, 0x1A, new int[]{0x05, 0x00, 0x1E, 0x1E, 0x74});
      // Button Max MSB
      configureControls(midiOut, 0x1B, new int[]{0x05, 0x00, 0x1F, 0x1F, 0x75});

      // Bank 1
      configureControls(midiOut, 0x1D, new int[]{0x01, 0x00, 0x2F, 0x00, 0x7F});
      // Bank 2
      configureControls(midiOut, 0x1C, new int[]{0x01, 0x01, 0x2E, 0x00, 0x7F});

      // Sound
      configureControls(midiOut, 0x1E, new int[]{0x01, 0x00, 0x76, 0x00, 0x7F});
      // Multi
      configureControls(midiOut, 0x1F, new int[]{0x01, 0x00, 0x77, 0x00, 0x7F});

      // Fader 1 - 9 / Bank 1 & 2
      configureControls(midiOut, 0x0B, new int[]{0x01, 0, 0x49, 0, 0x7F});
      configureControls(midiOut, 0x2B, new int[]{0x01, 0, 0x43, 0, 0x7F});
      configureControls(midiOut, 0x0C, new int[]{0x01, 0, 0x4B, 0, 0x7F});
      configureControls(midiOut, 0x2C, new int[]{0x01, 0, 0x44, 0, 0x7F});
      configureControls(midiOut, 0x0D, new int[]{0x01, 0, 0x4F, 0, 0x7F});
      configureControls(midiOut, 0x2D, new int[]{0x01, 0, 0x45, 0, 0x7F});
      configureControls(midiOut, 0x0E, new int[]{0x01, 0, 0x48, 0, 0x7F});
      configureControls(midiOut, 0x2E, new int[]{0x01, 0, 0x46, 0, 0x7F});
      configureControls(midiOut, 0x4B, new int[]{0x01, 0, 0x50, 0, 0x7F});
      configureControls(midiOut, 0x6B, new int[]{0x01, 0, 0x57, 0, 0x7F});
      configureControls(midiOut, 0x4C, new int[]{0x01, 0, 0x51, 0, 0x7F});
      configureControls(midiOut, 0x6C, new int[]{0x01, 0, 0x58, 0, 0x7F});
      configureControls(midiOut, 0x4D, new int[]{0x01, 0, 0x52, 0, 0x7F});
      configureControls(midiOut, 0x6D, new int[]{0x01, 0, 0x59, 0, 0x7F});
      configureControls(midiOut, 0x4E, new int[]{0x01, 0, 0x53, 0, 0x7F});
      configureControls(midiOut, 0x6E, new int[]{0x01, 0, 0x5A, 0, 0x7F});
      configureControls(midiOut, 0x4F, new int[]{0x01, 0, 0x55, 0, 0x7F});
      configureControls(midiOut, 0x6F, new int[]{0x01, 0, 0x5C, 0, 0x7F});

      setKnobFix(midiOut, false);
   }

   static void setKnobFix(final MidiOut midiOut, final boolean knobFix)
   {
      midiOut.sendSysex(knobFix
         ? "F0 00 20 6B 7F 42 02 00 40 0D 7F F7"
         : "F0 00 20 6B 7F 42 02 00 40 0D 01 F7");
   }

   public static void resetToAbsoluteMode(final MidiOut midiOut)
   {
      // Set Encoders back to absolute:
      for(int i = 0; i < 10; i++)
      {
         configureEncoder(midiOut, ENCODER1_SYSEX[i], ENCODER1_CCS[i], false);
         configureEncoder(midiOut, ENCODER2_SYSEX[i], ENCODER2_CCS[i], false);
      }
      // Volume Encoder to Absolute:
      configureEncoder(midiOut, 0x30, 7, false);

      // Set global Absolute Mode:
      SysexBuilder.fromHex("F0 00 20 6B 7F 42 02 00 40 02 01 F7");
   }

   static void configureControls(final MidiOut midiOut, int index, int[] values)
   {
      assert (values.length == 5 || values.length == 6);

      int loopIndex = 1;

      for(int value : values)
      {
         String header = "F0 00 20 6B 7F 42 02 00";
         midiOut.sendSysex(
            SysexBuilder.fromHex(header).addByte(loopIndex++).addByte(index).addByte(value).addHex("F7").array());
      }
   }

   static void configureEncoder(
      final MidiOut midiOut, int index, int cc, boolean relative)
   {
      int mode = relative ? 2 : 1;
      int min = 0;
      int max = 127;

      int values[] = {mode, 0, cc, min, max};
      int loopIndex = 1;

      for(int value : values)
      {
         String header = "F0 00 20 6B 7F 42 02 00";
         midiOut.sendSysex(
            SysexBuilder.fromHex(header).addByte(loopIndex++).addByte(index).addByte(value).addHex("F7").array());
      }
   }
}
