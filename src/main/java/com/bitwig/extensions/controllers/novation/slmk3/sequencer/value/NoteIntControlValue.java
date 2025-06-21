package com.bitwig.extensions.controllers.novation.slmk3.sequencer.value;

import java.util.List;

import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.controllers.novation.slmk3.sequencer.ClipState;

public class NoteIntControlValue extends NoteStepControlValue {
    private final NoteGetInt getFunction;
    private final NoteSetInt setFunction;
    protected int minValue = 0;
    protected int maxValue = 0;
    private final boolean normalize;
    
    @FunctionalInterface
    public interface NoteGetInt {
        int get(NoteStep note);
    }
    
    @FunctionalInterface
    public interface NoteSetInt {
        void set(NoteStep note, int value);
    }
    
    public NoteIntControlValue(final ClipState clipState, final NoteGetInt getFunction, final NoteSetInt setFunction) {
        this(clipState, getFunction, setFunction, 0, 1, 0, true);
    }
    
    public NoteIntControlValue(final ClipState clipState, final NoteGetInt getFunction, final NoteSetInt setFunction,
        final int minValue, final int maxValue) {
        this(clipState, getFunction, setFunction, minValue, maxValue, 0, true);
    }
    
    public NoteIntControlValue(final ClipState clipState, final NoteGetInt getFunction, final NoteSetInt setFunction,
        final int minValue, final int maxValue, final int incBuffer, final boolean normalize) {
        super(clipState, incBuffer);
        this.getFunction = getFunction;
        this.setFunction = setFunction;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.normalize = normalize;
    }
    
    @Override
    public void update() {
        if (!clipState.hasHeldStepsAndNotes()) {
            displayValue.set("--");
        } else {
            final List<NoteStep> notes = clipState.getHeldNotes();
            final int[] minMax = determineMinMax(notes);
            if (minMax[0] == minMax[1]) {
                displayValue.set("%d".formatted(minMax[0]));
            } else {
                displayValue.set("%d-%d".formatted(minMax[0], minMax[1]));
            }
        }
    }
    
    protected int[] determineMinMax(final List<NoteStep> steps) {
        boolean first = true;
        int min = 0;
        int max = 0;
        for (final NoteStep step : steps) {
            final int value = getFunction.get(step);
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
    
    @Override
    public void incrementBy(final int inc, final boolean modifier) {
        if (!clipState.hasHeldSteps()) {
            return;
        }
        final int increment = incBuffer == null ? inc : incBuffer.inc(inc);
        if (increment == 0) {
            return;
        }
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
    
    protected boolean incStep(final NoteStep step, final int incAmount) {
        final int value = getFunction.get(step);
        final int newValue = value + incAmount;
        if (newValue >= minValue && newValue <= maxValue) {
            setFunction.set(step, newValue);
            return true;
        }
        return false;
    }
    
}
