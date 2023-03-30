package com.bitwig.extensions.controllers.akai.apcmk2.layer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.akai.apcmk2.DebugApc;
import com.bitwig.extensions.controllers.akai.apcmk2.MidiProcessor;
import com.bitwig.extensions.controllers.akai.apcmk2.ViewControl;
import com.bitwig.extensions.controllers.akai.apcmk2.control.HardwareElementsApc;
import com.bitwig.extensions.controllers.akai.apcmk2.control.RgbButton;
import com.bitwig.extensions.controllers.akai.apcmk2.led.RgbLightState;
import com.bitwig.extensions.controllers.novation.commonsmk3.ColorLookup;
import com.bitwig.extensions.controllers.novation.commonsmk3.SpecialDevices;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.di.PostConstruct;

import java.util.Arrays;

public class DrumPadLayer extends Layer {

    @Inject
    private ViewControl viewCursorControl;

    private DrumPadBank drumPadBank;
    private NoteInput noteInput;
    private final int[] padColors = new int[64];
    private final Integer[] noteTable = new Integer[128];
    private final boolean[] isPlaying = new boolean[128];
    private int padsNoteOffset;
    private int currentTrackColor;

    public DrumPadLayer(final Layers layers) {
        super(layers, "DRUM_PAD_LAYER");
    }

    @PostConstruct
    public void init(final ControllerHost host, final MidiProcessor midiProcessor,
                     final HardwareElementsApc hwElements) {

        noteInput = midiProcessor.getMidiIn().createNoteInput("MIDI", "88????", "98????");
        noteInput.setShouldConsumeEvents(false);
        DebugApc.println(" INIT >> ");
        final CursorTrack cursorTrack = viewCursorControl.getCursorTrack();
        cursorTrack.color()
           .addValueObserver(
              (r, g, b) -> currentTrackColor = com.bitwig.extensions.controllers.akai.apcmk2.led.ColorLookup.toColor(r,
                 g, b));
        cursorTrack.playingNotes().addValueObserver(this::handleNotes);

        final PinnableCursorDevice primaryDevice = viewCursorControl.getPrimaryDevice();
        final DeviceBank drumBank = cursorTrack.createDeviceBank(1);
        final DeviceMatcher drumMatcher = host.createBitwigDeviceMatcher(SpecialDevices.DRUM.getUuid());
        drumBank.setDeviceMatcher(drumMatcher);
        drumPadBank = primaryDevice.createDrumPadBank(64);
        drumPadBank.scrollPosition().addValueObserver(index -> {
            padsNoteOffset = index;
            if (isActive()) {
                applyNotes(padsNoteOffset);
            }
        });

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                final RgbButton button = hwElements.getGridButton(row, col);
                final int index = (7 - row) * 8 + col;
                final DrumPad pad = drumPadBank.getItemAt(index);
                pad.exists().markInterested();
                pad.color().addValueObserver((r, g, b) -> padColors[index] = ColorLookup.toColor(r, g, b));
                button.bindLight(this, () -> getPadState(index, pad));
            }
        }

    }

    public void applyNotes(final int noteOffset) {
        Arrays.fill(noteTable, -1);
        for (int note = 0; note < 64; note++) {
            final int value = noteOffset + note;
            noteTable[36 + note] = value < 128 ? value : -1;
        }
        noteInput.setKeyTranslationTable(noteTable);
    }

    private void handleNotes(final PlayingNote[] playingNotes) {
        if (!isActive()) {
            return;
        }
        Arrays.fill(isPlaying, false);
        for (final PlayingNote playingNote : playingNotes) {
            isPlaying[playingNote.pitch()] = true;
        }
    }

    private RgbLightState getPadState(final int index, final DrumPad pad) {
        final boolean playing = isPlaying(index);
        if (pad.exists().get()) {
            if (playing) {
                return RgbLightState.WHITE;
            }
            if (padColors[index] == 0) {
                return RgbLightState.of(currentTrackColor);
            }
            return RgbLightState.of(padColors[index]);
        }
        return playing ? RgbLightState.WHITE_DIM : RgbLightState.OFF;
    }

    public boolean isPlaying(final int index) {
        final int offset = padsNoteOffset + index;
        if (offset < 128) {
            return isPlaying[offset];
        }
        return false;
    }

    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        Arrays.fill(noteTable, -1);
        noteInput.setKeyTranslationTable(noteTable);
    }

    @Override
    protected void onActivate() {
        super.onActivate();
        applyNotes(padsNoteOffset);
    }
}
