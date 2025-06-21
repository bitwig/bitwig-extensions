package com.bitwig.extensions.controllers.novation.slmk3.sequencer.value;

import java.util.List;

import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.controllers.novation.slmk3.sequencer.ClipState;

public class NoteGainValueControl extends NoteStepControlValue {
    
    public NoteGainValueControl(final ClipState clipState) {
        super(clipState, 0);
    }
    
    @Override
    public void update() {
        if (!clipState.hasHeldStepsAndNotes()) {
            displayValue.set("--");
        } else {
            final List<NoteStep> notes = clipState.getHeldNotes();
            final double[] minMax = determineMinMax(notes);
            displayValue.set(this.convertValueRange(minMax[0], minMax[1]));
        }
    }
    
    private String convertValueRange(final double min, final double max) {
        if (min == max) {
            return "%2.0f%%".formatted(min * 100);
        }
        return "%2.0f-%2.0f%%".formatted(min * 100, max * 100);
    }
    
    protected boolean incStep(final NoteStep step, final int incAmount) {
        final double value = step.gain() / 2;
        final double newValue = Math.min(1, Math.max(0, value + 0.05 * incAmount));
        if (newValue != value) {
            step.setGain(newValue);
            return true;
        }
        return false;
    }
    
    protected double[] determineMinMax(final List<NoteStep> steps) {
        boolean first = true;
        double min = 0;
        double max = 0;
        for (final NoteStep step : steps) {
            final double value = step.gain() / 2;
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
        return new double[] {min, max};
    }
    
    
}
