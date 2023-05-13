package com.bitwig.extensions.controllers.novation.launchpadpromk3.sliderlayers;

import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.controllers.novation.commonsmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.commonsmk3.SliderBinding;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.SysExHandler;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.layers.ControlMode;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

import java.util.ArrayList;
import java.util.List;

public abstract class SliderLayer extends Layer {
    public static final int BASE_CHANNEL = 4;

    protected final HardwareSlider[] sliders = new HardwareSlider[8];
    protected final ControlMode mode;
    protected final SysExHandler modeHandler;
    protected final MidiProcessor midiProcessor;
    protected final List<SliderBinding> valueBindings = new ArrayList<>();
    protected int[] tracksExistsColors = new int[8];

    public SliderLayer(final String name, final ControlMode mode, final HardwareSurface controlSurface,
                       final Layers layers, final MidiProcessor midiProcessor, final SysExHandler sysExHandler) {
        super(layers, name);
        this.mode = mode;
        this.midiProcessor = midiProcessor;
        modeHandler = sysExHandler;

        initSliders(name, mode, controlSurface);
    }

    private void initSliders(final String name, final ControlMode mode, final HardwareSurface surface) {
        final MidiIn midiIn = midiProcessor.getMidiIn();
        for (int i = 0; i < 8; i++) {
            sliders[i] = surface.createHardwareSlider(name + "_SLIDER_" + (i + 1));
            sliders[i].setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(BASE_CHANNEL, mode.getCcNr() + i));
        }
    }

    protected abstract void refreshTrackColors();


}
