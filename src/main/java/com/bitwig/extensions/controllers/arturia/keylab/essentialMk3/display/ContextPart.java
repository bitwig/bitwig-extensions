package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.display;

import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.KeylabIcon;

public class ContextPart {
   private String text;
   private KeylabIcon icon;
   private FrameType frame;

   public enum FrameType {
      NONE("00"),
      BAR("01"),
      FRAME_SMALL("02"),
      FRAME_FULL("03");
      private final String hexValue;

      FrameType(final String hexValue) {
         this.hexValue = hexValue;
      }

      public String getHexValue() {
         return hexValue;
      }
   }

   public ContextPart(final String text, final KeylabIcon icon, final FrameType frame) {
      this.text = text;
      this.icon = icon;
      this.frame = frame;
   }

   public String getText() {
      return text;
   }

   public KeylabIcon getIcon() {
      return icon;
   }

   public void setText(final String text) {
      this.text = text;
   }

   public void setIcon(final KeylabIcon icon) {
      this.icon = icon;
   }

   public void setFrame(final FrameType framed) {
      frame = framed;
   }

   public FrameType getFrame() {
      return frame;
   }
}
