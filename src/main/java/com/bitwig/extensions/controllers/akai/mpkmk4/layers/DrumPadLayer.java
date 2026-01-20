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
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkViewControl;
import com.bitwig.extensions.controllers.akai.mpkmk4.ScaleSetup;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkMultiStateButton;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkColor;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkMonoState;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.IntValueObject;

public class DrumPadLayer extends Layer {
    
    private MpkColor trackColor;
    private final ScaleSetup scaleSetup = new ScaleSetup();
    private final NoteInput noteInput;
    private boolean hasDrumPads;
    private final DrumPadBank focusPadBank;
    protected final int[] noteToPad = new int[128];
    protected final int[] padToNote = new int[16];
    protected boolean[] isBaseNote = new boolean[16];
    protected final Integer[] deactivationTable = new Integer[128];
    private final Integer[] noteTable = new Integer[128];
    private final Integer[] velocityTable = new Integer[128];
    private final IntValueObject padOffset = new IntValueObject(36, 0, 120);
    private final PadSlot[] padSlots = new PadSlot[16];
    
    public DrumPadLayer(final Layers layers, final MpkHwElements hwElements, final LayerCollection layerCollection,
        final MpkViewControl viewControl, final MpkMidiProcessor midiProcessor) {
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
        
        for (int i = 0; i < 4; i++) {
            final int colIndex = i;
            for (int j = 0; j < 4; j++) {
                final int rowIndex = j;
                final int padIndex = rowIndex * 4 + colIndex;
                final DrumPad pad = drumPadBank.getItemAt(padIndex);
                final MpkMultiStateButton button = gridButtons.get(padIndex);
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
            if (value > 0 && padIndex >= 0 && padIndex <= 16) {
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
            for (int i = 0; i < 16; i++) {
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
            for (int i = 0; i < 16; i++) {
                noteTable[i + 0x24] = padOff + i;
                noteToPad[padOff + i] = i;
                padToNote[i] = padOff + i;
            }
        } else {
            final List<Integer> noteSequence = scaleSetup.getNoteSequence(16);
            isBaseNote = getScaleSetup().getBaseNotes(16);
            for (int i = 0; i < 16; i++) {
                final int note = noteSequence.get(i);
                noteTable[i + 0x24] = noteSequence.get(i);
                noteToPad[note] = i;
                padToNote[i] = note;
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
        for (int i = 0; i < 16; i++) {
            padSlots[i].setPlaying(false);
        }
        noteInput.setKeyTranslationTable(noteTable);
        this.noteInput.setShouldConsumeEvents(false);
    }
    
}
