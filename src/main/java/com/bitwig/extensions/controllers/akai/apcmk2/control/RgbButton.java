package com.bitwig.extensions.controllers.akai.apcmk2.control;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extensions.controllers.akai.apcmk2.MidiProcessor;
import com.bitwig.extensions.controllers.akai.apcmk2.led.RgbLightState;
import com.bitwig.extensions.framework.values.Midi;

public class RgbButton extends ApcButton {
    
    protected RgbButton(final int noteNr, final HardwareSurface surface, final MidiProcessor midiProcessor) {
        super(noteNr, surface, midiProcessor);
        light.state().setValue(RgbLightState.OFF);
        light.state().onUpdateHardware(this::updateState);
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
