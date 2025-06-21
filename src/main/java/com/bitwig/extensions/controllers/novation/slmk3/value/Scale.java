package com.bitwig.extensions.controllers.novation.slmk3.value;

public enum Scale implements ScaleInterface {
    
    CHROMATIC("Chromatic", "Chrom", 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), //
    MAJOR("Ionian/Major", "Major", 0, 2, 4, 5, 7, 9, 11), //
    MINOR("Aeolian/Minor", "Minor", 0, 2, 3, 5, 7, 8, 10), //
    PENTATONIC("Pentatonic", "Penta", 0, 2, 4, 7, 9), //
    PENTATONIC_MINOR("Pentatonic Minor", "PentaMin", 0, 3, 5, 7, 10), //
    DORIAN("Dorian (B/g)", "Dorian", 0, 2, 3, 5, 7, 9, 10), //
    PHRYGIAN("Phrygian (A-flat/f)", "Phryg", 0, 1, 3, 5, 7, 8, 10), //
    LYDIAN("Lydian (D/e)", "Lydian", 0, 2, 4, 6, 7, 9, 11), //
    MIXOLYDIAN("Mixolydian (F/d)", "Mixo", 0, 2, 4, 5, 7, 9, 10), //
    LOCRIAN("Locrian", "Locr", 0, 1, 3, 5, 6, 8, 10), //
    DIMINISHED("Diminished", "", 0, 2, 3, 5, 6, 8, 9, 10), //
    MAJOR_BLUES("Major Blues", "MajBlue", 0, 3, 4, 7, 9, 10), //
    MINOR_BLUES("Minor Blues", "MinBlue", 0, 3, 4, 6, 7, 10), //
    WHOLE_TONE("Whole", "Whole", 0, 2, 4, 6, 8, 10), //
    ARABIAN("Arabian", "Arab", 0, 2, 4, 5, 6, 8, 10), //
    EGYPTIAN("Egyptian", "Egypt", 0, 2, 5, 7, 10), //
    GYPSI("Gypsi", "Gypsi", 0, 2, 3, 6, 7, 8, 11), //
    SPANISH("Spanish", "Span", 0, 1, 3, 4, 5, 7, 8, 10);
    
    
    private final String name;
    private final int[] intervals;
    private final String shortName;
    
    Scale(final String name, final String shortName, final int... notes) {
        this.name = name;
        this.intervals = notes;
        this.shortName = shortName;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public int[] getIntervals() {
        return intervals;
    }
    
    public int toOffset(final int padIndex) {
        final int base = padIndex % intervals.length;
        final int mult = padIndex / intervals.length;
        return intervals[base] + mult * 12;
    }
    
    public int toOffsetOct(final int padIndex) {
        final int index = (intervals.length == 7 && padIndex > 7) ? padIndex - 1 : padIndex;
        final int base = index % intervals.length;
        return intervals[base] + (padIndex / intervals.length) * 12;
    }
    
    @Override
    public int getNextNote(final int startNote, final int baseNote, final int amount) {
        final int noteIndex = (startNote + 12 - baseNote) % 12;
        int octave = startNote < baseNote ? (startNote - baseNote - 12) / 12 : (startNote - baseNote) / 12;
        
        final int index = findScaleIndex(noteIndex, intervals);
        
        int nextIndex = index + amount;
        if (nextIndex >= intervals.length) {
            nextIndex = 0;
            octave++;
        } else if (nextIndex < 0) {
            nextIndex = intervals.length - 1;
            octave--;
        }
        return intervals[nextIndex] + baseNote + octave * 12;
    }
    
    private static int findScaleIndex(final int noteIndex, final int[] intervalls) {
        for (int i = 0; i < intervalls.length; i++) {
            if (intervalls[i] >= noteIndex) {
                return i;
            }
        }
        return -1;
    }
    
    @Override
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
