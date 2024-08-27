package com.bitwig.extensions.controllers.arturia.keylab.mk3.controls;

import java.util.function.Consumer;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.MidiProcessor;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.color.RgbColor;
import com.bitwig.extensions.framework.Layer;

public abstract class TouchControl {
    protected final MidiProcessor midiProcessor;
    protected final HardwareButton hwButton;
    private final int id;
    
    public TouchControl(final HardwareButton button, final int id, final int ccTouchNr,
        final MidiProcessor midiProcessor) {
        this.midiProcessor = midiProcessor;
        this.hwButton = button;
        this.id = id;
        hwButton.isPressed().markInterested();
        hwButton.pressedAction().setActionMatcher(midiProcessor.getMidiIn().createCCActionMatcher(0, ccTouchNr, 127));
        hwButton.releasedAction().setActionMatcher(midiProcessor.getMidiIn().createCCActionMatcher(0, ccTouchNr, 0));
    }
    
    public BooleanValue isTouched() {
        return hwButton.isPressed();
    }
    
    public void bindPressed(final Layer layer, final Runnable action) {
        layer.bind(hwButton, hwButton.pressedAction(), action);
    }
    
    public void bindReleased(final Layer layer, final Runnable action) {
        layer.bind(hwButton, hwButton.releasedAction(), action);
    }
    
    public void bindIsPressed(final Layer layer, final Consumer<Boolean> action) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> action.accept(true));
        layer.bind(hwButton, hwButton.releasedAction(), () -> action.accept(false));
    }
    
    public void updateDisplay(final String parameterName, final String value, final int intValue) {
        midiProcessor.popup(0, value, parameterName, intValue, RgbColor.WIDGET);
    }
    
    public void setParameterControl(final boolean exists) {
        midiProcessor.submitControl(id, exists);
    }
}
