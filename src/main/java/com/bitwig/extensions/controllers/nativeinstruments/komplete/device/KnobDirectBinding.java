package com.bitwig.extensions.controllers.nativeinstruments.komplete.device;

import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.MidiProcessor;
import com.bitwig.extensions.framework.Binding;

public class KnobDirectBinding extends Binding<RelativeHardwareKnob, DirectSlot> {
    private final int index;
    private String name = "";
    private String valueText = "";
    private int paramValue;
    
    private final MidiProcessor midiProcessor;
    
    protected KnobDirectBinding(final int index, final RelativeHardwareKnob encoder, final DirectSlot slot,
        final MidiProcessor midiProcessor) {
        super(encoder, encoder, slot);
        this.index = index;
        this.midiProcessor = midiProcessor;
        slot.getParamName().addValueObserver(this::updateRemoteName);
        //parameter.exists().addValueObserver(this::updateRemoteExists);
        slot.getParamValue().addValueObserver(this::updateValueDisplay);
        slot.getValue().addValueObserver(this::updateValue);
    }
    
    private void updateValue(final int paramValue) {
        this.paramValue = paramValue;
        if (isActive()) {
            midiProcessor.updateParameterValue(index, paramValue);
        }
    }
    
    private void updateValueDisplay(final String valueText) {
        this.valueText = valueText;
        if (isActive()) {
            midiProcessor.sendParamValue(index, this.valueText);
        }
    }
    
    private void updateRemoteName(final String name) {
        this.name = name;
        updateDisplay();
    }
    
    private void updateDisplay() {
        if (!isActive()) {
            return;
        }
        if (getTarget().exists()) {
            midiProcessor.sendRemoteState(index, getType(), this.name);
        } else {
            midiProcessor.sendRemoteState(index, 0, "");
        }
    }
    
    
    private int getType() {
        return 0;
    }
    
    @Override
    protected void deactivate() {
    }
    
    @Override
    protected void activate() {
        updateDisplay();
        if (getTarget().exists()) {
            midiProcessor.updateParameterValue(index, paramValue);
            midiProcessor.sendParamValue(index, valueText);
        } else {
            midiProcessor.updateParameterValue(index, 0);
            midiProcessor.sendParamValue(index, "");
        }
    }
}
