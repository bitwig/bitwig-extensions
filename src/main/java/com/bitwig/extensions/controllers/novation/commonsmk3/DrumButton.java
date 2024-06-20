package com.bitwig.extensions.controllers.novation.commonsmk3;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extensions.framework.values.Midi;


public class DrumButton extends LaunchPadButton {
    private final int notevalue;
    private final int midiState;

    public DrumButton(final HardwareSurface surface, final MidiProcessor midiProcessor, final int channel,
                      final int noteValue) {
        super("grid_drum" + channel + "_" + noteValue, surface, midiProcessor, channel);

        notevalue = noteValue;
        midiState = Midi.NOTE_ON | channel;
        initButtonNote(midiProcessor.getMidiIn(), notevalue);
        light.state().setValue(RgbState.of(0));
        light.state().onUpdateHardware(this::updateState);
    }

    private void updateState(final InternalHardwareLightState state) {
        if (state instanceof RgbState) {
            final RgbState rgbState = (RgbState) state;
            midiProcessor.sendMidi(midiState, notevalue, rgbState.getColorIndex());
            switch (rgbState.getState()) {
                case NORMAL:
                    midiProcessor.sendMidi(midiState, notevalue, rgbState.getColorIndex());
                    break;
                case FLASHING:
                    midiProcessor.sendMidi(midiState, notevalue, rgbState.getAltColor());
                    midiProcessor.sendMidi(midiState + 1, notevalue, rgbState.getColorIndex());
                    break;
                case PULSING:
                    midiProcessor.sendMidi(midiState + 2, notevalue, rgbState.getColorIndex());
                    break;
            }
        } else {
            midiProcessor.sendMidi(midiState, notevalue, 0);
        }
    }

    @Override
    public void refresh() {
        updateState(light.state().currentValue());
    }
}
