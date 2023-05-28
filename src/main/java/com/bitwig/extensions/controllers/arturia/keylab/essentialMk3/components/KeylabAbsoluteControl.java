package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components;

import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.display.LcdDisplay;

public class KeylabAbsoluteControl {
   public static final int ENCODER_CC = 96;
   public static final int SLIDER_CC = 105;
   private static final int ENCODER_COMPONENT_ID = 3;
   private static final String SYS_EX_STATE = "F0 00 20 6B 7F 42 02 0F 40 %02X %02X F7";
   private static final String SYS_FORCE_VALUE = "F0 00 20 6B 7F 42 04 01 60 21 03 %02X 00 F7";

   private AbsoluteHardwareControl control;
   private LcdDisplay.ValueType valueType;
   private int index;
   private int ccNr;
   private MidiOut midiOut;

   public KeylabAbsoluteControl(LcdDisplay.ValueType valueType, int index, HardwareSurface surface, MidiIn midiIn,
                                MidiOut midiOut) {
      this.valueType = valueType;
      this.index = index;
      this.midiOut = midiOut;

      if (valueType == LcdDisplay.ValueType.KNOB) {
         this.ccNr = ENCODER_CC + index;
         control = surface.createAbsoluteHardwareKnob("KNOB_" + (index + 1));
         control.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, this.ccNr));
      } else {
         this.ccNr = SLIDER_CC + index;
         control = surface.createHardwareSlider("FADER_" + (index + 1));
         control.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, this.ccNr));
      }
   }

   public AbsoluteHardwareControl getControl() {
      return control;
   }

   public int getIndex() {
      return index;
   }

   public LcdDisplay.ValueType getValueType() {
      return valueType;
   }

   public void updateValue(int currentValue) {
      if (valueType == LcdDisplay.ValueType.KNOB) {
         midiOut.sendSysex(String.format(SYS_EX_STATE, index + ENCODER_COMPONENT_ID, currentValue));
      }
   }

   public void forceValue(boolean active) {
      if (!active) {
         midiOut.sendSysex(String.format(SYS_FORCE_VALUE, this.ccNr));
      } else {
         //midiOut.sendSysex(String.format("F0 00 20 6B 7F 42 04 01 60 20 03 %02X 00 F7", this.ccNr));
      }
   }
}
