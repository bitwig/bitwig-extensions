package com.bitwig.extensions.controllers.novation.slmk3.sequencer.value;

import java.util.List;

import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.controllers.novation.slmk3.sequencer.ClipState;

public class NoteDoubleControlValue extends NoteStepControlValue {
    private final StringConverter converter;
    protected double incAmount = 0.01;
    protected double modIncAmount = 0.01;
    protected final NoteGetDouble getFunction;
    protected final NoteSetDouble setFunction;
    protected double minValue = 0;
    protected double maxValue = 0;
    private final boolean normalize;
    
    public interface StringConverter {
        String convert(double min, double max);
    }
    
    @FunctionalInterface
    public interface NoteGetDouble {
        double get(NoteStep note);
    }
    
    @FunctionalInterface
    public interface NoteSetDouble {
        void set(NoteStep note, double value);
    }
    
    public NoteDoubleControlValue(final ClipState clipState, final NoteGetDouble getFunction,
        final NoteSetDouble setFunction) {
        this(clipState, 0.01, 0.01, getFunction, setFunction, 0, 1, 0, true, null);
    }
    
    public NoteDoubleControlValue(final ClipState clipState, final NoteGetDouble getFunction,
        final NoteSetDouble setFunction, final StringConverter converter) {
        this(clipState, 0.01, 0.01, getFunction, setFunction, 0, 1, 0, true, converter);
    }
    
    public NoteDoubleControlValue(final ClipState clipState, final NoteGetDouble getFunction,
        final NoteSetDouble setFunction, final double minValue) {
        this(clipState, 0.01, 0.01, getFunction, setFunction, minValue, 1, 0, true, null);
    }
    
    public NoteDoubleControlValue(final ClipState clipState, final double incAmount, final double modIncAmount,
        final NoteGetDouble getFunction, final NoteSetDouble setFunction, final double minValue, final double maxValue,
        final int incBuffer, final boolean normalize, final StringConverter converter) {
        super(clipState, incBuffer);
        this.incAmount = incAmount;
        this.modIncAmount = modIncAmount;
        this.getFunction = getFunction;
        this.setFunction = setFunction;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.normalize = normalize;
        this.converter = converter == null ? this::convertValueRange : converter;
    }
    
    @Override
    public void update() {
        if (!clipState.hasHeldStepsAndNotes()) {
            displayValue.set("--");
        } else {
            final List<NoteStep> notes = clipState.getHeldNotes();
            final double[] minMax = determineMinMax(notes);
            displayValue.set(this.converter.convert(minMax[0], minMax[1]));
        }
    }
    
    private String convertValueRange(final double min, final double max) {
        if (min == max) {
            return "%2.0f%%".formatted(min * 100);
        }
        return "%2.0f-%2.0f%%".formatted(min * 100, max * 100);
    }
    
    protected double[] determineMinMax(final List<NoteStep> steps) {
        boolean first = true;
        double min = 0;
        double max = 0;
        for (final NoteStep step : steps) {
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
        final double value = getFunction.get(step);
        final double newValue = Math.min(this.maxValue, Math.max(this.minValue, value + 0.01 * incAmount));
        if (newValue != value) {
            this.setFunction.set(step, newValue);
            return true;
        }
        return false;
    }
    
    
}
