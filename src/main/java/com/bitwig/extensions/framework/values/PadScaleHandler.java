package com.bitwig.extensions.framework.values;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.SettableEnumValue;

public class PadScaleHandler {
    
    private final List<IScale> scales;
    private final List<String> baseNotes = List.of("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B");
    private final int padCount;
    private final SettableEnumValue baseNotesAssignment;
    
    private int currentScale = 0;
    private int baseNote = 0;
    private int noteOffset = 48;
    
    private final SettableEnumValue scaleAssignment;
    private final List<Runnable> stateChangedListener = new ArrayList<>();
    
    public PadScaleHandler(final ControllerHost host, final List<IScale> includedScales, final int padCount,
        final boolean scaleToDocumentState) {
        this.scales = includedScales;
        this.padCount = padCount;
        final DocumentState documentState = host.getDocumentState();
        
        if (scaleToDocumentState) {
            scaleAssignment = documentState.getEnumSetting("Pad Scale", //
                "Pads", scales.stream().map(IScale::getName).toArray(String[]::new), scales.get(0).getName());
            scaleAssignment.addValueObserver(this::handleScaleChanged);
            
            baseNotesAssignment = documentState.getEnumSetting("Base Note", //
                "Pads", baseNotes.stream().toArray(String[]::new), baseNotes.get(0));
            baseNotesAssignment.addValueObserver(this::handleBaseNoteChanged);
        } else {
            scaleAssignment = null;
            baseNotesAssignment = null;
        }
    }
    
    public void addStateChangedListener(final Runnable listener) {
        this.stateChangedListener.add(listener);
    }
    
    public int getBaseNote() {
        return baseNote;
    }
    
    public IScale getCurrentScale() {
        return scales.get(currentScale);
    }
    
    private void handleBaseNoteChanged(final String newNote) {
        final int index = baseNotes.indexOf(newNote);
        if (index != -1) {
            baseNote = index;
            stateChangedListener.forEach(Runnable::run);
        }
    }
    
    private void handleScaleChanged(final String newScale) {
        scales.stream().filter(scale -> scale.getName().equals(newScale)).map(scale -> scales.indexOf(scale))
            .findFirst().ifPresent(newIndex -> {
                currentScale = newIndex;
                stateChangedListener.forEach(Runnable::run);
            });
    }
    
    public String getBaseNoteStr() {
        return baseNotes.get(baseNote % 12);
    }
    
    public String getOctaveOffset() {
        final String note = baseNotes.get((noteOffset + baseNote) % 12);
        final int oct = (noteOffset + baseNote) / 12;
        return "%s %d".formatted(note, oct - 2);
    }
    
    public int matchScale(final int inNote, final int dir) {
        final IScale scale = scales.get(currentScale);
        final int scaleIndex = (inNote % 12 + 12 - baseNote) % 12;
        if (scale.inScale(scaleIndex)) {
            return inNote;
        }
        final int nextInScale = scale.nextInScale(scaleIndex);
        final int diff = nextInScale - scaleIndex;
        return inNote + diff * dir;
    }
    
    public boolean inScale(final int inNote) {
        final IScale scale = scales.get(currentScale);
        final int scaleIndex = (inNote % 12 + 12 - baseNote) % 12;
        return scale.inScale(scaleIndex);
    }
    
    public void incScaleSelection(final int dir) {
        currentScale += dir;
        if (currentScale >= scales.size()) {
            currentScale = 0;
        } else if (currentScale < 0) {
            currentScale = scales.size() - 1;
        }
        if (scaleAssignment != null) {
            scaleAssignment.set(scales.get(currentScale).getName());
        }
        stateChangedListener.forEach(Runnable::run);
    }
    
    public void incrementNoteOffset(final int dir) {
        final IScale activeScale = scales.get(currentScale);
        final int[] intervals = activeScale.getIntervals();
        final int scaleSize = intervals.length;
        final int amount = scaleSize == 12 ? 4 : 12;
        final int lastNoteOffset = padCount / scaleSize * 12 - scaleSize;
        final int newValue = noteOffset + amount * dir;
        if (newValue >= 0 && (newValue + lastNoteOffset) < 128) {
            noteOffset = newValue;
            stateChangedListener.forEach(Runnable::run);
        }
    }
    
    public int getStartNote() {
        if (getCurrentScale().getIntervals().length == 12) {
            return noteOffset + baseNote;
        }
        final int octave = noteOffset / 12;
        return octave * 12 + baseNote;
    }
    
    public void incBaseNote(final int dir) {
        final int newBaseNote = baseNote + dir;
        if (newBaseNote >= 0 && newBaseNote < 12) {
            baseNote = newBaseNote;
            if (baseNotesAssignment != null) {
                baseNotesAssignment.set(baseNotes.get(baseNote));
            }
        }
    }
    
    public boolean isBaseNote(final int note) {
        return (note + 12 - baseNote) % 12 == 0;
    }
    
    public boolean inScale(final Integer note) {
        final int noteBase = (note - baseNote + 120) % 12;
        return getCurrentScale().inScale(noteBase);
    }
}
