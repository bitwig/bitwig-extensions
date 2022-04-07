package com.bitwig.extensions.controllers.mackie.definition;

import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.NoteAssignment;
import com.bitwig.extensions.controllers.mackie.OverrideNoteAssignment;
import com.bitwig.extensions.controllers.mackie.display.MainUnitButton;

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

   public ControllerConfig(final Map<BasicNoteOnAssignment, Integer> assignOverrides,
                           final ManufacturerType manufacturerType, final SubType subType,
                           final boolean hasLowerDisplay) {
      this.assignOverrides = assignOverrides;
      this.hasLowerDisplay = hasLowerDisplay;
      this.manufacturerType = manufacturerType;
      this.subType = subType;
      hasDedicateVu = false;
      hasMasterVu = false;
   }

   public ControllerConfig(final boolean hasLowerDisplay) {
      assignOverrides = new HashMap<>();
      this.hasLowerDisplay = hasLowerDisplay;
      manufacturerType = ManufacturerType.MACKIE;
      hasDedicateVu = true;
      hasMasterVu = false;
      subType = SubType.UNSPECIFIED;
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

   public void simuLayout(final BasicNoteOnAssignment assignment, final MainUnitButton button)
   {
      switch(assignment) {
         case CANCEL: button.configureSimulator("CANCEL", 6,7);break;
         case SHIFT: button.configureSimulator("SHIFT", 4,5);break;
         case OPTION: button.configureSimulator("OPT", 5,5);break;
         case ALT: button.configureSimulator("DEL", 4,6);break;
         case CONTROL: button.configureSimulator("DUP", 5,6);break;

         case DISPLAY_NAME: button.configureSimulator("NM/V", 1,0);break;
         case DISPLAY_SMPTE: button.configureSimulator("SMPT", 2,0); break;
         case GROUP: button.configureSimulator("LNCH", 6,0);break;
         case NUDGE: button.configureSimulator("KEY", 4, 0); break;
         case STEP_SEQ: button.configureSimulator("STEP", 5,0);break;
         case F1: button.configureSimulator("F1", 0,1);break;
         case F2: button.configureSimulator("F2", 1,1);break;
         case F3: button.configureSimulator("F3", 2,1);break;
         case F4: button.configureSimulator("F4", 3,1);break;
         case F5: button.configureSimulator("F5", 4,1);break;
         case F6: button.configureSimulator("F6", 5,1);break;
         case F7: button.configureSimulator("F7", 6,1);break;
         case F8: button.configureSimulator("F8", 7,1);break;

         case GV_MIDI_LF1: button.configureSimulator("MI-DV", 0, 2); break;
         case GV_INPUTS_LF2: button.configureSimulator("x2", 1,2);break;
         case GV_AUDIO_LF3: button.configureSimulator("x3", 2,2);break;
         case GV_INSTRUMENT_LF4: button.configureSimulator("x4", 3,2);break;
         case GV_AUX_LF5: button.configureSimulator("x5", 4,2);break;
         case GV_BUSSES_LF6: button.configureSimulator("x6", 5,2);break;
         case GV_OUTPUTS_LF7: button.configureSimulator("x7", 6,2);break;
         case GV_USER_LF8: button.configureSimulator("x8", 7,2);break;

         case REWIND: button.configureSimulator("<<", 2, 9); break;
         case FFWD: button.configureSimulator(">>", 3, 9); break;
         case CYCLE: button.configureSimulator("<lp>", 4, 9); break;
         case PLAY: button.configureSimulator(">", 6, 9); break;
         case RECORD: button.configureSimulator("rec", 7, 9);break;
         case STOP: button.configureSimulator("stop", 5, 9);break;
         case CLIP_OVERDUB: button.configureSimulator("ovr", 7, 7);break;

         case CURSOR_LEFT: button.configureSimulator("<", 0, 11); break;
         case CURSOR_RIGHT: button.configureSimulator(">", 2, 11); break;
         case CURSOR_UP: button.configureSimulator("^", 1, 10); break;
         case CURSOR_DOWN: button.configureSimulator("v", 1, 12); break;
         case ZOOM: button.configureSimulator("Zoom", 1, 11); break;
         case BANK_LEFT: button.configureSimulator("<B", 4, 8); break;
         case BANK_RIGHT: button.configureSimulator("B>", 5, 8); break;
         case TRACK_LEFT: button.configureSimulator("<T", 6, 8); break;
         case TRACK_RIGHT: button.configureSimulator("T>", 7, 8); break;
         case FLIP: button.configureSimulator("FLIP", 5, 7); break;
         default:
            // currently ignore
      }
   }
}
