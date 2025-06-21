package com.bitwig.extensions.controllers.arturia.keylab.mk3.controls;


import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.framework.Binding;

public class TouchDisplayBinding extends Binding<TouchControl, Parameter> {

    private String lastValue = "";
    private String labelValue = "";
    private boolean touched = false;
    private int value = 0;
    private boolean parameterExists = false;

    public TouchDisplayBinding(final TouchControl encoder, final Parameter target, final StringValue label) {
        super(target, encoder, target);
        target.displayedValue().addValueObserver(this::handleValueChanged);
        target.value().addValueObserver(128, this::handleIntValueChanged);
        target.exists().addValueObserver(this::handleParameterExists);
        label.addValueObserver(this::handleLabelChanged);
        encoder.isTouched().addValueObserver(this::handleTouched);
    }

    private void handleIntValueChanged(final int value) {
        this.value = value;
        if (isActive() && touched && parameterExists) {
            updateDisplay();
        }
    }

    private void handleParameterExists(final boolean exists) {
        this.parameterExists = exists;
        if (isActive()) {
            getSource().setParameterControl(parameterExists);
        }
    }

    private void handleValueChanged(final String newValue) {
        this.lastValue = newValue;
        if (isActive() && touched && parameterExists) {
            updateDisplay();
        }
    }

    private void handleLabelChanged(final String newValue) {
        this.labelValue = newValue;
        if (isActive() && touched && parameterExists) {
            updateDisplay();
        }
    }

    private void handleTouched(final boolean touched) {
        this.touched = touched;
        if (isActive() && touched && parameterExists) {
            updateDisplay();
        }
    }

    private void updateDisplay() {
        getSource().updateDisplay(labelValue, lastValue, value);
    }

    @Override
    protected void deactivate() {

    }

    @Override
    protected void activate() {
        getSource().setParameterControl(parameterExists);
    }

}
