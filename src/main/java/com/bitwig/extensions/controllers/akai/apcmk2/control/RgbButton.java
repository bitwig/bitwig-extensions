package com.bitwig.extensions.controllers.akai.apcmk2.control;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extensions.controllers.akai.apcmk2.MidiProcessor;
import com.bitwig.extensions.controllers.akai.apcmk2.led.RgbLightState;
import com.bitwig.extensions.framework.values.Midi;

public class RgbButton extends ApcButton {

   protected RgbButton(final int channel, final int noteNr, String name, final HardwareSurface surface,
                       final MidiProcessor midiProcessor) {
      super(channel, noteNr, name, surface, midiProcessor);
      light.state().setValue(RgbLightState.OFF);
      if (channel == 9) {
         light.state().onUpdateHardware(this::updateDrumState);
      } else {
         light.state().onUpdateHardware(this::updateState);
      }
   }

   private void updateDrumState(final InternalHardwareLightState internalHardwareLightState) {
      if (internalHardwareLightState instanceof RgbLightState) {
         RgbLightState state = (RgbLightState) internalHardwareLightState;
         midiProcessor.sendMidi(Midi.NOTE_ON | 0x9, midiId, state.getColorIndex());
      } else {
         midiProcessor.sendMidi(Midi.NOTE_ON, midiId, 0);
      }
   }


   private void updateState(final InternalHardwareLightState internalHardwareLightState) {
      if (internalHardwareLightState instanceof RgbLightState) {
         RgbLightState state = (RgbLightState) internalHardwareLightState;
         midiProcessor.sendMidi(state.getMidiCode(), midiId, state.getColorIndex());
      } else {
         midiProcessor.sendMidi(Midi.NOTE_ON, midiId, 0);
      }
   }

   public void reset() {
      updateState(light.state().currentValue());
   }
}
