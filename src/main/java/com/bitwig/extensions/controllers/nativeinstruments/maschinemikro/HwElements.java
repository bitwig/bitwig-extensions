package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.CcButton;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.RgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Component;

import java.util.*;
import java.util.function.IntConsumer;

@Component
public class HwElements {

   private static final int[] PAD_MAP = {0x48, 0x49, 0x4A, 0x4B, //
      0x44, 0x45, 0x46, 0x47, //
      0x40, 0x41, 0x42, 0x43, //
      0x3C, 0x3D, 0x3E, 0x3F,};

   private final ControllerHost host;
   private final Map<CcAssignment, CcButton> buttonMap = new HashMap<>();
   private final RelativeHardwareKnob mainEncoder;
   private final List<RgbButton> padButtons = new ArrayList<>();

   public HwElements(HardwareSurface surface, ControllerHost host, MidiProcessor midiProcessor) {
      this.host = host;
      Arrays.stream(CcAssignment.values())
         .forEach(ccAssignment -> buttonMap.put(ccAssignment, new CcButton(ccAssignment, surface, midiProcessor)));
      mainEncoder = createMainEncoder(0x7, surface, midiProcessor.getMidiIn());
      for (int i = 0; i < 16; i++) {
         RgbButton button = new RgbButton(PAD_MAP[i], "PAD_" + i, surface, midiProcessor);
         padButtons.add(button);
      }
   }

   public List<RgbButton> getPadButtons() {
      return padButtons;
   }

   public CcButton getButton(CcAssignment assignment) {
      return buttonMap.get(assignment);
   }

   public RelativeHardwareKnob getMainEncoder() {
      return mainEncoder;
   }

   public void bindEncoder(final Layer layer, final RelativeHardwareKnob encoder, final IntConsumer action) {
      final HardwareActionBindable incAction = host.createAction(() -> action.accept(1), () -> "+");
      final HardwareActionBindable decAction = host.createAction(() -> action.accept(-1), () -> "-");
      layer.bind(encoder, host.createRelativeHardwareControlStepTarget(incAction, decAction));
   }

   private RelativeHardwareKnob createMainEncoder(final int ccNr, final HardwareSurface surface, final MidiIn midiIn) {
      final RelativeHardwareKnob mainEncoder = surface.createRelativeHardwareKnob("MAIN_ENCODER+_" + ccNr);
      final RelativeHardwareValueMatcher stepUpMatcher = midiIn.createRelativeValueMatcher(
         "(status == 176 && data1 == " + ccNr + " && data2==1)", 1);
      final RelativeHardwareValueMatcher stepDownMatcher = midiIn.createRelativeValueMatcher(
         "(status == 176 && data1 == " + ccNr + " && data2==127)", -1);

      final RelativeHardwareValueMatcher matcher = host.createOrRelativeHardwareValueMatcher(stepDownMatcher,
         stepUpMatcher);
      mainEncoder.setAdjustValueMatcher(matcher);
      mainEncoder.setStepSize(1);
      return mainEncoder;
   }

}
