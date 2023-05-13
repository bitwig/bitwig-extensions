package com.bitwig.extensions.controllers.novation.launchkey_mk3;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiOut;

public class LcdDisplay {

   private final static String LAUNCHKEY_PREFIX = "f0 00 20 29 02 0f ";

   private final MidiOut midiOut;
   private final ControllerHost host;

   public LcdDisplay(final MidiOut midiOut, final ControllerHost host) {
      this.midiOut = midiOut;
      this.host = host;
   }

   public void sendText(final String text, final int row) {
      final StringBuilder sb = new StringBuilder(LAUNCHKEY_PREFIX);
      sb.append("04 ");
      sb.append(String.format("%02x ", row));
      sb.append(toSysEx(text));
      sb.append("f7");
      midiOut.sendSysex(sb.toString());
   }

   public void setParameter(final String text, final int controllerIndex) {
      final StringBuilder sb = new StringBuilder(LAUNCHKEY_PREFIX);
      sb.append("07 ");
      sb.append(String.format("%02x ", controllerIndex));
      sb.append(toSysEx(text));
      sb.append("f7");
      midiOut.sendSysex(sb.toString());
   }

   public void setValue(final String text, final int controllerIndex) {
      final StringBuilder sb = new StringBuilder(LAUNCHKEY_PREFIX);
      sb.append("08 ");
      sb.append(String.format("%02x ", controllerIndex));
      sb.append(toSysEx(text));
      sb.append("f7");
      midiOut.sendSysex(sb.toString());
   }

   public void clearDisplay() {
      midiOut.sendSysex(LAUNCHKEY_PREFIX + "06 f7");
   }

   public static String toSysEx(final String text) {
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < text.length(); i++) {
         final char c = convert(text.charAt(i));
         final String hexValue = Integer.toHexString((byte) c);
         sb.append(hexValue.length() < 2 ? "0" + hexValue : hexValue);
         sb.append(" ");
      }
      return sb.toString();
   }

   private static char convert(final char c) {
      if (c < 128) {
         return c;
      }
      switch (c) {
         case 'Á':
         case 'À':
         case 'Ä':
            return 'A';
         case 'É':
         case 'È':
            return 'E';
         case 'á':
         case 'à':
         case 'ä':
            return 'a';
         case 'Ö':
            return 'O';
         case 'Ü':
            return 'U';
         case 'è':
         case 'é':
            return 'e';
         case 'ö':
            return 'o';
         case 'ü':
            return 'u';
         case 'ß':
            return 's';
      }
      return '?';
   }
}
