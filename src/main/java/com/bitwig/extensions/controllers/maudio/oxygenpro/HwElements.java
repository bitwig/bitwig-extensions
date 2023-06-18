package com.bitwig.extensions.controllers.maudio.oxygenpro;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.maudio.oxygenpro.control.CcButton;
import com.bitwig.extensions.controllers.maudio.oxygenpro.control.PadButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BooleanValueObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

@Component
public class HwElements {
   private static final int[] PAD_NOTE_NR = { //
      0x28, 0x29, 0x2A, 0x2B, 0x30, 0x31, 0x32, 0x33,  //top Row
      0x24, 0x25, 0x26, 0x27, 0x2C, 0x2D, 0x2E, 0x2F //bottom row
   };

   private static final int SLIDER_CC = 0xC;
   private static final int KNOB_CC = 0x16;
   private static final int MASTER_CC = 0x29;
   private final ControllerHost host;

   private Map<OxygenCcAssignments, CcButton> buttonMap = new HashMap<>();
   private List<CcButton> trackButtons = new ArrayList<>();
   private List<PadButton> padButtons = new ArrayList<>();
   private final HardwareSlider[] sliders = new HardwareSlider[9];
   private final HardwareSlider masterSlider;
   private final AbsoluteHardwareKnob[] knobs = new AbsoluteHardwareKnob[8];
   private final RelativeHardwareKnob mainEncoder;

   private final BooleanValueObject shiftActive = new BooleanValueObject();
   private final BooleanValueObject backActive = new BooleanValueObject();

   public HwElements(HardwareSurface surface, MidiProcessor midiProcessor, ControllerHost host, OxyConfig config) {
      this.host = host;
      MidiIn midiIn = midiProcessor.getMidiIn();

      for (int i = 0; i < config.getNumberOfControls(); i++) {
         sliders[i] = surface.createHardwareSlider("SLIDER_" + i);
         sliders[i].setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, SLIDER_CC + i));
      }
      for (int i = 0; i < config.getNumberOfControls(); i++) {
         knobs[i] = surface.createAbsoluteHardwareKnob("KNOB_" + i);
         knobs[i].setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, KNOB_CC + i));
      }
      if (config.hasMasterSlider()) {
         masterSlider = surface.createHardwareSlider("MASTER_SLIDER");
         masterSlider.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, MASTER_CC));
      } else {
         masterSlider = null;
      }

      for (int i = 0; i < config.getNumberOfControls(); i++) {
         CcButton button = new CcButton(OxygenCcAssignments.TRACK_1.getCcNr() + i, 0, "TRACK_BUTTON_" + i, surface,
            midiProcessor, true);
         trackButtons.add(button);
      }

      for (int i = 0; i < config.getNumberOfControls() * 2; i++) {
         PadButton button = new PadButton(PAD_NOTE_NR[i], "PAD_" + i, surface, midiProcessor);
         padButtons.add(button);
      }
      addButton(OxygenCcAssignments.MUTE_MODE, surface, midiProcessor, 0xf);
      addButton(OxygenCcAssignments.SOLO_MODE, surface, midiProcessor, 0xf);
      addButton(OxygenCcAssignments.SELECT_MODE, surface, midiProcessor, 0xf);
      addButton(OxygenCcAssignments.REC_MODE, surface, midiProcessor, 0xf);
      addButton(OxygenCcAssignments.OXY_MODE, surface, midiProcessor, 0xf);

      addButton(OxygenCcAssignments.PAN_MODE, surface, midiProcessor, 0xf);
      addButton(OxygenCcAssignments.DEVICE_MODE, surface, midiProcessor, 0xf);
      addButton(OxygenCcAssignments.SENDS_MODE, surface, midiProcessor, 0xf);

      addButton(OxygenCcAssignments.RECORD, surface, midiProcessor);
      addButton(OxygenCcAssignments.LOOP, surface, midiProcessor);
      addButton(OxygenCcAssignments.STOP, surface, midiProcessor);
      addButton(OxygenCcAssignments.PLAY, surface, midiProcessor);
      addButton(OxygenCcAssignments.DAW, surface, midiProcessor);
      addButton(OxygenCcAssignments.BANK_LEFT, surface, midiProcessor);
      addButton(OxygenCcAssignments.BANK_RIGHT, surface, midiProcessor);
      addButton(OxygenCcAssignments.SHIFT, surface, midiProcessor);
      addButton(OxygenCcAssignments.PRESET, surface, midiProcessor);
      addButton(OxygenCcAssignments.ENCODER_PUSH, surface, midiProcessor);
      addButton(OxygenCcAssignments.BACK, surface, midiProcessor);
      addButton(OxygenCcAssignments.FAST_RWD, surface, midiProcessor);
      addButton(OxygenCcAssignments.FAST_FWD, surface, midiProcessor);
      addButton(OxygenCcAssignments.SCENE_LAUNCH1, surface, midiProcessor);
      addButton(OxygenCcAssignments.SCENE_LAUNCH2, surface, midiProcessor);
      mainEncoder = createMainEncoder(OxygenCcAssignments.ENCODER.getCcNr(), surface, midiIn);
   }

   public BooleanValueObject getShiftActive() {
      return shiftActive;
   }

   public BooleanValueObject getBackActive() {
      return backActive;
   }

   public HardwareSlider getMasterSlider() {
      return masterSlider;
   }

   public AbsoluteHardwareKnob getKnob(int index) {
      return knobs[index];
   }

   public RelativeHardwareKnob getMainEncoder() {
      return mainEncoder;
   }

   public HardwareSlider getSlider(int index) {
      return sliders[Math.min(sliders.length - 1, index)];
   }

   private void addButton(OxygenCcAssignments ccAssignment, HardwareSurface surface, MidiProcessor midiProcessor) {
      CcButton button = new CcButton(ccAssignment.getCcNr(), ccAssignment.getChannel(), ccAssignment.toString(),
         surface, midiProcessor);
      buttonMap.put(ccAssignment, button);
   }

   private void addButton(OxygenCcAssignments ccAssignment, HardwareSurface surface, MidiProcessor midiProcessor,
                          int channel) {
      CcButton button = new CcButton(ccAssignment.getCcNr(), channel, ccAssignment.toString(), surface, midiProcessor);
      buttonMap.put(ccAssignment, button);
   }

   public CcButton getButton(OxygenCcAssignments assignment) {
      return buttonMap.get(assignment);
   }

   public List<CcButton> getTrackButtons() {
      return trackButtons;
   }

   public List<PadButton> getPadButtons() {
      return padButtons;
   }

   private RelativeHardwareKnob createMainEncoder(final int ccNr, final HardwareSurface surface, final MidiIn midiIn) {
      final RelativeHardwareKnob mainEncoder = surface.createRelativeHardwareKnob("MAIN_ENCODER+_" + ccNr);
      final RelativeHardwareValueMatcher stepUpMatcher = midiIn.createRelativeValueMatcher(
         "(status == 176 && data1 == " + ccNr + " && data2>64)", 1);
      final RelativeHardwareValueMatcher stepDownMatcher = midiIn.createRelativeValueMatcher(
         "(status == 176 && data1 == " + ccNr + " && data2<64)", -1);

      final RelativeHardwareValueMatcher matcher = host.createOrRelativeHardwareValueMatcher(stepDownMatcher,
         stepUpMatcher);
      mainEncoder.setAdjustValueMatcher(matcher);
      mainEncoder.setStepSize(1);
      return mainEncoder;
   }

   public void bindEncoder(final Layer layer, final RelativeHardwareKnob encoder, final IntConsumer action) {
      final HardwareActionBindable incAction = host.createAction(() -> action.accept(1), () -> "+");
      final HardwareActionBindable decAction = host.createAction(() -> action.accept(-1), () -> "-");
      layer.bind(encoder, host.createRelativeHardwareControlStepTarget(incAction, decAction));
   }

}
