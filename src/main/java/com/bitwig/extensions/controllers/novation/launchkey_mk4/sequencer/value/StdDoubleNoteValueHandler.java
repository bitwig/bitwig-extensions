package com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer.value;

import com.bitwig.extension.controller.api.NoteStep;

public class StdDoubleNoteValueHandler extends DoubleNoteValueHandler {
    protected double incAmount = 0.01;
    protected double minValue = 0;
    protected double maxValue = 0;
    
    public StdDoubleNoteValueHandler(final NoteGetDouble getFunction, final NoteSetDouble setFunction) {
        this(getFunction, setFunction, 0, 1, 0.1);
    }
    
    public StdDoubleNoteValueHandler(final NoteGetDouble getFunction, final NoteSetDouble setFunction, final double min,
        final double max, final double incAmount) {
        super(getFunction, setFunction);
        this.minValue = min;
        this.maxValue = max;
        this.incAmount = incAmount;
    }
    
    @Override
    protected void calcDisplayValue() {
        if (currentSteps.isEmpty()) {
            displayValue.set("-");
        }
        final double[] minMax = determineMinMax();
        if (minMax[0] == minMax[1]) {
            displayValue.set("%3.0f%%".formatted(minMax[0] * 100));
        } else {
            displayValue.set("%3.0f-%3.0f%%".formatted(minMax[0] * 100, minMax[1] * 100));
        }
    }
    
    @Override
    protected boolean incStep(final NoteStep step, final int incAmount) {
        final double value = getFunction.get(step);
        final double newValue = Math.min(this.maxValue, Math.max(this.minValue, value - 0.01 * incAmount));
        if (newValue != value) {
            this.setFunction.set(step, newValue);
            return true;
        }
        return false;
    }
    
}
