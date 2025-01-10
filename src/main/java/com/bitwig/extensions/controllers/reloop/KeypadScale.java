package com.bitwig.extensions.controllers.reloop;

import com.bitwig.extensions.framework.values.IScale;

public enum KeypadScale implements IScale {
    
    CHROMATIC("Chromatic", "Chrom", 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), //
    PENTATONIC("Pentatonic", "Penta", 0, 2, 4, 7, 9), //
    PENTATONIC_MINOR("Pentatonic Minor", "PentaMin", 0, 3, 5, 7, 10), //
    MAJOR_8("Ionian/Major", "Major", 0, 2, 4, 5, 7, 9, 11, 12), //
    MINOR_8("Aeolian/Minor", "Minor", 0, 2, 3, 5, 7, 8, 10, 12), //
    DORIAN_8("Dorian (B/g)", "Dorian", 0, 2, 3, 5, 7, 9, 10, 12), //
    PHRYGIAN_8("Phrygian (A-flat/f)", "Phryg", 0, 1, 3, 5, 7, 8, 10, 12), //
    MIXOLYDIAN_8("Mixolydian (F/d)", "Mixo", 0, 2, 4, 5, 7, 9, 10, 12),
    MINOR_BLUES("Minor Blues", "MinBlue", 0, 3, 4, 6, 7, 10), //
    MAJOR_BLUES_8("Major Blues", "MajBlue", 0, 3, 4, 7, 9, 10), //
    ;
    
    private final String name;
    private final int[] intervals;
    private final String shortName;
    private final boolean[] inScaleMatch = new boolean[12];
    
    KeypadScale(final String name, final String shortName, final int... notes) {
        this.name = name;
        this.intervals = notes;
        this.shortName = shortName;
        for (int i = 0; i < this.intervals.length; i++) {
            inScaleMatch[this.intervals[i] % 12] = true;
        }
    }
    
    public String getName() {
        return name;
    }
    
    public int[] getIntervals() {
        return intervals;
    }
    
    @Override
    public IScale[] getValues() {
        return KeypadScale.values();
    }
    
    @Override
    public boolean inScale(final int noteBase) {
        return inScaleMatch[noteBase % 12];
    }
    
    public String getShortName() {
        return shortName;
    }
    
    /**
     * Given a starting note, determines the highest note at the end of the range.
     *
     * @param startNote starting note
     * @param noteRange available notes
     * @return last note in range
     */
    public int highestNote(final int startNote, final int noteRange) {
        final int octaves = noteRange / intervals.length;
        final int lastValue = intervals[(noteRange - 1) % intervals.length];
        return startNote + octaves * 12 + lastValue;
    }
    
}
