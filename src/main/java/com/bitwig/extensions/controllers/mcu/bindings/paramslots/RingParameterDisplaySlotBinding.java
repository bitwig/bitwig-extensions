package com.bitwig.extensions.controllers.mcu.bindings.paramslots;

import com.bitwig.extensions.controllers.mcu.bindings.ResetableBinding;
import com.bitwig.extensions.controllers.mcu.bindings.RingDisplayBinding;
import com.bitwig.extensions.controllers.mcu.control.RingDisplay;
import com.bitwig.extensions.controllers.mcu.control.RingDisplayType;
import com.bitwig.extensions.controllers.mcu.devices.ParamPageSlot;

/**
 * Special binding for the encoder ring display, that also responds to being
 * enabled or not.
 */
public class RingParameterDisplaySlotBinding extends RingDisplayBinding<ParamPageSlot> implements ResetableBinding {
    
    private int lastValue;
    private boolean lastEnableValue = false;
    private boolean exists = false;
    
    public RingParameterDisplaySlotBinding(final ParamPageSlot source, final RingDisplay target) {
        super(target, source, RingDisplayType.FILL_LR_0);
        source.getRingValue().addValueObserver(newValue -> {
            valueChange(source.getRingDisplayType().getOffset() + newValue);
        });
        source.getExistsValue().addValueObserver(exists -> {
            this.exists = exists;
            update();
        });
        source.getEnabledValue().addValueObserver(this::handleEnabled);
        lastValue = source.getRingDisplayType().getOffset() + source.getRingValue().get();
    }
    
    private void handleExists(final boolean exists) {
        this.exists = exists;
        if (isActive()) {
            update();
        }
    }
    
    private void handleEnabled(final boolean enableValue) {
        lastEnableValue = enableValue;
        if (isActive()) {
            update();
        }
    }
    
    @Override
    public void reset() {
        update();
    }
    
    public void update() {
        if (isActive()) {
            lastValue = getSource().getRingValue().get() + getSource().getRingDisplayType().getOffset();
            final int value = (lastEnableValue && exists) ? lastValue : 0;
            getTarget().sendValue(value, false);
        }
    }
    
    private void valueChange(final int value) {
        lastValue = value;
        if (isActive()) {
            final int newValue = (lastEnableValue && exists) ? lastValue : 0;
            getTarget().sendValue(newValue, false);
        }
    }
    
    @Override
    protected void activate() {
        lastValue =
            (exists && lastEnableValue) ? getSource().getRingDisplayType().getOffset() + getSource().getRingValue()
                .get() : 0;
        getTarget().sendValue(lastValue, false);
    }
    
    @Override
    protected int calcValue() {
        return 0;
    }
    
}
