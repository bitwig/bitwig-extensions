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
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMk4ControllerExtension;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkViewControl;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkRgbButton;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkColor;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class DrumPadLayer extends Layer {
    
    private MpkColor trackColor;
    private final PadState[] padStates = new PadState[16];
    private final NoteInput noteInput;
    private boolean hasDrumPads;
    private final DrumPadBank drumPadBank;
    protected final int[] noteToPad = new int[128];
    protected final int[] padToNote = new int[16];
    protected final Integer[] deactivationTable = new Integer[128];
    private final Integer[] noteTable = new Integer[128];
    private final Integer[] velocityTable = new Integer[128];
    private int padOffset = 36;
    
    private class PadState {
        private boolean isSelected;
        private boolean isBaseNote;
        private boolean playing;
        private MpkColor color = MpkColor.WHITE;
    }
    
    public DrumPadLayer(final Layers layers, final MpkHwElements hwElements, final LayerCollection layerCollection,
        final MpkViewControl viewControl, final MpkMidiProcessor midiProcessor) {
        super(layers, "DRUM_PADS");
        final List<MpkRgbButton> gridButtons = hwElements.getGridButtons();
        for (int i = 0; i < 16; i++) {
            padStates[i] = new PadState();
        }
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
        drumPadBank = viewControl.getPrimaryDevice().createDrumPadBank(16);
        viewControl.getPrimaryDevice().hasDrumPads().addValueObserver(this::handleHasDrumPadsChanged);
        drumPadBank.setIndication(true);
        drumPadBank.scrollPosition().addValueObserver(this::handlePadBankScrolling);
        
        for (int i = 0; i < 4; i++) {
            final int colIndex = i;
            for (int j = 0; j < 4; j++) {
                final int rowIndex = j;
                final int padIndex = rowIndex * 4 + colIndex;
                final DrumPad pad = drumPadBank.getItemAt(padIndex);
                final MpkRgbButton button = gridButtons.get(padIndex);
                setUpPad(padIndex, pad);
                button.bindLight(this, () -> getState(colIndex, rowIndex));
            }
        }
    }
    
    public void init() {
        Arrays.fill(noteTable, -1);
        for (final PadState pad : padStates) {
            pad.playing = false;
        }
        noteInput.setKeyTranslationTable(noteTable);
    }
    
    private void setUpPad(final int index, final DrumPad pad) {
        final PadState padState = padStates[index];
        pad.color().addValueObserver((r, g, b) -> padState.color = MpkColor.getColor(r, g, b));
        pad.name().markInterested();
        pad.exists().markInterested();
        pad.solo().markInterested();
        pad.mute().markInterested();
        pad.addIsSelectedInEditorObserver(selected -> {
            padState.isSelected = selected;
        });
    }
    
    private void handleHasDrumPadsChanged(final boolean hasDrumPads) {
        this.hasDrumPads = hasDrumPads;
        if (isActive()) {
            applyMode();
        } else {
            applyScale();
        }
    }
    
    private void handlePadBankScrolling(final int scrollPos) {
        padOffset = scrollPos;
        MpkMk4ControllerExtension.println(" VSH =>> %d", scrollPos);
        //selectPad(getSelectedIndex());
        if (isActive()) {
            applyScale();
        }
    }
    
    private void setTrackColor(final MpkColor color) {
        this.trackColor = color;
    }
    
    private InternalHardwareLightState getState(final int colIndex, final int rowIndex) {
        final int index = rowIndex * 4 + colIndex;
        if (hasDrumPads) {
            return padStates[index].playing
                ? padStates[index].color
                : padStates[index].color.variant(MpkColor.SOLID_10);
        }
        return padStates[index].playing ? trackColor : trackColor.variant(MpkColor.SOLID_10);
    }
    
    
    private void handleNotePlaying(final PlayingNote[] notes) {
        if (isActive()) {
            for (int i = 0; i < 16; i++) {
                padStates[i].playing = false;
            }
            for (final PlayingNote playingNote : notes) {
                final int padIndex = noteToPad[playingNote.pitch()];
                if (padIndex != -1) {
                    padStates[padIndex].playing = true;
                }
            }
        }
    }
    
    
    private void applyMode() {
        noteInput.setKeyTranslationTable(noteTable);
    }
    
    private void applyScale() {
        for (int i = 0; i < 16; i++) {
            noteTable[i + 0x24] = padOffset + i;
            noteToPad[padOffset + i] = i;
            padToNote[i] = padOffset + i;
        }
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
        applyScale();
        applyMode();
    }
    
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        Arrays.fill(noteTable, -1);
        for (final PadState pad : padStates) {
            pad.playing = false;
        }
        noteInput.setKeyTranslationTable(noteTable);
        this.noteInput.setShouldConsumeEvents(false);
    }
    
}
