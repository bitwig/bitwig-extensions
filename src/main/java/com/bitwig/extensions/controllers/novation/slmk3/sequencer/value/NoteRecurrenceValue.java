package com.bitwig.extensions.controllers.novation.slmk3.sequencer.value;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.controllers.novation.slmk3.sequencer.ClipState;

public class NoteRecurrenceValue extends NoteStepControlValue {
    
    private final NoteRecurrencePattern patternValue;
    private final List<Runnable> changeListeners = new ArrayList<>();
    
    public NoteRecurrenceValue(final ClipState clipState, final int incBuffer, final NoteRecurrencePattern pattern) {
        super(clipState, incBuffer);
        this.patternValue = pattern;
    }
    
    @Override
    public void update() {
        if (!clipState.hasHeldStepsAndNotes()) {
            displayValue.set("--");
        } else {
            final List<NoteStep> notes = clipState.getHeldNotes();
            if (notes.size() == 1) {
                displayValue.set("%d".formatted(notes.get(0).recurrenceLength()));
            } else if (notes.size() > 1) {
                final int[] minMax = determineMinMax(notes);
                if (minMax[0] == minMax[1]) {
                    displayValue.set("%d".formatted(notes.get(0).recurrenceLength()));
                } else {
                    displayValue.set("%d-%d".formatted(minMax[0], minMax[1]));
                }
            }
            patternValue.update();
        }
    }
    
    public void addChangeListener(final Runnable changeAction) {
        this.changeListeners.add(changeAction);
    }
    
    protected int[] determineMinMax(final List<NoteStep> steps) {
        boolean first = true;
        int min = 0;
        int max = 0;
        for (final NoteStep step : steps) {
            final int value = step.recurrenceLength();
            if (first) {
                min = value;
                max = value;
                first = false;
            } else if (value >= max) {
                max = value;
            } else if (value <= min) {
                min = value;
            }
        }
        return new int[] {min, max};
    }
    
    public void incrementBy(final int inc, final boolean modifier) {
        if (!clipState.hasHeldSteps()) {
            return;
        }
        final int increment = incBuffer == null ? inc : incBuffer.inc(inc);
        if (increment == 0) {
            notifyTurn();
            return;
        }
        notifyTurn();
        final List<NoteStep> notes = clipState.getHeldNotes();
        boolean changed = false;
        for (final NoteStep step : notes) {
            if (incStep(step, increment)) {
                changed = true;
            }
        }
        if (changed) {
            update();
        }
    }
    
    private void notifyTurn() {
        changeListeners.forEach(action -> action.run());
    }
    
    
    @Override
    protected boolean incStep(final NoteStep step, final int incAmount) {
        final int value = step.recurrenceLength();
        final int mask = step.recurrenceMask();
        final int newValue = Math.min(8, Math.max(1, value + incAmount));
        if (step.recurrenceLength() != newValue) {
            step.setRecurrence(newValue, mask);
            return true;
        }
        return false;
    }
    
    
}
