package com.bitwig.extensions.controllers.mackie.definition;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.RelativePosition;
import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.display.MainUnitButton;

import java.util.HashMap;
import java.util.Map;

public class SimulationLayout {

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

   public void add(final BasicNoteOnAssignment assignment, final String label, final int gridX, final int gridY) {
      map.put(assignment, new Element(gridX, gridY, label, "#eee"));
   }


   public void layout(final BasicNoteOnAssignment assignment, final MainUnitButton button) {
      final Element element = map.get(assignment);
      if (element != null) {
         layout(button, element);
      }
   }

   public void layout(final MainUnitButton button) {
      final Element element = map.get(button.getAssignment());
      if (element != null) {
         layout(button, element);
      }
   }

   private void layout(final MainUnitButton button, final Element element) {
      final HardwareButton hwButton = button.getButton();
      hwButton.setLabelColor(Color.fromHex(element.getColor()));
      hwButton.setLabel(element.getLabel());
      hwButton.setBounds(200 + 20 * element.getGridX(), 10 + 20 * element.getGridY(), 15, 10);
      hwButton.setLabelPosition(RelativePosition.BELOW);
   }
}
