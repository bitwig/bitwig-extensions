package com.bitwig.extensions.controllers.mackie.definition;

import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.NoteAssignment;
import com.bitwig.extensions.controllers.mackie.OverrideNoteAssignment;

import java.util.HashMap;
import java.util.Map;

public class ControllerConfig {
   private final Map<BasicNoteOnAssignment, Integer> assignOverrides;
   private final boolean hasLowerDisplay;
   private boolean hasDedicateVu;
   private boolean hasMasterVu;
   private boolean useClearDuplicateModifiers = false;
   private boolean functionSectionLayered = false;

   public ControllerConfig(final Map<BasicNoteOnAssignment, Integer> assignOverrides, final boolean hasLowerDisplay) {
      this.assignOverrides = assignOverrides;
      this.hasLowerDisplay = hasLowerDisplay;
      hasDedicateVu = false;
      hasMasterVu = false;
   }

   public ControllerConfig(final boolean hasLowerDisplay) {
      assignOverrides = new HashMap<>();
      this.hasLowerDisplay = hasLowerDisplay;
      hasDedicateVu = true;
      hasMasterVu = false;
   }

   public ControllerConfig setHasDedicateVu(final boolean hasDedicateVu) {
      this.hasDedicateVu = hasDedicateVu;
      return this;
   }

   public ControllerConfig setHasMasterVu(final boolean hasMasterVu) {
      this.hasMasterVu = hasMasterVu;
      return this;
   }

   public ControllerConfig setUseClearDuplicateModifiers(final boolean useClearDuplicateModifiers) {
      this.useClearDuplicateModifiers = useClearDuplicateModifiers;
      return this;
   }

   public ControllerConfig setFunctionSectionLayered(final boolean functionSectionLayer) {
      functionSectionLayered = functionSectionLayer;
      return this;
   }

   public boolean isFunctionSectionLayered() {
      return functionSectionLayered;
   }

   public boolean isUseClearDuplicateModifiers() {
      return useClearDuplicateModifiers;
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
