package com.bitwig.extensions.controllers.novation.slmk3.display;

import com.bitwig.extensions.controllers.novation.slmk3.MidiProcessor;

public interface ScreenConfigSource {
    boolean isActive();
    
    MidiProcessor getMidiProcessor();
}
