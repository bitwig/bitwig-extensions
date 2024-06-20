package com.bitwig.extensions.controllers.akai.apc.common.control;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extensions.controllers.akai.apc.common.MidiProcessor;
import com.bitwig.extensions.controllers.akai.apc.common.led.RgbLightState;
import com.bitwig.extensions.controllers.novation.commonsmk3.ColorLookup;
import com.bitwig.extensions.framework.values.Midi;

public class RgbButton extends ApcButton {

    public RgbButton(final int channel, final int noteNr, final String name, final HardwareSurface surface,
                     final MidiProcessor midiProcessor) {
        super(channel, noteNr, name, surface, midiProcessor);
        light.state().setValue(RgbLightState.OFF);
        light.setColorToStateFunction(this::colorToState);
        if (channel == 9) {
            light.state().onUpdateHardware(this::updateDrumState);
        } else {
            light.state().onUpdateHardware(this::updateState);
        }
    }

    private InternalHardwareLightState colorToState(final Color color) {
        return RgbLightState.of(ColorLookup.toColor(color.getRed255(), color.getGreen255(), color.getBlue255()));
    }

    private void updateDrumState(final InternalHardwareLightState internalHardwareLightState) {
        if (internalHardwareLightState instanceof RgbLightState state) {
            midiProcessor.sendMidi(Midi.NOTE_ON | 0x9, midiId, state.getColorIndex());
        } else {
            midiProcessor.sendMidi(Midi.NOTE_ON, midiId, 0);
        }
    }


    private void updateState(final InternalHardwareLightState internalHardwareLightState) {
        if (internalHardwareLightState instanceof RgbLightState state) {
            midiProcessor.sendMidi(state.getMidiCode(), midiId, state.getColorIndex());
        } else {
            midiProcessor.sendMidi(Midi.NOTE_ON, midiId, 0);
        }
    }

    @Override
    public void refresh() {
        updateState(light.state().currentValue());
    }
}
