package com.bitwig.extensions.controllers.novation.slmk3.value;

public interface ScaleInterface {
    
    String getName();
    
    String getShortName();
    
    int[] getIntervals();
    
    int toOffset(int padIndex);
    
    int toOffsetOct(final int padIndex);
    
    int getNextNote(final int startNote, final int baseNote, final int amount);
}
