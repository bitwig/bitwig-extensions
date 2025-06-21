package com.bitwig.extensions.controllers.reloop;

import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.framework.Layer;

public class StepEncoder {
    private final HardwareButton encoder;
    
    public StepEncoder(final HardwareSurface surface, final MidiProcessor midiProcessor, final int channel,
        final int ccNr) {
        final MidiIn midiIn = midiProcessor.getMidiIn();
        encoder = surface.createHardwareButton("ENCODER_" + ccNr);
        encoder.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccNr, 127));
        encoder.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccNr, 1));
        
        //shiftButton = surface.createHardwareButton("SHIFT_" + ccNr);
    }
    
    public void bindEncoder(final Layer layer, final IntConsumer intConsumer) {
        layer.bind(encoder, encoder.pressedAction(), () -> intConsumer.accept(1));
        layer.bind(encoder, encoder.releasedAction(), () -> intConsumer.accept(-1));
    }
}
