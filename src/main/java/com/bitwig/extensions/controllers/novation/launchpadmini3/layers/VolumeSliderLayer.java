package com.bitwig.extensions.controllers.novation.launchpadmini3.layers;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.novation.commonsmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.commonsmk3.PanelLayout;
import com.bitwig.extensions.controllers.novation.commonsmk3.SliderBinding;
import com.bitwig.extensions.controllers.novation.commonsmk3.ViewCursorControl;
import com.bitwig.extensions.controllers.novation.launchpadmini3.LaunchPadPreferences;
import com.bitwig.extensions.framework.Layers;

public class VolumeSliderLayer extends TrackSliderLayer {
    
    public VolumeSliderLayer(final ViewCursorControl viewCursorControl, final HardwareSurface controlSurface,
        final Layers layers, final MidiProcessor midiProcessor, final LaunchPadPreferences preferences) {
        super("VOL", controlSurface, layers, midiProcessor, 20, 9);
        bind(viewCursorControl.getTrackBank());
        preferences.getPanelLayout().addValueObserver(((oldValue, newValue) -> {
            layout = newValue;
            updateFaderState();
        }));
    }
    
    @Override
    protected void bind(final TrackBank trackBank) {
        for (int i = 0; i < 8; i++) {
            final Track track = trackBank.getItemAt(i);
            final SliderBinding binding = new SliderBinding(baseCcNr, track.volume(), sliders[i], i, midiProcessor);
            addBinding(binding);
            valueBindings.add(binding);
        }
    }
    
    @Override
    protected void updateFaderState() {
        if (isActive()) {
            refreshTrackColors();
            midiProcessor.setFaderBank(layout == PanelLayout.VERTICAL ? 0 : 1, tracksExistsColors, true, baseCcNr);
            valueBindings.forEach(SliderBinding::update);
        }
    }
    
    
    @Override
    protected void refreshTrackColors() {
        final boolean[] exists = trackState.getExists();
        for (int i = 0; i < 8; i++) {
            tracksExistsColors[i] = exists[i] ? baseColor : 0;
        }
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        updateFaderState();
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
        refreshTrackColors();
        updateFaderState();
    }
    
}
