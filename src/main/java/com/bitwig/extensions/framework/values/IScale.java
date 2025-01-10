package com.bitwig.extensions.framework.values;

public interface IScale {
    String getName();
    
    int[] getIntervals();
    
    boolean inScale(int noteBase);
    
    IScale[] getValues();
    
    static int findScaleIndex(final int noteIndex, final int[] intervalls) {
        for (int i = 0; i < intervalls.length; i++) {
            if (intervalls[i] >= noteIndex) {
                return i;
            }
        }
        return -1;
    }
    
    default int getNextNote(final int startNote, final int baseNote, final int amount) {
        final int noteIndex = (startNote + 12 - baseNote) % 12;
        int octave = startNote < baseNote ? (startNote - baseNote - 12) / 12 : (startNote - baseNote) / 12;
        
        final int[] intervals = getIntervals();
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
    
    default int nextInScale(int index) {
        while (true) {
            final int numberOfScales = getValues().length;
            if (!(index < numberOfScales)) {
                break;
            }
            if (inScale(index)) {
                return index;
            }
            index++;
        }
        return index;
    }
    
    
    int highestNote(int startNote, int noteRange);
}
