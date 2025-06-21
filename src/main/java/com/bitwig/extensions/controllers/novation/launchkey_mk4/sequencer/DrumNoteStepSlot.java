package com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer;

import java.util.Collection;
import java.util.List;

import com.bitwig.extension.controller.api.NoteStep;

public class DrumNoteStepSlot implements INoteStepSlot {
    
    private NoteStep step;
    private NoteStep copyNote;
    
    @Override
    public void updateNote(final NoteStep step) {
        this.step = step;
    }
    
    @Override
    public void clear() {
        this.step = null;
    }
    
    @Override
    public boolean hasNotes() {
        return step != null && step.state() == NoteStep.State.NoteOn;
    }
    
    @Override
    public boolean hasSustainNotes() {
        return step != null && step.state() == NoteStep.State.NoteSustain;
    }
    
    @Override
    public Collection<NoteStep> steps() {
        return step == null ? List.of() : List.of(step);
    }
    
    @Override
    public boolean containsNote(final int key) {
        // return map.containsKey(key);
        return false;  // Hm
    }
    
    
    @Override
    public NoteStepSlot copy() {
        final NoteStepSlot copy = new NoteStepSlot();
        copy.updateNote(step);
        return copy;
    }
}
