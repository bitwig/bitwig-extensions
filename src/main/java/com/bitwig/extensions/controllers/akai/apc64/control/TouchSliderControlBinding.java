package com.bitwig.extensions.controllers.akai.apc64.control;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.akai.apc64.layer.MainDisplay;
import com.bitwig.extensions.framework.Binding;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public class TouchSliderControlBinding extends Binding<AbsoluteHardwareControl, Parameter> {

    private final MainDisplay display;
    private final StringValue parameterOwner;
    private final int sliderIndex;
    private AbsoluteHardwareControlBinding hardwareBinding;
    private final Parameter parameter;
    private double downParameterValue;
    private final TouchSlider slider;
    private boolean active = false;
    private double sliderDownValue;
    private boolean stripTouched;
    private boolean fineModeActive = false;
    private boolean stripJustTouched = false;
    private boolean clearActive = false;

    public TouchSliderControlBinding(int sliderIndex, final TouchSlider source, final Parameter target,
                                     StringValue parameterOwner, BooleanValueObject fineModifierActive,
                                     BooleanValue clearModifier, MainDisplay display) {
        super(source, source.getFader(), target);
        this.sliderIndex = sliderIndex;
        this.parameter = target;
        this.slider = source;
        this.display = display;
        this.parameterOwner = parameterOwner;
        this.parameterOwner.markInterested();
        parameter.name().markInterested();
        fineModifierActive.addValueObserver(this::enableFineMode);
        clearModifier.addValueObserver(this::handleClearActive);
        slider.getTouchButton().isPressed().addValueObserver(this::handleStripTouched);
        source.getFader().value().addValueObserver(this::handleSliderValue);
        target.displayedValue().addValueObserver(this::handleParamChanged);
    }

    private void handleClearActive(boolean clearActive) {
        this.clearActive = clearActive;
        if (!active) {
            return;
        }
        if (clearActive) {
            deactivateValueBinding();
        } else {
            activate();
        }
    }

    private void handleParamChanged(String value) {
        if (active && stripTouched) {
            display.setParameterValue(value);
        }
    }

    private void handleStripTouched(boolean touched) {
        if (!active) {
            return;
        }
        if (clearActive) {
            if (touched) {
                parameter.restoreAutomationControl();
            }
        } else if (touched) {
            stripJustTouched = true;
            this.downParameterValue = parameter.value().get();
            if (active) {
                display.touchParameter(parameterOwner.get(), parameter.name().get(), parameter.displayedValue().get());
            }
        } else if (active && this.stripTouched) {
            display.releaseTouchParameter(sliderIndex);
        }
        this.stripTouched = touched;
    }

    private void handleSliderValue(double value) {
        if (stripJustTouched) {
            this.sliderDownValue = value;
            stripJustTouched = false;
        }
        if (!active) {
            return;
        }
        if (fineModeActive && stripTouched) {
            handleDelta(value);
        }
    }

    private void enableFineMode(boolean fineActive) {
        fineModeActive = fineActive;
        if (!active) {
            return;
        }
        if (fineActive) {
            if (stripTouched) {
                downParameterValue = parameter.getAsDouble();
            }
            deactivateValueBinding();
        } else {
            activate();
        }
    }

    private void handleDelta(double value) {
        if (!active) {
            return;
        }
        double delta = (value - sliderDownValue) * 0.25;
        double newValue = Math.max(0, Math.min(1, downParameterValue + delta));
        parameter.setImmediately(newValue);
    }

    private void deactivateValueBinding() {
        if (hardwareBinding != null) {
            hardwareBinding.removeBinding();
            hardwareBinding = null;
        }
    }

    @Override
    protected void activate() {
        active = true;
        hardwareBinding = addHardwareBinding();
    }

    @Override
    protected void deactivate() {
        active = false;
        if (hardwareBinding == null) {
            return;
        }
        hardwareBinding.removeBinding();
        hardwareBinding = null;
    }

    protected AbsoluteHardwareControlBinding addHardwareBinding() {
        return getSource().addBindingWithRange(getTarget(), 0, 1);
    }

}
