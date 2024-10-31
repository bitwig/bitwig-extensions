package com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer.value;

import java.util.List;

import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.framework.values.BasicStringValue;

public abstract class NoteValueHandler {
    protected List<NoteStep> currentSteps;
    protected final BasicStringValue displayValue = new BasicStringValue("-");
    protected final NoteGetDouble getFunction;
    protected final NoteSetDouble setFunction;
    
    public NoteValueHandler(final NoteGetDouble getFunction, final NoteSetDouble setFunction) {
        this.getFunction = getFunction;
        this.setFunction = setFunction;
    }
    
    public BasicStringValue getDisplayValue() {
        return displayValue;
    }
    
    public void doIncrement(final int incAmount) {
        boolean update = false;
        for (final NoteStep step : this.currentSteps) {
            if (incStep(step, incAmount)) {
                update = true;
            }
        }
        if (update) {
            calcDisplayValue();
        }
    }
    
    protected abstract boolean incStep(NoteStep step, int incAmount);
    
    public void setSteps(final List<NoteStep> steps) {
        this.currentSteps = steps;
        calcDisplayValue();
    }
    
    protected abstract void calcDisplayValue();
}
