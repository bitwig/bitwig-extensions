package com.bitwig.extensions.controllers.novation.launchpadmini3.layers;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.novation.commonsmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.commonsmk3.SliderBinding;
import com.bitwig.extensions.controllers.novation.commonsmk3.ViewCursorControl;
import com.bitwig.extensions.controllers.novation.launchpadmini3.LpMiniHwElements;
import com.bitwig.extensions.framework.Layers;

public class PanSliderLayer extends TrackSliderLayer {
    
    public PanSliderLayer(final ViewCursorControl viewCursorControl, final HardwareSurface controlSurface,
        final Layers layers, final MidiProcessor midiProcessor, final LpMiniHwElements hwElements) {
        super("PAN", controlSurface, layers, midiProcessor, 20, -1);
        bind(viewCursorControl.getTrackBank());
    }
    
    @Override
    protected void bind(final TrackBank trackBank) {
        for (int i = 0; i < 8; i++) {
            final Track track = trackBank.getItemAt(i);
            final SliderBinding binding = new SliderBinding(baseCcNr, track.pan(), sliders[i], i, midiProcessor);
            addBinding(binding);
            valueBindings.add(binding);
        }
    }
    
    @Override
    protected void refreshTrackColors() {
        final boolean[] exists = trackState.getExists();
        final int[] colorIndex = trackState.getColorIndex();
        
        for (int i = 0; i < 8; i++) {
            if (exists[i]) {
                tracksExistsColors[i] = colorIndex[i];
            } else {
                tracksExistsColors[i] = 0;
            }
        }
    }
    
    @Override
    protected void updateFaderState() {
        if (isActive()) {
            refreshTrackColors();
            midiProcessor.setFaderBank(1, tracksExistsColors, false, baseCcNr);
            valueBindings.forEach(SliderBinding::update);
        }
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
        refreshTrackColors();
        updateFaderState();
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        updateFaderState();
    }
    
}
