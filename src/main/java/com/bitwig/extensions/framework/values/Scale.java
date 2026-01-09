package com.bitwig.extensions.framework.values;

public enum Scale implements IScale {
    
    CHROMATIC("Chromatic", 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), //
    MAJOR("Ionian/Major", "Major", 0, 2, 4, 5, 7, 9, 11), //
    MINOR("Aeolian/Minor", "Minor", 0, 2, 3, 5, 7, 8, 10), //
    PENTATONIC("Pentatonic", 0, 2, 4, 7, 9), //
    PENTATONIC_MINOR("Pentatonic Minor", "Pent.Min", 0, 3, 5, 7, 10), //
    DORIAN("Dorian (B/g)", "Dorian", 0, 2, 3, 5, 7, 9, 10), //
    PHRYGIAN("Phrygian (A-flat/f)", "Phrygian", 0, 1, 3, 5, 7, 8, 10), //
    LYDIAN("Lydian (D/e)", "Lydian", 0, 2, 4, 6, 7, 9, 11), //
    MIXOLYDIAN("Mixolydian (F/d)", "Mixolydian", 0, 2, 4, 5, 7, 9, 10), //
    LOCRIAN("Locrian", 0, 1, 3, 5, 6, 8, 10), //
    DIMINISHED("Diminished", 0, 2, 3, 5, 6, 8, 9, 10), //
    MAJOR_BLUES("Major Blues", 0, 3, 4, 7, 9, 10), //
    MINOR_BLUES("Minor Blues", 0, 3, 4, 6, 7, 10), //
    WHOLE_TONE("Whole", 0, 2, 4, 6, 8, 10), //
    ARABIAN("Arabian", 0, 2, 4, 5, 6, 8, 10), //
    EGYPTIAN("Egyptian", 0, 2, 5, 7, 10), //
    GYPSI("Gypsi", 0, 2, 3, 6, 7, 8, 11), //
    SPANISH("Spanish", 0, 1, 3, 4, 5, 7, 8, 10);
    
    
    private final String name;
    private final String shortName;
    private final int[] intervals;
    private final boolean[] inscaleMatch = new boolean[12];
    
    Scale(final String name, final int... notes) {
        this(name, name, notes);
    }
    
    Scale(final String name, final String shortName, final int... notes) {
        this.name = name;
        this.shortName = shortName;
        this.intervals = notes;
        for (int i = 0; i < this.intervals.length; i++) {
            inscaleMatch[this.intervals[i] % 12] = true;
        }
    }
    
    @Override
    public IScale[] getValues() {
        return Scale.values();
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    public String getShortName() {
        return shortName;
    }
    
    @Override
    public int[] getIntervals() {
        return intervals;
    }
    
    @Override
    public boolean inScale(final int noteBase) {
        return inscaleMatch[noteBase % 12];
    }
    
    /**
     * Given a starting note, determines the highest note at the end of the range.
     *
     * @param startNote starting note
     * @param noteRange available notes
     * @return last note in range
     */
    @Override
    public int highestNote(final int startNote, final int noteRange) {
        final int octaves = noteRange / intervals.length;
        final int lastValue = intervals[(noteRange - 1) % intervals.length];
        return startNote + octaves * 12 + lastValue;
    }
    
    
}
