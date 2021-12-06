package com.bitwig.extensions.controllers.mackie;

import java.util.HashMap;
import java.util.Map;

public class ControllerConfig {
   private final Map<BasicNoteOnAssignment, Integer> assignOverrides;
   private final boolean hasLowerDisplay;

   public ControllerConfig(final Map<BasicNoteOnAssignment, Integer> assignOverrides, final boolean hasLowerDisplay) {
      this.assignOverrides = assignOverrides;
      this.hasLowerDisplay = hasLowerDisplay;
   }

   public ControllerConfig(final boolean hasLowerDisplay) {
      assignOverrides = new HashMap<>();
      this.hasLowerDisplay = hasLowerDisplay;
   }

   public boolean hasLowerDisplay() {
      return hasLowerDisplay;
   }

   public boolean hasOverrides() {
      return assignOverrides.isEmpty();
   }

   public NoteAssignment get(final BasicNoteOnAssignment assignment) {
      if (hasOverrides()) {
         return assignment;
      }
      final Integer override = assignOverrides.get(assignment);
      if (override != null) {
         return new OverrideNoteAssignment(override);
      }
      return assignment;
   }
}
