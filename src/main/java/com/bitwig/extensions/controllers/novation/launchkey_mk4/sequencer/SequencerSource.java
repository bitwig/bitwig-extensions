package com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer;

import com.bitwig.extension.controller.api.BeatTimeFormatter;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.values.IntValue;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.values.ValueSet;

public interface SequencerSource {
    boolean isActive();
    
    ValueSet getGridResolution();
    
    BeatTimeFormatter getBeatTimeFormatter();
    
    IntValue getMonoKeyNoteFocus();
}
