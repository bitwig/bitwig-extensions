package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import java.util.List;

import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkHwElements;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkViewControl;
import com.bitwig.extensions.controllers.akai.mpkmk4.bindings.ParameterDisplayBinding;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.Encoder;
import com.bitwig.extensions.framework.Layers;

public class MixEncoderLayer extends EncoderLayer {
    
    public MixEncoderLayer(final Layers layers, final MpkHwElements hwElements, final MpkMidiProcessor midiProcessor,
        final MpkViewControl viewControl) {
        super("GRID_MIXER", layers, midiProcessor);
        final TrackBank trackBank = viewControl.getTrackBank();
        final List<Encoder> encoders = hwElements.getEncoders();
        
        for (int i = 0; i < 4; i++) {
            final Track track = trackBank.getItemAt(i);
            final Encoder encoderPan = encoders.get(i);
            encoderPan.bindValue(this, track.pan());
            this.addBinding(new ParameterDisplayBinding(track.pan(), encoderPan, parameterValues, i));
            final Encoder encoderVolume = encoders.get(i + 4);
            encoderVolume.bindValue(this, track.volume());
            this.addBinding(new ParameterDisplayBinding(track.volume(), encoderVolume, parameterValues, i + 4));
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
