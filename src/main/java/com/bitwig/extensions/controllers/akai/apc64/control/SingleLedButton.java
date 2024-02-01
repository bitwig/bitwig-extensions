package com.bitwig.extensions.controllers.akai.apc64.control;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extensions.controllers.akai.apc.common.MidiProcessor;
import com.bitwig.extensions.controllers.akai.apc.common.control.ApcButton;
import com.bitwig.extensions.controllers.akai.apc.common.led.RgbLightState;
import com.bitwig.extensions.controllers.akai.apc.common.led.VarSingleLedState;
import com.bitwig.extensions.framework.values.Midi;

public class SingleLedButton extends ApcButton {

    public SingleLedButton(final int noteNr, final String name, final HardwareSurface surface,
                           final MidiProcessor midiProcessor) {
        super(0, noteNr, name, surface, midiProcessor);
        light.state().setValue(RgbLightState.OFF);
        light.state().onUpdateHardware(this::updateState);
        light.setColorToStateFunction(this::colorToState);
    }

    private InternalHardwareLightState colorToState(final Color color) {
        if (color.getRed255() == 0 && color.getBlue255() == 0 && color.getGreen255() == 0) {
            return VarSingleLedState.OFF;
        }
        return VarSingleLedState.FULL;
    }

    private void updateState(final InternalHardwareLightState internalHardwareLightState) {
        if (internalHardwareLightState instanceof VarSingleLedState state) {
            midiProcessor.sendMidi(Midi.NOTE_ON | state.getChannel(), midiId, state.getCode());
        } else {
            midiProcessor.sendMidi(Midi.NOTE_ON, midiId, 0);
        }
    }
}
