package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.MidiProcessor;

public class DeviceControl {
    
    private final MidiProcessor midiProcessor;
    
    public DeviceControl(final HardwareSurface surface, final ControllerHost host, final MidiProcessor deviceControl) {
        this.midiProcessor = deviceControl;
    }
}
