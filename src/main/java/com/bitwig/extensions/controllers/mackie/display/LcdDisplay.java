package com.bitwig.extensions.controllers.mackie.display;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.Midi;
import com.bitwig.extensions.controllers.mackie.StringUtil;
import com.bitwig.extensions.controllers.mackie.section.SectionType;

import java.util.Arrays;

/**
 * Represents 2x56 LCD display on the MCU or an extender.
 */
public class LcdDisplay implements DisplaySource {
   private static final int DISPLAY_LEN = 55;
   private static final int ROW2_START = 56;

   private final byte[] rowDisplayBuffer = { //
      (byte) 0XF0, 0, 0, 0X66, 0x14, 0x12, 0, // z: the grid number Zone number 0-3 * 28
      32, 32, 32, 0, 0, 0, 0, 0, 0, 0, // 7: 10 Chars
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 7: 10 Chars
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 7: 10 Chars
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 7: 10 Chars
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 7: 10 Chars
      0, 0, 0, 0, 0, 32, 32, (byte) 247};

   private final byte[] segBuffer;
   private final byte[] segBufferExp;
   private final DisplayPart part;
   public String sysHead;

   private final String[][] lastSendGrids = new String[][]{{"", "", "", "", "", "", "", "", ""}, //
      {"", "", "", "", "", "", "", "", ""}};
   private final String[] lastSentRows = new String[]{"", ""};
   private final boolean[] fullTextMode = new boolean[]{false, false};

   private final MidiOut midiOut;

   private VuMode vuMode = VuMode.LED;

   private boolean displayBarGraphEnabled = true;
   private boolean isLowerDisplay;

   private final int displayLen;

   private final int segmentLength;
   private final int segmentOffset;
   private final boolean hasDedicatedVu;

   /**
    * @param driver  the parent
    * @param midiOut the MIDI out destination for the Display
    * @param type    the main unit or a an extenter
    */
   public LcdDisplay(final MackieMcuProExtension driver, final MidiOut midiOut, final SectionType type,
                     final DisplayPart part, final boolean hasDedicatedVu) {
      this.midiOut = midiOut;
      this.hasDedicatedVu = hasDedicatedVu;
      this.part = part;
      if (part == DisplayPart.LOWER) {
         isLowerDisplay = true;
         rowDisplayBuffer[3] = 0X67;
         rowDisplayBuffer[4] = 0x15;
         rowDisplayBuffer[5] = 0x13;
         sysHead = "f0 00 00 67 15 ";
         segBuffer = new byte[]{ //
            (byte) 240, 0, 0, 0X67, 0x15, 0x13, 0, // z: the grid number Zone number 0-3 * 28
            0, 0, 0, 0, 0, 32,   // 7: 10 Chars
            (byte) 247};
         segBufferExp = new byte[]{ //
            (byte) 240, 0, 0, 0X67, 0x15, 0x13, 0, // z: the grid number Zone number 0-3 * 28
            0, 0, 0, 0, 0, 0, 32,   // 7: 10 Chars
            (byte) 247};
      } else {
         segBuffer = new byte[]{ //
            (byte) 240, 0, 0, 102, 20, 18, 0, // z: the grid number Zone number 0-3 * 28
            0, 0, 0, 0, 0, 0, 0, // 7: 10 Chars
            (byte) 247};
         segBufferExp = new byte[]{ //
            (byte) 240, 0, 0, 102, 20, 18, 0, // z: the grid number Zone number 0-3 * 28
            0, 0, 0, 0, 0, 0, 0, // 7: 10 Chars
            (byte) 247};
         if (type == SectionType.XTENDER) {
            rowDisplayBuffer[4] = 0x15;
            segBuffer[4] = 0x15;
            sysHead = "f0 00 00 66 15 ";
         } else {
            sysHead = "f0 00 00 66 14 ";
         }
      }
      displayLen = LcdDisplay.DISPLAY_LEN + (part == DisplayPart.LOWER && type != SectionType.XTENDER ? 1 : 0);

      if (part == DisplayPart.LOWER) {
         segmentLength = 6;
         segmentOffset = 2;
      } else {
         segmentLength = 7;
         segmentOffset = 0;
      }
      setVuMode(driver.getVuMode());
   }

   public int getSegmentLength() {
      return segmentLength;
   }

   public boolean isLowerDisplay() {
      return isLowerDisplay;
   }

   public void setFullTextMode(final int row, final boolean fullTextMode) {
      this.fullTextMode[row] = fullTextMode;
      setDisplayBarGraphEnabled(!isFullModeActive());
      refreshDisplay();
   }

   public void setDisplayBarGraphEnabled(final boolean displayBarGraphEnabled) {
      if (this.displayBarGraphEnabled == displayBarGraphEnabled) {
         return;
      }
      this.displayBarGraphEnabled = displayBarGraphEnabled;
      if (this.displayBarGraphEnabled) { // enable level metering in LCD
         if (vuMode != VuMode.LED) { // action only need if overall Vu Mode is actually set to such
            switchVuMode(vuMode);
         }
      } else if (vuMode != VuMode.LED) { // disable level metering in LCD
         switchVuMode(VuMode.LED);
      }
   }

   private boolean isFullModeActive() {
      return fullTextMode[0] | fullTextMode[1];
   }

   public void setVuMode(final VuMode mode) {
      if (hasDedicatedVu) {
         return;
      }
      vuMode = mode;
      if (!isFullModeActive()) {
         switchVuMode(mode);
         refreshDisplay();
      }
   }

   private void switchVuMode(final VuMode mode) {
      switch (mode) {
         case LED:
            midiOut.sendSysex(sysHead + "21 01 f7"); // Vertical VU
            for (int i = 0; i < 8; i++) {
               midiOut.sendMidi(Midi.CHANNEL_AT, i << 4, 0);
               midiOut.sendSysex(sysHead + "20 0" + i + " 01 f7");
            }
            break;
         case LED_LCD_VERTICAL:
            midiOut.sendSysex(sysHead + "21 01 f7"); // Vertical VU
            for (int i = 0; i < 8; i++) {
               midiOut.sendSysex(sysHead + "20 0" + i + " 03 f7");
               midiOut.sendMidi(Midi.CHANNEL_AT, i << 4, 0);
            }
            midiOut.sendSysex(sysHead + "20 00 03 f7");
            break;
         case LED_LCD_HORIZONTAL:
            midiOut.sendSysex(sysHead + "21 00 f7"); // Horizontal VU
            for (int i = 0; i < 8; i++) {
               midiOut.sendSysex(sysHead + "20 0" + i + " 03 f7");
               midiOut.sendMidi(Midi.CHANNEL_AT, i << 4, 0);
            }
            break;
      }
   }

   private void resetGrids(final int row) {
      Arrays.fill(lastSendGrids[row], "      ");
   }

   public void centerText(final DisplaySource source, final int row, final String text) {
      sendToDisplay(source, row, pad4Center(text));
   }

   public void centerText(final int row, final String text) {
      sendToDisplay(this, row, pad4Center(text));
   }

   @Override
   public String toString() {
      return "LcdDisplay " + part;
   }

   private String pad4Center(final String text) {
      final int fill = displayLen - text.length();
      if (fill < 0) {
         return text.substring(0, displayLen);
      }
      if (fill < 2) {
         return text;
      }
      return StringUtil.padString(text, fill / 2);
   }

   public void sendToDisplay(final DisplaySource source, final int row, final String text) {
      if (text.equals(lastSentRows[row])) {
         return;
      }
      lastSentRows[row] = text;
      resetGrids(row);
      sendFullRow(row, text);
   }

   private void sendFullRow(final int row, final String text) {
      rowDisplayBuffer[6] = (byte) (row * LcdDisplay.ROW2_START);
      final char[] ca = text.toCharArray();
      for (int i = 0; i < displayLen; i++) {
         rowDisplayBuffer[i + 7] = i < ca.length ? (byte) ca[i] : 32;
      }
      midiOut.sendSysex(rowDisplayBuffer);
   }

   public void sendToRow(final DisplaySource source, final int row, final int segment, final String text) {
      if (row > 1 || row < 0) {
         return;
      }
      if (!text.equals(lastSendGrids[row][segment])) {
         lastSendGrids[row][segment] = text;
         sendTextSeg(source, row, segment, text);
      }
   }

   public void sendToRowFull(final DisplaySource source, final int row, final int segment, final String text) {
      if (row > 1 || row < 0) {
         return;
      }
      if (!text.equals(lastSendGrids[row][segment])) {
         lastSendGrids[row][segment] = text;
         sendTextSegFull(source, row, segment, text);
      }
   }

   private void sendTextSegFull(final DisplaySource source, final int row, final int segment, final String text) {
      segBuffer[6] = (byte) (row * LcdDisplay.ROW2_START + segment * segmentLength + segmentOffset);
      final char[] ca = text.toCharArray();
      for (int i = 0; i < segmentLength; i++) {
         segBuffer[i + 7] = i < ca.length ? (byte) ca[i] : 32;
      }
      midiOut.sendSysex(segBuffer);
   }

   private void sendTextSeg(final DisplaySource source, final int row, final int segment, final String text) {
      final char[] ca = text.toCharArray();
      if (segment == 8) {
         if (isLowerDisplay()) {
            handleLastCell(row, segment, ca);
         }
      } else {
         segBuffer[6] = (byte) (row * LcdDisplay.ROW2_START + segment * segmentLength + segmentOffset);
         for (int i = 0; i < segmentLength - 1; i++) {
            segBuffer[i + 7] = i < ca.length ? (byte) ca[i] : 32;
         }
         if (segment < segmentLength) {
            segBuffer[6 + segmentLength] = ' ';
         }
         midiOut.sendSysex(segBuffer);
      }
   }

   private void handleLastCell(final int row, final int segment, final char[] ca) {
      segBufferExp[6] = (byte) (row * LcdDisplay.ROW2_START + segment * segmentLength + segmentOffset);
      for (int i = 0; i < segmentLength; i++) {
         segBufferExp[i + 7] = i < ca.length ? (byte) ca[i] : 32;
      }
      if (segment < segmentLength + 1) {
         segBufferExp[6 + segmentLength] = ' ';
      }
      midiOut.sendSysex(segBufferExp);
   }

   public void refreshDisplay() {
      for (int row = 0; row < 2; row++) {
         if (fullTextMode[row]) {
            sendFullRow(row, lastSentRows[row]);
         } else {
            for (int segment = 0; segment < 8; segment++) { // TODO segment 9
               sendTextSeg(this, row, segment, lastSendGrids[row][segment]);
            }
         }
      }
   }

   public void sendChar(final int index, final char cx) {
//		segBuffer[6] = (byte) index;
//		for (int i = 0; i < 6; i++) {
//			segBuffer[i + 7] = (byte) cx;
//		}
//		midiOut.sendSysex(segBuffer);
      // midiOut.sendSysex(SYS_HEAD + "12 00 03 03 03 03 03 03 f7");
//		midiOut.sendSysex(SYS_HEAD + "0a 01 f7");
      // midiOut.sendSysex(SYS_HEAD + "20 00 01 f7");
      // midiOut.sendMidi(Midi.CHANNEL_AT, 0x08, 0);
      midiOut.sendMidi(Midi.CC, 0x30, cx);
   }

   public void clearAll() {
      midiOut.sendSysex(sysHead + "62 f7");
      sendToDisplay(this, 0, "");
      sendToDisplay(this, 1, "");
   }

   public void exitMessage() {
      midiOut.sendSysex(sysHead + "62 f7");
      centerText(this, 0, "Bitwig Studio");
      centerText(this, 1, "... not running ...");
   }

   public void clearText() {
      sendToDisplay(this, 0, "");
      sendToDisplay(this, 1, "");
   }

   @Override
   public boolean isActive() {
      return true;
   }

}
