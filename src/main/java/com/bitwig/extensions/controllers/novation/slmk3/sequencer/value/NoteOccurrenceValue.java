package com.bitwig.extensions.controllers.novation.slmk3.sequencer.value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bitwig.extension.controller.api.NoteOccurrence;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.controllers.novation.slmk3.sequencer.ClipState;

public class NoteOccurrenceValue extends NoteStepControlValue {
    private static final Map<NoteOccurrence, String> VALUE_MAP = new HashMap<>();
    private static final List<NoteOccurrence> SEQ = new ArrayList<>();
    
    static {
        register(NoteOccurrence.ALWAYS, "Always");
        register(NoteOccurrence.FIRST, "First");
        register(NoteOccurrence.NOT_FIRST, "Not First");
        register(NoteOccurrence.PREV, "Previous");
        register(NoteOccurrence.NOT_PREV, "Not Prev");
        register(NoteOccurrence.PREV_CHANNEL, "Prev.Ch");
        register(NoteOccurrence.NOT_PREV_CHANNEL, "Not.Pr.Ch");
        register(NoteOccurrence.PREV_KEY, "Pr.Key");
        register(NoteOccurrence.NOT_PREV_KEY, "Not.Pr.Key");
        register(NoteOccurrence.FILL, "Fill");
        register(NoteOccurrence.NOT_FILL, "Not Fill");
    }
    
    private static void register(final NoteOccurrence value, final String displayName) {
        VALUE_MAP.put(value, displayName);
        SEQ.add(value);
    }
    
    public NoteOccurrenceValue(final ClipState clipState, final int incBuffer) {
        super(clipState, incBuffer);
    }
    
    @Override
    public void update() {
        if (!clipState.hasHeldStepsAndNotes()) {
            displayValue.set("--");
        } else {
            final List<NoteStep> notes = clipState.getHeldNotes();
            if (notes.size() == 1) {
                displayValue.set(VALUE_MAP.get(notes.get(0).occurrence()));
            } else {
                final int[] minMax = determineMinMax(notes);
                if (minMax[0] == minMax[1]) {
                    displayValue.set(VALUE_MAP.get(SEQ.get(minMax[0])));
                } else {
                    displayValue.set(VALUE_MAP.get(SEQ.get(minMax[0])) + "-" + VALUE_MAP.get(SEQ.get(minMax[1])));
                }
            }
        }
    }
    
    protected int[] determineMinMax(final List<NoteStep> steps) {
        boolean first = true;
        int min = 0;
        int max = 0;
        for (final NoteStep step : steps) {
            final int value = step.occurrence().ordinal();
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
        return new int[] {min, max};
    }
    
    
    @Override
    protected boolean incStep(final NoteStep step, final int incAmount) {
        final int value = step.occurrence().ordinal();
        final NoteOccurrence newValue = SEQ.get(Math.min(SEQ.size() - 1, Math.max(0, value + incAmount)));
        if (step.occurrence() != newValue) {
            step.setOccurrence(newValue);
            return true;
        }
        return false;
    }
    
    
}
