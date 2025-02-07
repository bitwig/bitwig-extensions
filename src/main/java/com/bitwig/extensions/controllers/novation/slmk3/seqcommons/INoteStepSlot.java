package com.bitwig.extensions.controllers.novation.slmk3.seqcommons;

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
