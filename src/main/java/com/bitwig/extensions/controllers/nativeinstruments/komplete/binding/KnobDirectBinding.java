package com.bitwig.extensions.controllers.nativeinstruments.komplete.binding;

import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.device.DirectSlot;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.MidiProcessor;
import com.bitwig.extensions.framework.Binding;

public class KnobDirectBinding extends Binding<RelativeHardwareKnob, DirectSlot> {
    private final int index;
    private String name = "";
    private String valueText = "";
    private double paramValue;
    
    private final MidiProcessor midiProcessor;
    
    public KnobDirectBinding(final int index, final RelativeHardwareKnob encoder, final DirectSlot slot,
        final MidiProcessor midiProcessor) {
        super(encoder, encoder, slot);
        this.index = index;
        this.midiProcessor = midiProcessor;
        slot.getParamName().addValueObserver(this::updateRemoteName);
        slot.getParamValue().addValueObserver(this::updateValueDisplay);
        slot.addValueObserver(this::updateValue);
    }
    
    private void updateValue(final double paramValue) {
        this.paramValue = paramValue;
        if (isActive()) {
            midiProcessor.updateParameterValue(index, (int) Math.round(paramValue * 127));
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
            midiProcessor.updateParameterValue(index, (int) Math.round(paramValue * 127));
            midiProcessor.sendParamValue(index, valueText);
        } else {
            midiProcessor.updateParameterValue(index, 0);
            midiProcessor.sendParamValue(index, "");
        }
    }
}
