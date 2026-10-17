package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import java.util.Arrays;
import java.util.List;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkHwElements;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkVariant;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkViewControl;
import com.bitwig.extensions.controllers.akai.mpkmk4.ScaleSetup;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkMultiStateButton;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkColor;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkMonoState;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.IntValueObject;

public class DrumPadLayer extends Layer {
    
    private static final int REVERSE_LOOKUP[] = {
        8, 9, 10, 11, //
        12, 13, 14, 15, //
        0, 1, 2, 3, //
        4, 5, 6, 7, //
    };
    private final boolean reversal;
    
    public static final int PAD_COUNT = 16;
    private MpkColor trackColor;
    private final ScaleSetup scaleSetup = new ScaleSetup();
    private final NoteInput noteInput;
    private boolean hasDrumPads;
    private final DrumPadBank focusPadBank;
    protected final int[] noteToPad = new int[128];
    protected final int[] padToNote = new int[PAD_COUNT];
    protected boolean[] isBaseNote = new boolean[PAD_COUNT];
    protected final Integer[] deactivationTable = new Integer[128];
    private final Integer[] noteTable = new Integer[128];
    private final Integer[] velocityTable = new Integer[128];
    private final IntValueObject padOffset = new IntValueObject(36, 0, 120);
    private final PadSlot[] padSlots = new PadSlot[PAD_COUNT];
    
    public DrumPadLayer(final Layers layers, final MpkHwElements hwElements, final MpkViewControl viewControl,
        final MpkMidiProcessor midiProcessor, final MpkVariant variant) {
        super(layers, "DRUM_PADS");
        final List<MpkMultiStateButton> gridButtons = hwElements.getGridButtons();
        this.noteInput = midiProcessor.getNoteInput();
        Arrays.fill(deactivationTable, -1);
        Arrays.fill(noteTable, -1);
        Arrays.fill(noteToPad, -1);
        Arrays.fill(padToNote, -1);
        for (int i = 0; i < 128; i++) {
            velocityTable[i] = i;
        }
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        cursorTrack.playingNotes().addValueObserver(this::handleNotePlaying);
        
        cursorTrack.color().addValueObserver((r, g, b) -> setTrackColor(MpkColor.getColor(r, g, b)));
        final DrumPadBank drumPadBank = viewControl.getPadBank();
        viewControl.getPrimaryDevice().hasDrumPads().addValueObserver(this::handleHasDrumPadsChanged);
        drumPadBank.setIndication(true);
        drumPadBank.scrollPosition().addValueObserver(this::handlePadBankScrolling);
        midiProcessor.addNoteListener(this::handlePlayingNote);
        focusPadBank = viewControl.getFocusDrumPad();
        reversal = !variant.isPadReverse();
        
        for (int i = 0; i < 4; i++) {
            final int colIndex = i;
            for (int j = 0; j < 4; j++) {
                final int rowIndex = j;
                
                final int padIndex = rowIndex * 4 + colIndex;
                final DrumPad pad = drumPadBank.getItemAt(padIndex);
                final MpkMultiStateButton button = gridButtons.get(REVERSE_LOOKUP[padIndex]);
                setUpPad(padIndex, pad);
                button.bindLight(this, () -> getState(colIndex, rowIndex));
            }
        }
        scaleSetup.addChangeListener(this::handleScaleChanged);
        padOffset.addValueObserver(v -> drumPadBank.scrollPosition().set(v));
    }
    
    private void handleScaleChanged() {
        if (isActive()) {
            applyScale();
        }
    }
    
    public ScaleSetup getScaleSetup() {
        return scaleSetup;
    }
    
    public IntValueObject getPadOffset() {
        return padOffset;
    }
    
    private void handlePlayingNote(final int note, final int value) {
        if (isActive()) {
            final int padIndex = noteToPad[note];
            if (value > 0 && padIndex >= 0 && padIndex <= PAD_COUNT) {
                if (hasDrumPads) {
                    final PadSlot padSlot = padSlots[padIndex];
                    //padSlot.getPad().selectInMixer();
                    padSlot.getDeviceItem().selectInEditor();
                    //TODO Make this optional ??
                }
            }
        }
    }
    
    public void init() {
        Arrays.fill(noteTable, -1);
        for (final PadSlot pad : padSlots) {
            pad.setPlaying(false);
        }
        noteInput.setKeyTranslationTable(noteTable);
    }
    
    private void setUpPad(final int index, final DrumPad pad) {
        final PadSlot padSlot = new PadSlot(index, pad);
        padSlots[index] = padSlot;
        pad.name().markInterested();
        pad.exists().markInterested();
        pad.solo().markInterested();
        pad.mute().markInterested();
        pad.addIsSelectedInEditorObserver(selected -> padSlot.setSelected(selected));
    }
    
    private void handleHasDrumPadsChanged(final boolean hasDrumPads) {
        this.hasDrumPads = hasDrumPads;
        if (isActive()) {
            applyScale();
        }
    }
    
    private void handlePadBankScrolling(final int scrollPos) {
        if (isActive()) {
            applyScale();
        }
    }
    
    private void setTrackColor(final MpkColor color) {
        this.trackColor = color;
    }
    
    private InternalHardwareLightState getState(final int colIndex, final int rowIndex) {
        final int index = rowIndex * 4 + colIndex;
        final PadSlot pad = padSlots[index];
        if (hasDrumPads) {
            return pad.isPlaying() ? pad.getColor() : pad.getColor().variant(MpkMonoState.SOLID_10);
        }
        if (isBaseNote[index]) {
            return pad.isPlaying() ? trackColor : trackColor.variant(MpkMonoState.SOLID_50);
        }
        return pad.isPlaying() ? trackColor : trackColor.variant(MpkMonoState.SOLID_10);
    }
    
    
    private void handleNotePlaying(final PlayingNote[] notes) {
        if (isActive()) {
            for (int i = 0; i < PAD_COUNT; i++) {
                padSlots[i].setPlaying(false);
            }
            for (final PlayingNote playingNote : notes) {
                final int padIndex = noteToPad[playingNote.pitch()];
                if (padIndex != -1) {
                    padSlots[padIndex].setPlaying(true);
                }
            }
        }
    }
    
    
    private void applyScale() {
        final int padOff = padOffset.get();
        Arrays.fill(noteToPad, -1);
        Arrays.fill(padToNote, -1);
        
        if (hasDrumPads) {
            for (int i = 0; i < PAD_COUNT; i++) {
                int offset = reversal ? REVERSE_LOOKUP[i] : i;
                final int note = padOff + i;
                noteTable[offset + 0x24] = note < 128 ? note : -1;
                if (note < 128) {
                    noteToPad[note] = i;
                    padToNote[i] = note;
                }
            }
        } else {
            final List<Integer> noteSequence = scaleSetup.getNoteSequence(PAD_COUNT);
            scaleSetup.adjustToRange(PAD_COUNT);
            isBaseNote = getScaleSetup().getBaseNotes(PAD_COUNT);
            for (int i = 0; i < PAD_COUNT; i++) {
                final int note = noteSequence.get(i);
                //int offset = reversePads ? (1 - (i / 8)) * 8 + (i % 8) : i;
                //int offset = reversePads ? offset_lookup[i] : i;
                int offset = reversal ? REVERSE_LOOKUP[i] : i;
                noteTable[offset + 0x24] = noteSequence.get(i);
                padToNote[i] = note;
                if (note != -1) {
                    noteToPad[note] = i;
                }
            }
        }
        noteInput.setKeyTranslationTable(noteTable);
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
        applyScale();
    }
    
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        Arrays.fill(noteTable, -1);
        for (int i = 0; i < PAD_COUNT; i++) {
            padSlots[i].setPlaying(false);
        }
        noteInput.setKeyTranslationTable(noteTable);
        this.noteInput.setShouldConsumeEvents(false);
    }
    
}
