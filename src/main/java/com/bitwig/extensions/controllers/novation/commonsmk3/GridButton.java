package com.bitwig.extensions.controllers.novation.commonsmk3;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extensions.framework.values.Midi;


public class GridButton extends LaunchPadButton {
    private final int notevalue;

    public GridButton(final HardwareSurface surface, final MidiProcessor midiProcessor, final int row, final int col) {
        super("grid_" + row + "_" + col, surface, midiProcessor, 0);
        notevalue = 10 * (8 - row) + col + 1;
        initButtonNote(midiProcessor.getMidiIn(), notevalue);
        light.state().setValue(RgbState.of(0));
        light.state().onUpdateHardware(this::updateState);
    }

    private void updateState(final InternalHardwareLightState state) {
        if (state instanceof RgbState) {
            final RgbState rgbState = (RgbState) state;
            switch (rgbState.getState()) {
                case NORMAL:
                    midiProcessor.sendMidi(Midi.NOTE_ON, notevalue, rgbState.getColorIndex());
                    break;
                case FLASHING:
                    midiProcessor.sendMidi(Midi.NOTE_ON, notevalue, rgbState.getAltColor());
                    midiProcessor.sendMidi(Midi.NOTE_ON + 1, notevalue, rgbState.getColorIndex());
                    break;
                case PULSING:
                    midiProcessor.sendMidi(Midi.NOTE_ON + 2, notevalue, rgbState.getColorIndex());
                    break;
            }
        } else {
            midiProcessor.sendMidi(Midi.NOTE_ON, notevalue, 0);
        }
    }

    @Override
    public void refresh() {
        updateState(light.state().currentValue());
    }


}
