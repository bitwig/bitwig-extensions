package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import java.util.List;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkHwElements;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkViewControl;
import com.bitwig.extensions.controllers.akai.mpkmk4.bindings.ParameterDisplayBinding;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.Encoder;
import com.bitwig.extensions.framework.Layers;

public class CursorTrackMixLayer extends EncoderLayer {
    
    public CursorTrackMixLayer(final Layers layers, final MpkHwElements hwElements,
        final MpkMidiProcessor midiProcessor, final MpkViewControl viewControl) {
        super("CURSOR_MIXER", layers, midiProcessor);
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        final List<Encoder> encoders = hwElements.getEncoders();
        
        final Encoder panEncoder = encoders.get(0);
        panEncoder.bindValue(this, cursorTrack.pan());
        this.addBinding(new ParameterDisplayBinding(cursorTrack.pan(), panEncoder, parameterValues, 0));
        final Encoder volumenEncoder = encoders.get(4);
        volumenEncoder.bindValue(this, cursorTrack.volume());
        this.addBinding(new ParameterDisplayBinding(cursorTrack.volume(), volumenEncoder, parameterValues, 4));
        for (int i = 0; i < 6; i++) {
            final Send send = cursorTrack.sendBank().getItemAt(i);
            final int index = i + (i / 3 + 1);
            final Encoder encoder = encoders.get(index);
            encoder.bindValue(this, send);
            this.addBinding(new ParameterDisplayBinding(send, encoder, parameterValues, index));
        }
    }
    
    @Override
    public void navigateLeft() {
    
    }
    
    @Override
    public void navigateRight() {
    
    }
    
    @Override
    public boolean canScrollRight() {
        return false;
    }
    
    @Override
    public boolean canScrollLeft() {
        return false;
    }
}
