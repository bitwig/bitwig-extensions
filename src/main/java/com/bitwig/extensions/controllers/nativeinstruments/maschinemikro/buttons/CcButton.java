package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.CcAssignment;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.MidiProcessor;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.Midi;

import java.util.function.BooleanSupplier;

public class CcButton extends GateButton {
   private final OnOffHardwareLight light;

   public CcButton(CcAssignment assignment, HardwareSurface surface, MidiProcessor midiProcessor) {
      this(assignment.getCcNr(), assignment.getChannel(), assignment.toString(), surface, midiProcessor);
   }

   public CcButton(final int midiId, final int channel, String name, HardwareSurface surface,
                   MidiProcessor midiProcessor) {
      super(midiId, midiProcessor);
      MidiIn midiIn = midiProcessor.getMidiIn();
      hwButton = surface.createHardwareButton(name + "_" + midiId);
      hwButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, midiId, 127));
      hwButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, midiId, 0));
      light = surface.createOnOffHardwareLight(name + "_LIGHT_" + midiId);
      hwButton.isPressed().markInterested();
      light.onUpdateHardware(() -> midiProcessor.sendMidi(Midi.CC, midiId, light.isOn().currentValue() ? 127 : 0));
   }

   public void bindLight(Layer layer, BooleanSupplier state) {
      layer.bind(state, light);
   }

   public void bindLightHeld(Layer layer) {
      layer.bind(hwButton.isPressed(), light);
   }

}
