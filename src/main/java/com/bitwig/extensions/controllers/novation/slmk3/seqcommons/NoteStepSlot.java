package com.bitwig.extensions.controllers.novation.slmk3.seqcommons;

import java.util.Collection;
import java.util.HashMap;

import com.bitwig.extension.controller.api.NoteStep;

public class NoteStepSlot implements INoteStepSlot {
    private final HashMap<Integer, NoteStep> map = new HashMap<>();
    private final HashMap<Integer, NoteStep> sustainNotes = new HashMap<>();
    
    public NoteStepSlot() {
    }
    
    @Override
    public void updateNote(final NoteStep step) {
        if (step.state() == NoteStep.State.NoteOn) {
            map.put(step.y(), step);
        } else if (step.state() == NoteStep.State.NoteSustain) {
            sustainNotes.put(step.y(), step);
        } else {
            map.remove(step.y());
            sustainNotes.remove(step.y());
        }
    }
    
    @Override
    public void clear() {
        map.clear();
    }
    
    @Override
    public boolean hasNotes() {
        return !map.isEmpty();
    }
    
    @Override
    public boolean hasSustainNotes() {
        return !sustainNotes.isEmpty();
    }
    
    @Override
    public Collection<NoteStep> steps() {
        return map.values();
    }
    
    @Override
    public NoteStepSlot copy() {
        final NoteStepSlot copy = new NoteStepSlot();
        copy.map.putAll(map);
        return copy;
    }
    
    @Override
    public boolean containsNote(final int key) {
        return map.containsKey(key);
    }
    
}
