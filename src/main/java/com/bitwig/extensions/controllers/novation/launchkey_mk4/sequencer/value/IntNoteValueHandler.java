package com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer.value;

import com.bitwig.extension.controller.api.NoteStep;

public class IntNoteValueHandler extends NoteValueHandler {
    protected final NoteGetInt getFunction;
    protected final NoteSetInt setFunction;
    protected int minValue;
    protected int maxValue;
    
    @FunctionalInterface
    public interface NoteGetInt {
        int get(NoteStep note);
    }
    
    @FunctionalInterface
    public interface NoteSetInt {
        void set(NoteStep note, int value);
    }
    
    public IntNoteValueHandler(final NoteGetInt getFunction, final NoteSetInt setFunction, final int minValue,
        final int maxValue) {
        this.getFunction = getFunction;
        this.setFunction = setFunction;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }
    
    @Override
    protected boolean incStep(final NoteStep step, final int incAmount) {
        final int value = getFunction.get(step);
        final int newValue = value - incAmount;
        if (newValue >= minValue && newValue <= maxValue) {
            setFunction.set(step, newValue);
            return true;
        }
        return false;
    }
    
    protected int[] determineMinMax() {
        boolean first = true;
        int min = 0;
        int max = 0;
        for (final NoteStep step : currentSteps) {
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
    protected void calcDisplayValue() {
        if (currentSteps.isEmpty()) {
            displayValue.set("-");
        }
        final int[] minMax = determineMinMax();
        if (minMax[0] == minMax[1]) {
            displayValue.set("%d".formatted(minMax[0]));
        } else {
            displayValue.set("%d-%d".formatted(minMax[0], minMax[1]));
        }
    }
}
