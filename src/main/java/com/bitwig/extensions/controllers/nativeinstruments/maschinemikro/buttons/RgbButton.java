package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons;

import java.util.function.Supplier;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.controllers.akai.apcmk2.led.RgbLightState;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.MidiProcessor;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.RgbColor;
import com.bitwig.extensions.framework.Layer;

public class RgbButton extends GateButton {
   private final MultiStateHardwareLight light;

   public RgbButton(final int midiId, final String name, final HardwareSurface surface, final MidiProcessor midiProcessor) {
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
   }

   private void updateState(final InternalHardwareLightState state) {
      if (state instanceof final RgbColor rgbState) {
         midiProcessor.updateColorPad(midiId, rgbState);
      }
   }

   public void bindLight(final Layer layer, final Supplier<InternalHardwareLightState> supplier) {
      layer.bindLightState(supplier, this.light);
   }

   public void bindDisabled(final Layer layer) {
      this.bindLight(layer, () -> RgbColor.OFF);
      this.bindRelease(layer, () -> {
      });
      this.bindPressed(layer, () -> {
      });
   }

}
