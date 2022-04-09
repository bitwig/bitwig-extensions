package com.bitwig.extensions.controllers.mackie.definition;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.display.DisplayPart;

import java.util.HashMap;
import java.util.Map;

public class SimulationLayout {

   public static final int FADER_Y_MM = 175;
   public static final int FADER_HEIGHT_IN_MM = 100;

   public static class Element {
      private final int gridX;
      private final int gridY;
      private final String label;
      private final String color;

      public Element(final int gridX, final int gridY, final String label, final String color) {
         this.gridX = gridX;
         this.gridY = gridY;
         this.label = label;
         this.color = color;
      }

      public int getGridX() {
         return gridX;
      }

      public int getGridY() {
         return gridY;
      }

      public String getLabel() {
         return label;
      }

      public String getColor() {
         return color;
      }
   }

   private final Map<BasicNoteOnAssignment, Element> map = new HashMap<>();
   private int jogWheelX = 20 * 4;
   private int jogWheelY = 20 * 10;

   public void setSurfaceSize(final HardwareSurface surface, final int nrOfExtenders) {
      surface.setPhysicalSize(380, 300);
   }

   public void add(final BasicNoteOnAssignment assignment, final String label, final int gridX, final int gridY) {
      map.put(assignment, new Element(gridX, gridY, label, "#eee"));
   }

   public void layout(final BasicNoteOnAssignment assignment, final HardwareButton button) {
      final Element element = map.get(assignment);
      if (element != null) {
         layout(button, element);
      }
   }

   public void setJogWheelPos(final int x, final int y) {
      jogWheelX = x;
      jogWheelY = y;
   }

   private void layout(final HardwareButton hwButton, final Element element) {
      hwButton.setLabelColor(Color.fromHex(element.getColor()));
      hwButton.setLabel(element.getLabel());
      hwButton.setBounds(200 + 20 * element.getGridX(), 10 + 20 * element.getGridY(), 15, 10);
      hwButton.setLabelPosition(RelativePosition.BELOW);
   }

   public void layoutJogwheel(final RelativeHardwareKnob knob) {
      knob.setBounds(200 + jogWheelX, 20 + jogWheelY, 15 * 3, 15 * 3);
   }

   public void layoutEncoder(final int sectionIndex, final int encoderIndex, final RelativeHardwareKnob encoder) {
      encoder.setBounds(10 + encoderIndex * 20, 50, 15, 15);
      encoder.hardwareButton().setBounds(12 + encoderIndex * 20, 67, 9, 9);
      encoder.hardwareButton().setLabel("P" + (encoderIndex + 1));
   }

   public void layoutSlider(final int sectionIndex, final int sliderIndex, final HardwareSlider slider) {
      final int xInMM = 10 + sliderIndex * 20;
      slider.setBounds(xInMM, FADER_Y_MM, 15, FADER_HEIGHT_IN_MM);
      slider.setLabel("VOL " + (sliderIndex + 1));
      slider.setLabelColor(Color.fromHex("#0f0"));
      slider.setLabelPosition(RelativePosition.BELOW);
      slider.hardwareButton().setBounds(xInMM, FADER_Y_MM + FADER_HEIGHT_IN_MM + 10, 15, 7);
      slider.hardwareButton().setLabelColor(Color.fromHex("#0ff"));
      slider.hardwareButton().setLabel("TCH");
   }

   public void layoutMainSlider(final HardwareSlider slider) {
      final int xInMM = 10 + 8 * 20;
      slider.setBounds(xInMM, FADER_Y_MM, 15, FADER_HEIGHT_IN_MM);
      slider.setLabel("MST");
      slider.setLabelColor(Color.fromHex("#0f0"));
      slider.setLabelPosition(RelativePosition.BELOW);
      slider.hardwareButton().setBounds(xInMM, FADER_Y_MM + FADER_HEIGHT_IN_MM + 10, 15, 7);
      slider.hardwareButton().setLabelColor(Color.fromHex("#0ff"));
      slider.hardwareButton().setLabel("MTC");
   }

   public void layoutDisplay(final DisplayPart part, final int sectionIndex, final HardwareTextDisplay display) {
      display.line(0).backgroundColor().setValue(Color.fromHex("#fff"));
      display.setBounds(10, 10 + ((part == DisplayPart.UPPER) ? 0 : 155), 180, 15);
      for (int i = 0; i < 2; i++) {
         final HardwareTextDisplayLine line = display.line(i);
         line.text().setMaxChars(56);
         line.textColor().setValue(Color.fromHex("#f00"));
         line.backgroundColor().setValue(Color.fromHex("#00f"));
         line.text().setValue("");
      }

   }

}
