package com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer.value;

import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.values.ValueSet;

public class NoteLengthValueHandler extends DoubleNoteValueHandler {
    
    private final ValueSet gridValue;
    
    public NoteLengthValueHandler(final NoteGetDouble getFunction, final NoteSetDouble setFunction,
        final ValueSet gridValue) {
        super(getFunction, setFunction);
        this.gridValue = gridValue;
    }
    
    @Override
    protected boolean incStep(final NoteStep step, final int incDir) {
        final int stepLen = (int) Math.round(getFunction.get(step) / gridValue.getValue());
        final int newLen = stepLen - incDir;
        if (newLen > 0) {
            setFunction.set(step, newLen * gridValue.getValue());
            return true;
        }
        return false;
    }
    
    @Override
    protected void calcDisplayValue() {
        final double[] minMax = determineMinMax();
        
        final int minSteps = (int) Math.round(minMax[0] / gridValue.getValue());
        final int maxSteps = (int) Math.round(minMax[1] / gridValue.getValue());
        
        if (minSteps == maxSteps) {
            displayValue.set("%d Steps".formatted(minSteps));
        } else {
            displayValue.set("%d-%d Steps".formatted(minSteps, maxSteps));
        }
    }
}
