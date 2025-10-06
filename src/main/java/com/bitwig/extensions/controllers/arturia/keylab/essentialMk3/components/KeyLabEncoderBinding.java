package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components;

import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extension.controller.api.AbsoluteHardwareControlBinding;
import com.bitwig.extension.controller.api.HardwareBinding;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.display.LcdDisplay;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.display.LcdDisplayMode;
import com.bitwig.extensions.framework.Binding;

public class KeyLabEncoderBinding extends Binding<AbsoluteHardwareControl, Parameter> {
    private final LcdDisplay display;
    private HardwareBinding hwBinding;
    private final KeylabAbsoluteControl control;
    private int currentValue = 0;
    private boolean targetExists = false;
    
    public KeyLabEncoderBinding(final KeylabAbsoluteControl control, final Parameter target,
        final StringValue nameSource, final LcdDisplayMode mode, final LcdDisplay display) {
        super(control.getControl(), control.getControl(), target);
        this.control = control;
        this.display = display;
        target.displayedValue().markInterested();
        target.name().markInterested();
        target.value().markInterested();
        nameSource.markInterested();
        final int index = control.getIndex() + 1;
        getSource().value().addValueObserver(val -> handleControlValueChanged(mode, val));
        
        target.exists().addValueObserver(exists -> {
            targetExists = exists;
            if (isActive()) {
                control.forceValue(exists);
            }
        });
        
        target.value().addValueObserver(
            128, v -> {
                currentValue = v;
                if (!isActive()) {
                    return;
                }
                display.sendValueText(
                    index, control.getValueType(), mode, nameSource.get(), target.displayedValue().get(), v);
                control.updateValue(currentValue);
            });
    }
    
    private void handleControlValueChanged(final LcdDisplayMode mode, final double value) {
        if (!isActive()) {
            return;
        }
        final int index = control.getIndex() + 1;
        display.enableValues(index, mode);
    }
    
    
    @Override
    protected void activate() {
        assert hwBinding == null;
        hwBinding = getHardwareBinding();
        control.updateValue(currentValue);
        control.forceValue(targetExists);
    }
    
    @Override
    protected void deactivate() {
        assert hwBinding != null;
        
        hwBinding.removeBinding();
        hwBinding = null;
    }
    
    protected AbsoluteHardwareControlBinding getHardwareBinding() {
        return getTarget().addBinding(getSource());
    }
    
}
