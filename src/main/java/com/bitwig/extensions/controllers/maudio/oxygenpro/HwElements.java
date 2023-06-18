package com.bitwig.extensions.controllers.maudio.oxygenpro;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.controllers.maudio.oxygenpro.control.CcButton;
import com.bitwig.extensions.controllers.maudio.oxygenpro.control.PadButton;
import com.bitwig.extensions.framework.di.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HwElements {
   private static final int[] PAD_NOTE_NR = { //
      0x28, 0x29, 0x2A, 0x2B, 0x30, 0x31, 0x32, 0x33,  //top Row
      0x24, 0x25, 0x26, 0x27, 0x2C, 0x2D, 0x2E, 0x2F //bottom row
   };

   private static final int SLIDER_CC = 0xC;
   private static final int KNOB_CC = 0x16;
   private static final int MASTER_CC = 0x29;

   private Map<OxygenCcAssignments, CcButton> buttonMap = new HashMap<>();
   private List<CcButton> trackButtons = new ArrayList<>();
   private List<PadButton> padButtons = new ArrayList<>();
   private final HardwareSlider[] sliders = new HardwareSlider[9];
   private final HardwareSlider masterSlider;
   private final AbsoluteHardwareKnob[] knobs = new AbsoluteHardwareKnob[8];

   public HwElements(HardwareSurface surface, MidiProcessor midiProcessor, OxyConfig config) {
      DebugOutOxy.println(" INIT HW");
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

      addButton(OxygenCcAssignments.LOOP, surface, midiProcessor);
      addButton(OxygenCcAssignments.STOP, surface, midiProcessor);
      addButton(OxygenCcAssignments.PLAY, surface, midiProcessor);
      addButton(OxygenCcAssignments.DAW, surface, midiProcessor);
      addButton(OxygenCcAssignments.BANK_LEFT, surface, midiProcessor);
      addButton(OxygenCcAssignments.BANK_RIGHT, surface, midiProcessor);
      addButton(OxygenCcAssignments.SHIFT, surface, midiProcessor);
      addButton(OxygenCcAssignments.ENCODER_PUSH, surface, midiProcessor);
      addButton(OxygenCcAssignments.BACK, surface, midiProcessor);
      addButton(OxygenCcAssignments.FAST_RWD, surface, midiProcessor);
      addButton(OxygenCcAssignments.FAST_FWD, surface, midiProcessor);
   }

   public HardwareSlider getMasterSlider() {
      return masterSlider;
   }

   public AbsoluteHardwareKnob getKnob(int index) {
      return knobs[index];
   }

   public HardwareSlider getSlider(int index) {
      return sliders[Math.min(sliders.length - 1, index)];
   }

   private void addButton(OxygenCcAssignments ccAssignment, HardwareSurface surface, MidiProcessor midiProcessor) {
      CcButton button = new CcButton(ccAssignment.getCcNr(), 0, ccAssignment.toString(), surface, midiProcessor);
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
}
