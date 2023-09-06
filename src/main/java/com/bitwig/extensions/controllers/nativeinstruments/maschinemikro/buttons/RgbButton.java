package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.controllers.akai.apcmk2.led.RgbLightState;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.MidiProcessor;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.RgbColor;
import com.bitwig.extensions.framework.Layer;

import java.util.function.Supplier;

public class RgbButton extends GateButton {
   private final MultiStateHardwareLight light;

   public RgbButton(final int midiId, String name, HardwareSurface surface, MidiProcessor midiProcessor) {
      super(midiId, midiProcessor);
      MidiIn midiIn = midiProcessor.getMidiIn();
      hwButton = surface.createHardwareButton(name + "_" + midiId);
      hwButton.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(0, midiId));
      hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, midiId));
      
      hwButton.isPressed().markInterested();
      light = surface.createMultiStateHardwareLight(name + "_LIGHT_" + midiId);
      light.state().setValue(RgbLightState.OFF);
      hwButton.isPressed().markInterested();
      light.state().onUpdateHardware(this::updateState);
   }

   private void updateState(InternalHardwareLightState state) {
      if (state instanceof RgbColor rgbState) {
         midiProcessor.updateColorPad(midiId, rgbState);
      }
   }

   public void bindLight(Layer layer, final Supplier<InternalHardwareLightState> supplier) {
      layer.bindLightState(supplier, this.light);
   }


}
