package com.bitwig.extensions.controllers.novation.slmk3.sequencer.value;

import java.util.List;

import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.controllers.novation.slmk3.sequencer.ClipState;
import com.bitwig.extensions.controllers.novation.slmk3.value.ValueSet;

public class NoteDurationControlValue extends NoteDoubleControlValue {
    
    private final ValueSet gridResolution;
    
    public NoteDurationControlValue(final ClipState clipState, final ValueSet gridResolution) {
        super(clipState, 1.0, 0.05, NoteStep::duration, NoteStep::setDuration, 0.01, 16, 3, true,
            NoteDurationControlValue::convertToString);
        this.gridResolution = gridResolution;
    }
    
    private static String convertToString(final double min, final double max) {
        if (min == max) {
            return "%2.2f".formatted(min * 4);
        }
        return "%2.2f-%2.2f".formatted(min * 4, max * 4);
    }
    
    protected boolean incStep(final NoteStep step, final int incValue, final boolean modifier) {
        final double value = getFunction.get(step);
        
        if (modifier) {
            final double min = gridResolution.getValue() * 0.01;
            final double newValue =
                Math.min(128, Math.max(min, value + incValue * gridResolution.getValue() * modIncAmount));
            if (newValue != value) {
                this.setFunction.set(step, newValue);
                return true;
            }
        } else {
            final int stepLen = (int) Math.round(value / gridResolution.getValue());
            final double newLen = (stepLen + incValue) * gridResolution.getValue();
            if (newLen > 0 && newLen <= 128 && newLen != value) {
                this.setFunction.set(step, newLen);
                return true;
            }
        }
        
        
        return false;
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
            if (incStep(step, increment, modifier)) {
                changed = true;
            }
        }
        if (changed) {
            update();
        }
    }
    
    @Override
    public void update() {
        if (!clipState.hasHeldStepsAndNotes()) {
            displayValue.set("--");
        } else {
            final List<NoteStep> notes = clipState.getHeldNotes();
            final double[] minMax = determineMinMax(notes);
            if (minMax[0] == minMax[1]) {
                displayValue.set("%2.2f".formatted(minMax[0] * 4));
            } else {
                displayValue.set("%2.2f-%2.2f".formatted(minMax[0] * 4, minMax[1] * 4));
            }
        }
    }
    
}
