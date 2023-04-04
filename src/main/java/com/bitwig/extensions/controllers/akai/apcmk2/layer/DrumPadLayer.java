package com.bitwig.extensions.controllers.akai.apcmk2.layer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.akai.apcmk2.DebugApc;
import com.bitwig.extensions.controllers.akai.apcmk2.ViewControl;
import com.bitwig.extensions.controllers.akai.apcmk2.control.HardwareElementsApc;
import com.bitwig.extensions.controllers.akai.apcmk2.control.RgbButton;
import com.bitwig.extensions.controllers.akai.apcmk2.led.RgbLightState;
import com.bitwig.extensions.controllers.akai.apcmk2.midi.MidiProcessor;
import com.bitwig.extensions.controllers.novation.commonsmk3.ColorLookup;
import com.bitwig.extensions.controllers.novation.commonsmk3.SpecialDevices;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.di.PostConstruct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DrumPadLayer extends Layer {

    @Inject
    private ViewControl viewCursorControl;

    private DrumPadBank drumPadBank;
    private NoteInput noteInput;
    private final int[] padColors = new int[64];
    private final Integer[] noteTable = new Integer[128];
    private final boolean[] isPlaying = new boolean[128];
    private int padsNoteOffset;
    private int plainNoteOffset = 24;
    private int currentTrackColor;
    private boolean focusOnDrumDevice = false;
    private List<RgbButton> buttons = new ArrayList<>();

    public DrumPadLayer(final Layers layers) {
        super(layers, "DRUM_PAD_LAYER");
    }

    @PostConstruct
    public void init(final ControllerHost host, final MidiProcessor midiProcessor,
                     final HardwareElementsApc hwElements) {

        noteInput = midiProcessor.createNoteInput("MIDI", "89????", "99????");
        noteInput.setShouldConsumeEvents(true);
        final CursorTrack cursorTrack = viewCursorControl.getCursorTrack();
        cursorTrack.color()
                .addValueObserver(
                        (r, g, b) -> currentTrackColor = com.bitwig.extensions.controllers.akai.apcmk2.led.ColorLookup.toColor(
                                r, g, b));
        cursorTrack.playingNotes().addValueObserver(this::handleNotes);

        final PinnableCursorDevice primaryDevice = viewCursorControl.getPrimaryDevice();
        final DeviceBank drumBank = cursorTrack.createDeviceBank(1);
        final DeviceMatcher drumMatcher = host.createBitwigDeviceMatcher(SpecialDevices.DRUM.getUuid());
        drumBank.setDeviceMatcher(drumMatcher);
        Device drumDevice = drumBank.getItemAt(0);
        drumDevice.exists().addValueObserver(this::focusOnDrumDevice);
        drumPadBank = primaryDevice.createDrumPadBank(64);
        drumPadBank.scrollPosition().addValueObserver(index -> {
            padsNoteOffset = index;
            DebugApc.println(" PAD Offset = %d", padsNoteOffset);
            if (isActive()) {
                applyNotes(padsNoteOffset);
            }
        });

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                final RgbButton button = hwElements.getDrumButton(row, col);
                buttons.add(button);
                final int index = (7 - row) * 4 + col / 4 * 32 + col % 4;
                final DrumPad pad = drumPadBank.getItemAt(index);
                pad.exists().markInterested();
                pad.color().addValueObserver((r, g, b) -> padColors[index] = ColorLookup.toColor(r, g, b));
                button.bindLight(this, () -> getPadState(index, pad));
            }
        }
    }

    public void focusOnDrumDevice(boolean focusOnDrumDevice) {
        this.focusOnDrumDevice = focusOnDrumDevice;
        applyNotes(focusOnDrumDevice ? padsNoteOffset : plainNoteOffset);
    }

    public void refreshButtons() {
        for (RgbButton button : buttons) {
            button.refresh();
        }
    }

    public void applyNotes(final int noteOffset) {
        Arrays.fill(noteTable, -1);
        for (int note = 0; note < 64; note++) {
            int tableNote = note / 8 * 4 + note % 4 + (note % 8) / 4 * 32;
            final int value = noteOffset + tableNote;
            noteTable[0x40 + note] = value < 128 ? value : -1;
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
                return RgbLightState.GREEN;
            }
            if (padColors[index] == 0) {
                return RgbLightState.of(currentTrackColor);
            }
            return RgbLightState.of(padColors[index]);
        }
        return playing ? RgbLightState.GREEN : RgbLightState.of(2);
    }

    public boolean isPlaying(final int index) {
        final int offset = (focusOnDrumDevice ? padsNoteOffset : plainNoteOffset) + index;
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
        applyNotes(focusOnDrumDevice ? padsNoteOffset : plainNoteOffset);
    }


}
