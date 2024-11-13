package com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer.value;

import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.values.ValueSet;

public class NoteLengthFineValueHandler extends DoubleNoteValueHandler {
    
    private final ValueSet gridValue;
    
    public NoteLengthFineValueHandler(final NoteGetDouble getFunction, final NoteSetDouble setFunction,
        final ValueSet gridValue) {
        super(getFunction, setFunction);
        this.gridValue = gridValue;
    }
    
    @Override
    protected boolean incStep(final NoteStep step, final int incDir) {
        final double value = getFunction.get(step) / gridValue.getValue();
        final double newLen = value - incDir * 0.1;
        if (newLen > 0) {
            setFunction.set(step, newLen * gridValue.getValue());
            return true;
        }
        return false;
    }
    
    @Override
    protected void calcDisplayValue() {
        final double[] minMax = determineMinMax();
        
        final double minSteps = minMax[0] / gridValue.getValue();
        final double maxSteps = minMax[1] / gridValue.getValue();
        
        if (minMax[0] == minMax[1]) {
            displayValue.set("%2.1f Steps".formatted(minSteps));
        } else {
            displayValue.set("%2.1f-%2.1f Steps".formatted(minSteps, maxSteps));
        }
    }
}
