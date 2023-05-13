package com.bitwig.extensions.controllers.novation.launchpadmini3.layers;

import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.controllers.novation.commonsmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.commonsmk3.SliderBinding;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSliderLayer extends Layer {
    public static final int BASE_CHANNEL = 4;
    protected final HardwareSlider[] sliders = new HardwareSlider[8];
    protected final MidiProcessor midiProcessor;
    protected final List<SliderBinding> valueBindings = new ArrayList<>();
    protected final int baseCcNr;
    protected final int baseColor;
    protected int[] tracksExistsColors = new int[8];

    public AbstractSliderLayer(final String name, final HardwareSurface controlSurface, final Layers layers,
                               final MidiProcessor midiProcessor, final int baseCcNr, final int baseColor) {
        super(layers, name);
        this.midiProcessor = midiProcessor;
        this.baseCcNr = baseCcNr;
        this.baseColor = baseColor;
        initSliders(name, controlSurface);
    }

    private void initSliders(final String name, final HardwareSurface surface) {
        final MidiIn midiIn = midiProcessor.getMidiIn();
        for (int i = 0; i < 8; i++) {
            sliders[i] = surface.createHardwareSlider(name + "_SLIDER_" + (i + 1));
            sliders[i].setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(BASE_CHANNEL, baseCcNr + i));
        }
    }

    protected abstract void refreshTrackColors();

    public boolean canBeEntered(final ControlMode mode) {
        return true;
    }

}
