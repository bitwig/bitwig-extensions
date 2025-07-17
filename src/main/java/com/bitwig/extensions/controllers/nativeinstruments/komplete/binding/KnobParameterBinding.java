package com.bitwig.extensions.controllers.nativeinstruments.komplete.binding;

import com.bitwig.extension.controller.api.HardwareBinding;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwareControlBinding;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.MidiProcessor;
import com.bitwig.extensions.framework.Binding;

public class KnobParameterBinding extends Binding<RelativeHardwareKnob, SettableRangedValue> {
    public static final double BASE_SENSITIVITY = 0.18;
    public static final double FINE_SENSITIVITY = BASE_SENSITIVITY / 4.0;
    
    private final int index;
    private String name = "";
    private String valueText = "";
    private boolean fineTune;
    private double origin;
    private int discreteSteps;
    private int paramValue;
    private boolean onPlugin;
    private double sensitivity = BASE_SENSITIVITY;
    
    private HardwareBinding hwBinding;
    private final MidiProcessor midiProcessor;
    
    public KnobParameterBinding(final int index, final RelativeHardwareKnob encoder, final Parameter parameter,
        final MidiProcessor midiProcessor) {
        super(encoder, encoder, parameter);
        this.index = index;
        this.midiProcessor = midiProcessor;
        parameter.name().addValueObserver(this::updateRemoteName);
        parameter.exists().addValueObserver(this::updateRemoteExists);
        parameter.displayedValue().addValueObserver(this::updateValueDisplay);
        parameter.discreteValueCount().addValueObserver(this::updateDiscreteSteps);
        parameter.getOrigin().addValueObserver(this::updateOriginValue);
        parameter.value().addValueObserver(128, this::updateValue);
    }
    
    public void setOnPlugin(final boolean onPlugin) {
        this.onPlugin = onPlugin;
        updateSensitivity();
    }
    
    private void updateValue(final int paramValue) {
        this.paramValue = paramValue;
        if (isActive()) {
            midiProcessor.updateParameterValue(index, paramValue);
        }
    }
    
    private void updateOriginValue(final double v) {
        this.origin = v;
        updateDisplay();
    }
    
    private void updateDiscreteSteps(final int steps) {
        this.discreteSteps = steps;
        updateSensitivity();
        updateDisplay();
    }
    
    private void updateValueDisplay(final String s) {
        this.valueText = s;
        if (isActive()) {
            midiProcessor.sendParamValue(index, valueText);
        }
    }
    
    private void updateRemoteName(final String name) {
        this.name = name;
        updateDisplay();
    }
    
    private void updateRemoteExists(final boolean exists) {
        updateDisplay();
    }
    
    private void updateDisplay() {
        if (!isActive()) {
            return;
        }
        midiProcessor.sendRemoteState(index, getType(), this.name);
    }
    
    protected RelativeHardwareControlBinding getHardwareBinding(final double sensitivity) {
        return getTarget().addBindingWithSensitivity(getSource(), sensitivity);
        //return null;
    }
    
    private int getType() {
        if (origin == 0.5) {
            return 1;
        }
        if (discreteSteps == 2) {
            return 2;
        }
        if (discreteSteps > 2) {
            return 3;
        }
        return 0;
    }
    
    private void updateSensitivity() {
        final double newSensitivity = calcSensitivity();
        if (newSensitivity != this.sensitivity) {
            this.sensitivity = newSensitivity;
            if (isActive() && hwBinding != null) {
                hwBinding.removeBinding();
                //KompleteKontrolExtension.println(" SEN %d %f", index, sensitivity);
                hwBinding = getHardwareBinding(sensitivity);
            }
        }
    }
    
    private double calcSensitivity() {
        if (discreteSteps < 1 || onPlugin) {
            return fineTune ? FINE_SENSITIVITY : BASE_SENSITIVITY;
        }
        return fineTune ? 1.0 : Math.max(1, discreteSteps / 4);
    }
    
    public void setFineTune(final boolean fineTune) {
        this.fineTune = fineTune;
        if (isActive()) {
            updateSensitivity();
        }
    }
    
    @Override
    protected void deactivate() {
        if (hwBinding != null && isActive()) {
            hwBinding.removeBinding();
        }
    }
    
    @Override
    protected void activate() {
        if (hwBinding != null) {
            hwBinding.removeBinding();
        }
        //KompleteKontrolExtension.println(" SEN %d %f", index, sensitivity);
        hwBinding = getHardwareBinding(sensitivity);
        updateDisplay();
        midiProcessor.updateParameterValue(index, paramValue);
        midiProcessor.sendParamValue(index, valueText);
    }
}
