package com.bitwig.extensions.controllers.arturia.keylab.mk3.controls;

import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.MidiProcessor;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.color.RgbColor;
import com.bitwig.extensions.framework.Layer;

public class TouchSlider extends TouchControl {
    private final HardwareSlider slider;
    
    public TouchSlider(final int id, final int ccNr, final int ccTouchNr, final HardwareSurface surface,
        final MidiProcessor midiProcessor) {
        super(surface.createHardwareButton("SLIDER_TOUCH_%d".formatted(id + 1)), id, ccTouchNr, midiProcessor);
        final MidiIn midiIn = midiProcessor.getMidiIn();
        slider = surface.createHardwareSlider("SLIDER_%d".formatted(id + 1));
        slider.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, ccNr));
    }
    
    public void bindParameter(final Layer layer, final Parameter parameter, final StringValue labelValue) {
        layer.bind(slider, parameter);
        layer.bind(hwButton, hwButton.pressedAction(), () -> parameter.touch(true));
        layer.bind(hwButton, hwButton.releasedAction(), () -> parameter.touch(false));
        layer.addBinding(new TouchDisplayBinding(this, parameter, labelValue));
    }
    
    public void updateDisplay(final String parameterName, final String value, final int intValue) {
        midiProcessor.popup(1, value, parameterName, intValue, RgbColor.WIDGET);
    }
    
    
}
