package com.bitwig.extensions.controllers.akai.mpkmk4;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extensions.framework.values.IntValueObject;
import com.bitwig.extensions.framework.values.Scale;
import com.bitwig.extensions.framework.values.ValueObject;

public class ScaleSetup {
    private final static String[] NOTES =
        {"  C", " C#", "  D", " D#", "  E", "  F", " F#", "  G", " G#", "  A", " A#", "  B"};
    private final ValueObject<Scale> scale =
        new ValueObject<>(Scale.CHROMATIC, ScaleSetup::increment, ScaleSetup::convert);
    private final IntValueObject baseNote = new IntValueObject(0, 0, 11, v -> NOTES[v]);
    private final IntValueObject octaveOffset = new IntValueObject(4, 0, 8);
    private final List<Runnable> changeListeners = new ArrayList<>();
    
    public ScaleSetup() {
        baseNote.addValueObserver(v -> fireChange());
        octaveOffset.addValueObserver(v -> fireChange());
        scale.addValueObserver(v -> fireChange());
    }
    
    public IntValueObject getBaseNote() {
        return baseNote;
    }
    
    public IntValueObject getOctaveOffset() {
        return octaveOffset;
    }
    
    public ValueObject<Scale> getScale() {
        return scale;
    }
    
    public String getScaleInfo() {
        return "%s %s".formatted(NOTES[baseNote.get()], scale.get().getName());
    }
    
    private void fireChange() {
        changeListeners.forEach(l -> l.run());
    }
    
    public static String toNote(final int noteValue) {
        return NOTES[noteValue % 12];
    }
    
    public void addChangeListener(final Runnable action) {
        changeListeners.add(action);
    }
    
    public boolean canIncrementScale(final int dir) {
        if (dir < 0 == scale.get().ordinal() > 0) {
            return true;
        } else {
            return dir > 0 && scale.get().ordinal() + dir < Scale.values().length;
        }
    }
    
    private static Scale increment(final Scale current, final int amount) {
        final int ord = current.ordinal();
        final Scale[] values = Scale.values();
        final int newOrd = ord + amount;
        if (newOrd < 0) {
            return values[0];
        }
        if (newOrd >= values.length) {
            return values[values.length - 1];
        }
        return values[newOrd];
    }
    
    private static String convert(final Scale scale) {
        return scale.getName();
    }
    
    public List<Integer> getNoteSequence(final int len) {
        final List<Integer> result = new ArrayList<>();
        final int[] intervals = scale.get().getIntervals();
        for (int i = 0; i < len; i++) {
            final int octave = octaveOffset.get() + i / intervals.length;
            final int note = baseNote.get() + octave * 12 + intervals[i % intervals.length];
            if (note < 128) {
                result.add(note);
            }
        }
        return result;
    }
    
    public boolean[] getBaseNotes(final int len) {
        final boolean[] result = new boolean[len];
        final int[] intervals = scale.get().getIntervals();
        for (int i = 0; i < len; i++) {
            if (i % intervals.length == 0) {
                result[i] = true;
            }
        }
        
        return result;
    }
    
    public String getStartInfo() {
        return "%s(%d)".formatted(octaveOffset.get(), octaveOffset.get() * 12 + baseNote.get());
    }
}
