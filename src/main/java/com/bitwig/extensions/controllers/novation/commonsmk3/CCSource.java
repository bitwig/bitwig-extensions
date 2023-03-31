package com.bitwig.extensions.controllers.novation.commonsmk3;

import com.bitwig.extension.controller.api.HardwareActionMatcher;
import com.bitwig.extension.controller.api.MidiIn;

public interface CCSource {
    int getCcValue();

    HardwareActionMatcher createMatcher(final MidiIn midiIn, final int matchValue);
}
