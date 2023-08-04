package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.display;

import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.KeylabIcon;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components.SysExHandler;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.time.TimedDelayEvent;

@Component
public class LcdDisplay {
   public static final int MAX_TEXT_LEN = 20;

   private final SysExHandler sysExHandler;
   private long acceptTime;
   private LcdDisplayMode acceptMode = LcdDisplayMode.NONE;
   private int acceptIndex = -1;

   public enum ValueType {
      SLIDER("Fader"),
      KNOB("Encoder");
      private final String displayValue;

      ValueType(String value) {
         this.displayValue = value;
      }

      public String getDisplayValue() {
         return displayValue;
      }
   }

   public LcdDisplay(final SysExHandler sysExHandler) {
      this.sysExHandler = sysExHandler;
      acceptTime = System.currentTimeMillis();
   }

   public void enableValues(final int index, final LcdDisplayMode mode) {
      if (acceptMode != LcdDisplayMode.INIT) {
         acceptMode = mode;
         acceptIndex = index;
         acceptTime = System.currentTimeMillis();
      }
   }

   public void ping() {
      if (acceptMode != LcdDisplayMode.NONE && (System.currentTimeMillis() - acceptTime) > 2000) {
         acceptMode = LcdDisplayMode.NONE;
         acceptIndex = -1;
      }
   }

   public void logoText(final String top, final String bottom, final KeylabIcon icon) {
      final String sysex = createTopIconScreen(top, bottom, icon, true);
      sysExHandler.queueTimedEvent(new TimedDelayEvent(() -> sysExHandler.sendSysexText(sysex), 100));
   }

   public void setTopIconText(final String top, final String bottom, final KeylabIcon icon, final boolean isTransient) {
      final String sysex = createTopIconScreen(top, bottom, icon, isTransient);
      sysExHandler.sendSysexText(sysex);
   }

   private String createTopIconScreen(final String top, final String bottom, final KeylabIcon icon,
                                      final boolean isTransient) {
      final StringBuilder sysex = getScreenSysexHead("1A");
      sysex.append("01 ");
      appendString(sysex, top);
      sysex.append("02 ");
      appendString(sysex, bottom);
      sysex.append("03 ");
      sysex.append(SysExHandler.toHex(icon.getKey()));
      sysex.append(isTransient ? "01 " : "00 ");
      sysex.append("F7");
      return sysex.toString();
   }

   public void sendPopup(final String top, final String bottom, final KeylabIcon icon) {
      final String popUpTextSysEx = String.format("%s01 %s02 %s03 %02X 00 F7", getScreenSysexHead("17"),
         textToSysEx(top), textToSysEx(bottom), icon.getKey());
      sysExHandler.sendSysexText(popUpTextSysEx);
   }


   public void sendLines(final String line, final boolean isTransient) {
      final StringBuilder sysex = getScreenSysexHead("10");
      sysex.append("01 ");
      appendString(sysex, line);
      sysex.append("00 ");
      sysex.append(isTransient ? "01 " : "00 ");
      sysex.append("F7");
      sysExHandler.sendSysexText(sysex.toString());
   }

   private void sendLines(final String line1, final String line2, final boolean isTransient) {
      final StringBuilder sysex = getScreenSysexHead("12");
      sysex.append("01 ");
      appendString(sysex, line1);
      sysex.append("00 ");
      sysex.append("02 ");
      appendString(sysex, line2);
      sysex.append("00 ");
      sysex.append(isTransient ? "01 " : "00 ");
      sysex.append("F7");
      sysExHandler.sendSysexText(sysex.toString());
   }

   public void sendLineScroll(final String line1, final String line2, final boolean isTransient) {
      final StringBuilder sysex = getScreenSysexHead("13");
      sysex.append("01 ");
      appendString(sysex, line1);
      sysex.append("00 ");
      sysex.append("02 ");
      appendString(sysex, line2);
      sysex.append("00 ");
      sysex.append(isTransient ? "01 " : "00 ");
      sysex.append("F7");
      sysExHandler.sendSysexText(sysex.toString());
   }

   public void centerScreen() {
      final StringBuilder sysex = getScreenSysexHead("1D");
      sysex.append("01 00 00 02 06 00 03 07 00 04 0D 05 00 00 05 00 F7");
      sysExHandler.sendSysexText(sysex.toString());
   }

   public void sendNavigationPage(final ContextPageConfiguration page, final boolean isTransient) {
      if (page.getSecondaryText() == null) {
         sendLines(page.getMainText(), isTransient);
      } else {
         sendLines(page.getMainText(), page.getSecondaryText(), isTransient);
      }
      if (page.getHeaderText() != null) {
         sendHeader(page);
      }
      sendFooter(page);
   }

   public void updateFooter(final ContextPageConfiguration page) {
      sendFooter(page);
   }

   public void updateHeader(final ContextPageConfiguration page) {
      sendHeader(page);
   }

   private void sendHeader(final ContextPageConfiguration page) {
      final StringBuilder sysex = getScreenSysexHead("01");
      sysex.append("01 ");
      sysex.append(SysExHandler.toHex(page.getHeaderIcon().getKey()));
      sysex.append("00 ");
      sysex.append("02 ");
      appendString(sysex, page.getHeaderText());
      sysex.append("F7");
      sysExHandler.sendSysexText(sysex.toString());
   }

   private void sendFooter(final ContextPageConfiguration page) {
      final ContextPart[] elements = page.getContextParts();
      final StringBuilder sysex = getScreenSysexHead("03");
      for (int i = 0; i < elements.length; i++) {
         final int id = (i + 1) << 4;
         sysex.append(SysExHandler.toHex(id));
         sysex.append(elements[i].getFrame().getHexValue());
         sysex.append("00 ");
         if (!elements[i].getText().isBlank()) {
            sysex.append(SysExHandler.toHex(id + 1));
            appendString(sysex, elements[i].getText());
         }
         sysex.append(SysExHandler.toHex(id + 2));
         sysex.append(SysExHandler.toHex(elements[i].getIcon().getKey()));
         sysex.append("00 ");
      }
      sysex.append("F7");
      sysExHandler.sendSysexText(sysex.toString());
   }

   public void sendValueText(final int index, final ValueType type, final LcdDisplayMode textMode, final String top,
                             final String bottom, final int value) {
      if (textMode != acceptMode || acceptIndex != index) {
         return;
      }
      final StringBuilder sysex = getScreenSysexHead(type == ValueType.SLIDER ? "15" : "14");
      sysex.append("01 ");
      appendString(sysex, top, 12);
      sysex.append("02 ");
      appendString(sysex, bottom, 12);
      sysex.append("03 ");
      sysex.append(String.format("%02X", value));
      sysex.append("00 ");
      sysex.append("01 ");
      sysex.append("F7");
      sysExHandler.sendSysexText(sysex.toString());
   }

   private StringBuilder getScreenSysexHead(final String item) {
      final StringBuilder sysex = new StringBuilder(SysExHandler.ARTURIA_SYSEX_HEADER);
      sysex.append("04 01 "); // Command ID + Patch ID
      sysex.append("60 "); // paramtype
      sysex.append(item); // paramtype
      sysex.append(" "); // paramtype
      return sysex;
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

   private static void appendString(final StringBuilder sb, final String text, final int maxTextLen) {
      for (int i = 0; i < text.length() && i < maxTextLen; i++) {
         final char c = convert(text.charAt(i));
         final String hexValue = Integer.toHexString((byte) c);
         sb.append(hexValue.length() < 2 ? "0" + hexValue : hexValue);
         sb.append(" ");
      }
      sb.append("00 ");
   }

   private static void appendString(final StringBuilder sb, final String text) {
      appendString(sb, text, MAX_TEXT_LEN);
   }

   private static String textToSysEx(final String text) {
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < text.length() && i < MAX_TEXT_LEN; i++) {
         final char c = convert(text.charAt(i));
         final String hexValue = Integer.toHexString((byte) c);
         sb.append(hexValue.length() < 2 ? "0" + hexValue : hexValue);
         sb.append(" ");
      }
      sb.append("00 ");
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
