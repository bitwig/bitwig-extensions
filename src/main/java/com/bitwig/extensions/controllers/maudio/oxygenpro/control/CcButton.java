package com.bitwig.extensions.controllers.maudio.oxygenpro.control;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extensions.controllers.maudio.oxygenpro.MidiProcessor;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.Midi;

import java.util.function.BooleanSupplier;

public class CcButton extends OxygenButton {
   private final OnOffHardwareLight light;

   public CcButton(final int midiId, final int channel, String name, HardwareSurface surface,
                   MidiProcessor midiProcessor) {
      this(midiId, channel, name, surface, midiProcessor, false);
   }

   public CcButton(final int midiId, final int channel, String name, HardwareSurface surface,
                   MidiProcessor midiProcessor, boolean withLight) {
      super(midiId, midiProcessor);
      MidiIn midiIn = midiProcessor.getMidiIn();
      hwButton = surface.createHardwareButton(name + "_" + midiId);
      hwButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, midiId, 127));
      hwButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, midiId, 0));
      if (withLight) {
         light = surface.createOnOffHardwareLight(name + "_LIGHT_" + midiId);
         hwButton.isPressed().markInterested();
         light.onUpdateHardware(() -> midiProcessor.sendMidi(Midi.CC, midiId, light.isOn().currentValue() ? 127 : 0));
      } else {
         light = null;
      }
   }

   public void bindLight(Layer layer, BooleanSupplier state) {
      if (light != null) {
         layer.bind(state, light);
      }
   }

}
