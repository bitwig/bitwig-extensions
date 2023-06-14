package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.display;

import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.KeylabIcon;

public class ContextPageConfiguration {

   private String mainText;
   private String secondaryText;
   private final ContextPart[] contextParts = new ContextPart[4];
   private String headerText;
   private KeylabIcon headerIcon = KeylabIcon.NONE;


   public ContextPageConfiguration(final String mainText, final ContextPart... context) {
      this.mainText = mainText;
      for (int i = 0; i < contextParts.length; i++) {
         if (i < context.length) {
            contextParts[i] = context[i];
         } else {
            contextParts[i] = new ContextPart("", KeylabIcon.NONE, ContextPart.FrameType.BAR);
         }
      }
   }

   public void setHeaderText(final String headerText) {
      this.headerText = headerText;
   }

   public String getHeaderText() {
      return headerText;
   }

   public void setMainText(final String text) {
      mainText = text;
   }

   public String getMainText() {
      return mainText;
   }

   public KeylabIcon getHeaderIcon() {
      return headerIcon;
   }

   public void setHeaderIcon(final KeylabIcon headerIcon) {
      this.headerIcon = headerIcon;
   }

   public String getSecondaryText() {
      return secondaryText;
   }

   public void setSecondaryText(final String secondaryText) {
      this.secondaryText = secondaryText;
   }

   public ContextPart[] getContextParts() {
      return contextParts;
   }

   public void setFramed(final int index, final ContextPart.FrameType frameType) {
      if (index < contextParts.length) {
         contextParts[index].setFrame(frameType);
      }
   }

   public void setText(final int index, final String text) {
      if (index < contextParts.length) {
         contextParts[index].setText(text);
      }
   }

}
