package com.bitwig.extensions.controllers.mackie;

import java.util.HashMap;
import java.util.Map;

public class ControllerConfig {
   private final Map<BasicNoteOnAssignment, Integer> assignOverrides;
   private final boolean hasLowerDisplay;
   private final boolean hasDedicateVu;
   private final boolean hasMasterVu;

   public ControllerConfig(final Map<BasicNoteOnAssignment, Integer> assignOverrides, final boolean hasLowerDisplay,
                           final boolean hasDedicateVu, final boolean hasMasterVu) {
      this.assignOverrides = assignOverrides;
      this.hasLowerDisplay = hasLowerDisplay;
      this.hasDedicateVu = hasDedicateVu;
      this.hasMasterVu = hasMasterVu;
   }

   public ControllerConfig(final boolean hasLowerDisplay) {
      assignOverrides = new HashMap<>();
      this.hasLowerDisplay = hasLowerDisplay;
      hasDedicateVu = true;
      hasMasterVu = false;
   }

   public boolean isHasDedicateVu() {
      return hasDedicateVu;
   }

   public boolean hasLowerDisplay() {
      return hasLowerDisplay;
   }

   public boolean hasOverrides() {
      return !assignOverrides.isEmpty();
   }

   public NoteAssignment get(final BasicNoteOnAssignment assignment) {
      if (!hasOverrides()) {
         return assignment;
      }
      final Integer override = assignOverrides.get(assignment);
      if (override != null) {
         return new OverrideNoteAssignment(override);
      }
      return assignment;
   }

   public boolean hasMasterVu() {
      return hasMasterVu;
   }

}
