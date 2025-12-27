package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import java.util.Optional;

import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.ParameterValues;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.RemotesDisplayControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public abstract class EncoderLayer extends Layer {
    final protected MpkMidiProcessor midiProcessor;
    protected final ParameterValues parameterValues;
    
    public EncoderLayer(final String name, final Layers layers, final MpkMidiProcessor midiProcessor) {
        super(layers, name);
        this.midiProcessor = midiProcessor;
        this.parameterValues = new ParameterValues(midiProcessor);
    }
    
    public Optional<RemotesDisplayControl> getDisplayControl() {
        return Optional.empty();
    }
    
    public abstract void navigateLeft();
    
    public abstract void navigateRight();
    
    public abstract boolean canScrollRight();
    
    public abstract boolean canScrollLeft();
    
    @Override
    protected void onActivate() {
        super.onActivate();
    }
    
}
