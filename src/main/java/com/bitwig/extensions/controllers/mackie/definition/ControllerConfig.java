package com.bitwig.extensions.controllers.mackie.definition;

import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.NoteAssignment;
import com.bitwig.extensions.controllers.mackie.OverrideNoteAssignment;

import java.util.HashMap;
import java.util.Map;

public class ControllerConfig {
   private final Map<BasicNoteOnAssignment, Integer> assignOverrides;
   private final boolean hasLowerDisplay;
   private final ManufacturerType manufacturerType;
   private final SubType subType;
   private boolean hasDedicateVu;
   private boolean hasMasterVu;
   private boolean useClearDuplicateModifiers = false;
   private boolean functionSectionLayered = false;
   private final SimulationLayout simulationLayout;
   private final boolean has2ClickResolution;

   public ControllerConfig(final Map<BasicNoteOnAssignment, Integer> assignOverrides,
                           final ManufacturerType manufacturerType, final SubType subType,
                           final boolean hasLowerDisplay, final boolean has2ClickResolution) {
      this.assignOverrides = assignOverrides;
      this.hasLowerDisplay = hasLowerDisplay;
      this.manufacturerType = manufacturerType;
      this.subType = subType;
      hasDedicateVu = false;
      hasMasterVu = false;
      simulationLayout = new SimulationLayout();
      this.has2ClickResolution = has2ClickResolution; // if encoders needs 2 clicks to register step (iCon)
   }

   public ControllerConfig(final boolean hasLowerDisplay) {
      assignOverrides = new HashMap<>();
      this.hasLowerDisplay = hasLowerDisplay;
      manufacturerType = ManufacturerType.MACKIE;
      hasDedicateVu = true;
      hasMasterVu = false;
      subType = SubType.UNSPECIFIED;
      simulationLayout = new SimulationLayout();
      has2ClickResolution = false;
   }

   public boolean isHas2ClickResolution() {
      return has2ClickResolution;
   }

   public SimulationLayout getSimulationLayout() {
      return simulationLayout;
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

   public ManufacturerType getManufacturerType() {
      return manufacturerType;
   }

   public SubType getSubType() {
      return subType;
   }

   public boolean hasMasterVu() {
      return hasMasterVu;
   }

}
