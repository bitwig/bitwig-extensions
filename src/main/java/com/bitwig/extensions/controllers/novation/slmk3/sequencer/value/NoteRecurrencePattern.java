package com.bitwig.extensions.controllers.novation.slmk3.sequencer.value;

import java.util.List;

import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.controllers.novation.slmk3.sequencer.ClipState;

public class NoteRecurrencePattern extends NoteStepControlValue {
    
    public NoteRecurrencePattern(final ClipState clipState, final int incBuffer) {
        super(clipState, incBuffer);
    }
    
    @Override
    public void update() {
        if (!clipState.hasHeldStepsAndNotes()) {
            displayValue.set("--");
        } else {
            final List<NoteStep> notes = clipState.getHeldNotes();
            if (!notes.isEmpty()) {
                final int len = notes.get(0).recurrenceLength();
                final int mask = notes.get(0).recurrenceMask();
                final StringBuilder v = new StringBuilder();
                for (int i = 0; i < len; i++) {
                    v.append((((1 << i) & mask) != 0 ? 'x' : 'o'));
                }
                displayValue.set(v.toString());
            }
        }
    }
    
    @Override
    protected boolean incStep(final NoteStep step, final int incAmount) {
        final int len = step.recurrenceLength();
        final int mask = step.recurrenceMask();
        final int max = (1 << len) - 1;
        final int newValue = Math.min(max, Math.max(0, mask + incAmount));
        if (mask != newValue) {
            step.setRecurrence(len, newValue);
            return true;
        }
        return false;
    }
    
    public int getLength() {
        final List<NoteStep> notes = clipState.getHeldNotes();
        if (!notes.isEmpty()) {
            return notes.get(0).recurrenceLength();
        }
        return 0;
    }
    
    public void toggleMask(final int index) {
        final int length = getLength();
        if (index >= length) {
            return;
        }
        int currentMask = getMask();
        final int mask = 0x1 << index;
        if ((mask & currentMask) != 0) {
            currentMask &= ~mask;
        } else {
            currentMask |= mask;
        }
        final List<NoteStep> notes = clipState.getHeldNotes();
        for (final NoteStep step : notes) {
            step.setRecurrence(length, currentMask);
        }
        update();
    }
    
    
    public int getMask() {
        final List<NoteStep> notes = clipState.getHeldNotes();
        if (!notes.isEmpty()) {
            return notes.get(0).recurrenceMask();
        }
        return 0;
    }
}
