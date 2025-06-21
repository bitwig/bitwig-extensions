package com.bitwig.extensions.controllers.novation.slmk3.bindings;

import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.novation.slmk3.MidiProcessor;
import com.bitwig.extensions.framework.Binding;

public class SliderNotificationBinding extends Binding<HardwareSlider, MidiProcessor> {
    
    private String name;
    private String value;
    private long lastSliderUpdate = -1;
    
    public SliderNotificationBinding(final HardwareSlider slider, final MidiProcessor midiProcessor,
        final Parameter parameter, final StringValue nameSource) {
        super(slider, slider, midiProcessor);
        slider.value().addValueObserver(v -> this.handleSliderChanged(v));
        parameter.displayedValue().addValueObserver(this::handleValueChanged);
        nameSource.addValueObserver(value -> this.name = value);
    }
    
    private void handleSliderChanged(final double v) {
        if (isActive()) {
            lastSliderUpdate = System.currentTimeMillis();
            update();
        }
    }
    
    private void handleValueChanged(final String paramValue) {
        this.value = paramValue;
        if (isActive() && (System.currentTimeMillis()) - lastSliderUpdate < 50) {
            update();
        }
    }
    
    private void update() {
        getTarget().sendNotification(name, value);
    }
    
    @Override
    protected void deactivate() {
    }
    
    @Override
    protected void activate() {
        // getTarget().sendValue(lastValue);
    }
    
}
