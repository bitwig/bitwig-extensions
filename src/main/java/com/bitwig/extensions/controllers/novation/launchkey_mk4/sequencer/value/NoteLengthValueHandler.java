package com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer.value;

import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.values.ValueSet;

public class NoteLengthValueHandler extends NoteValueHandler {
    
    private final ValueSet gridValue;
    
    public NoteLengthValueHandler(final NoteGetDouble getFunction, final NoteSetDouble setFunction,
        final ValueSet gridValue) {
        super(getFunction, setFunction);
        this.gridValue = gridValue;
    }
    
    @Override
    protected boolean incStep(final NoteStep step, final int incAmount) {
        final int stepLen = (int) Math.round(getFunction.get(step) / gridValue.getValue());
        final int newLen = stepLen - incAmount;
        if (newLen > 0) {
            setFunction.set(step, newLen * gridValue.getValue());
            return true;
        }
        return false;
    }
    
    @Override
    protected void calcDisplayValue() {
        boolean first = true;
        double min = 0;
        double max = 0;
        for (final NoteStep step : currentSteps) {
            final double value = getFunction.get(step);
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
        
        final int minSteps = (int) Math.round(min / gridValue.getValue());
        final int maxSteps = (int) Math.round(max / gridValue.getValue());
        
        if (min == max) {
            displayValue.set("%d Steps".formatted(minSteps));
        } else {
            displayValue.set("%d-%d Steps".formatted(minSteps, maxSteps));
        }
    }
}
