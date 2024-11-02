package com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer.value;

import com.bitwig.extension.controller.api.NoteStep;

public abstract class DoubleNoteValueHandler extends NoteValueHandler {
    protected final NoteGetDouble getFunction;
    protected final NoteSetDouble setFunction;
    
    @FunctionalInterface
    public interface NoteSetDouble {
        void set(NoteStep note, double value);
    }
    
    @FunctionalInterface
    public interface NoteGetDouble {
        double get(NoteStep note);
    }
    
    public DoubleNoteValueHandler(final NoteGetDouble getFunction, final NoteSetDouble setFunction) {
        this.getFunction = getFunction;
        this.setFunction = setFunction;
    }
    
    protected double[] determineMinMax() {
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
        return new double[] {min, max};
    }
    
    
}
