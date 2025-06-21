package com.bitwig.extensions.controllers.novation.slmk3.seqcommons;

import com.bitwig.extension.controller.api.BeatTimeFormatter;
import com.bitwig.extensions.controllers.novation.slmk3.value.IntValue;
import com.bitwig.extensions.controllers.novation.slmk3.value.ValueSet;

public interface SequencerSource {
    boolean isActive();
    
    ValueSet getGridResolution();
    
    int getHeldIndex();
    
    BeatTimeFormatter getBeatTimeFormatter();
    
    IntValue getMonoKeyNoteFocus();
}
