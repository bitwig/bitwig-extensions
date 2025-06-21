package com.bitwig.extensions.controllers.novation.slmk3.sequencer.value;

import java.util.List;

import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.controllers.novation.slmk3.sequencer.ClipState;
import com.bitwig.extensions.framework.values.BasicStringValue;

public abstract class NoteStepControlValue implements ControlValue {
    protected final BasicStringValue displayValue = new BasicStringValue("--");
    protected final ClipState clipState;
    protected final IncBuffer incBuffer;
    
    protected NoteStepControlValue(final ClipState clipState, final int incBuffer) {
        this.clipState = clipState;
        this.incBuffer = incBuffer == 0 ? null : new IncBuffer(incBuffer);
    }
    
    @Override
    public BasicStringValue getDisplayValue() {
        return displayValue;
    }
    
    @Override
    public void incrementBy(final int inc, final boolean modifier) {
        if (!clipState.hasHeldStepsAndNotes()) {
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
    
    protected abstract boolean incStep(final NoteStep step, final int incAmount);
}
