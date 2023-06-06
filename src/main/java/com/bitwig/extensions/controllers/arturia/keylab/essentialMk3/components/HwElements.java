package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.CCAssignment;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.RgbButton;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.display.LcdDisplay;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;

@Component
public class HwElements {
   public static final int NUM_PADS_TRACK = 8;
   private final Map<CCAssignment, RgbButton> buttons = new HashMap<>();
   private final KeylabAbsoluteControl[] knobs = new KeylabAbsoluteControl[9];
   private final KeylabAbsoluteControl[] sliders = new KeylabAbsoluteControl[9];
   private final RgbButton[] padBankAButtons = new RgbButton[NUM_PADS_TRACK];
   private final RgbButton[] padBankBButtons = new RgbButton[NUM_PADS_TRACK];
   private final RelativeHardwareKnob mainEncoder;
   private final HardwareButton encoderPress;

   private final HardwareButton bankButton;
   private final ControllerHost host;

   public HwElements(final ControllerHost host, final HardwareSurface surface, final SysExHandler sysExHandler,
                     MidiOut midiOut) {
      this.host = host;
      for (final CCAssignment assignment : CCAssignment.values()) {
         if (!assignment.isMultiBase()) {
            final RgbButton button = new RgbButton(assignment, RgbButton.Type.CC, 0, sysExHandler, surface);
            buttons.put(assignment, button);
         }
      }
      final MidiIn midiIn = sysExHandler.getMidiIn();
      mainEncoder = createMainEncoder(116, host, surface, midiIn);
      encoderPress = createEncoderPress(117, surface, midiIn);
      bankButton = surface.createHardwareButton("Bank_Button");
      bankButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, 0x76, 127));
      bankButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(0, 0x76, 0));

      for (int i = 0; i < 9; i++) {
         knobs[i] = new KeylabAbsoluteControl(LcdDisplay.ValueType.KNOB, i, surface, midiIn, midiOut);
         sliders[i] = new KeylabAbsoluteControl(LcdDisplay.ValueType.SLIDER, i, surface, midiIn, midiOut);
      }
      final int[] mapping = new int[]{4, 5, 6, 7, 0, 1, 2, 3};
      for (int i = 0; i < 8; i++) {
         padBankAButtons[i] = new RgbButton("KL_PAD_A", i + CCAssignment.PAD1_A.getItemId(), RgbButton.Type.NOTE,
            CCAssignment.PAD1_A.getCcId() + mapping[i], 10, surface, sysExHandler);
         padBankBButtons[i] = new RgbButton("KL_PAD_B", i + CCAssignment.PAD1_B.getItemId(), RgbButton.Type.NOTE,
            CCAssignment.PAD1_B.getCcId() + i, 10, surface, sysExHandler);
      }
   }

   private RelativeHardwareKnob createMainEncoder(final int ccNr, final ControllerHost host,
                                                  final HardwareSurface surface, final MidiIn midiIn) {
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

   private HardwareButton createEncoderPress(final int ccNr, final HardwareSurface surface, final MidiIn midiIn) {
      final HardwareButton encoderButton = surface.createHardwareButton("ENCODER_PUSH");

      encoderButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, ccNr, 127));
      encoderButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(0, ccNr, 0));
      return encoderButton;
   }

   public HardwareButton getBankButton() {
      return bankButton;
   }

   public RgbButton getButton(final CCAssignment assignment) {
      return buttons.get(assignment);
   }

   public RgbButton[] getPadBankAButtons() {
      return padBankAButtons;
   }

   public RgbButton[] getPadBankBButtons() {
      return padBankBButtons;
   }

   public KeylabAbsoluteControl[] getSliders() {
      return sliders;
   }

   public KeylabAbsoluteControl[] getKnobs() {
      return knobs;
   }

   public RelativeHardwareKnob getMainEncoder() {
      return mainEncoder;
   }

   public HardwareButton getEncoderPress() {
      return encoderPress;
   }
}
