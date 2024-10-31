package com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer;

import java.util.Collection;

import com.bitwig.extension.controller.api.NoteStep;

public interface INoteStepSlot {
    void updateNote(NoteStep step);
    
    void clear();
    
    boolean hasNotes();
    
    boolean hasSustainNotes();
    
    Collection<NoteStep> steps();
    
    NoteStepSlot copy();
    
    boolean containsNote(int key);
}
