package com.bitwig.extensions.controllers.maudio.oxygenpro.control;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.controllers.maudio.oxygenpro.MidiProcessor;
import com.bitwig.extensions.controllers.maudio.oxygenpro.RgbColor;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.RgbLightState;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.Midi;

import java.util.function.Supplier;

public class PadButton extends OxygenButton {

   private final MultiStateHardwareLight light;

   public PadButton(final int midiId, final String name, final HardwareSurface surface,
                    final MidiProcessor midiProcessor) {
      super(midiId, midiProcessor);
      final MidiIn midiIn = midiProcessor.getMidiIn();
      hwButton = surface.createHardwareButton(name + "_" + midiId);
      hwButton.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(0, midiId));
      hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, midiId));
      hwButton.isPressed().markInterested();
      light = surface.createMultiStateHardwareLight(name + "_LIGHT_" + midiId);
      light.state().setValue(RgbLightState.OFF);
      light.setColorToStateFunction(RgbLightState::forColor);
      hwButton.isPressed().markInterested();
      light.state().onUpdateHardware(this::updateState);
      hwButton.setBackgroundLight(light);
   }

   private void updateState(final InternalHardwareLightState state) {
      if (state instanceof final RgbColor rgbState) {
         midiProcessor.sendMidi(Midi.NOTE_ON, midiId, rgbState.getStateIndex());
      }
   }

   public void bindLight(final Layer layer, final Supplier<InternalHardwareLightState> supplier) {
      layer.bindLightState(supplier, this.light);
   }

}
