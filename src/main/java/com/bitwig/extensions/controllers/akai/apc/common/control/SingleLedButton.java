package com.bitwig.extensions.controllers.akai.apc.common.control;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extensions.controllers.akai.apc.common.led.RgbLightState;
import com.bitwig.extensions.controllers.akai.apc.common.led.SingleLedState;
import com.bitwig.extensions.controllers.akai.apc.common.MidiProcessor;
import com.bitwig.extensions.framework.values.Midi;

public class SingleLedButton extends ApcButton {

    public SingleLedButton(final int noteNr, String name, final HardwareSurface surface,
                           final MidiProcessor midiProcessor) {
        super(0, noteNr, name, surface, midiProcessor);
        light.state().setValue(RgbLightState.OFF);
        light.state().onUpdateHardware(this::updateState);
    }

    private void updateState(final InternalHardwareLightState internalHardwareLightState) {
        if (internalHardwareLightState instanceof SingleLedState) {
            final SingleLedState state = (SingleLedState) internalHardwareLightState;
            midiProcessor.sendMidi(Midi.NOTE_ON, midiId, state.getCode());
        } else {
            midiProcessor.sendMidi(Midi.NOTE_ON, midiId, 0);
        }
    }

}
