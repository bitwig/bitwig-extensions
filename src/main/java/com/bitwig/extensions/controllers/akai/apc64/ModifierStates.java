package com.bitwig.extensions.controllers.akai.apc64;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BooleanValueObject;

@Component
public class ModifierStates {

    public static final int MSK_SHIFT = 0x1;
    public static final int MSK_CLEAR = 0x2;

    private final BooleanValueObject shiftActive = new BooleanValueObject();
    private final BooleanValueObject clearActive = new BooleanValueObject();
    private final BooleanValueObject duplicateActive = new BooleanValueObject();
    private final SettableBooleanValue quantizeActive = new BooleanValueObject();
    private final BooleanValueObject altActive = new BooleanValueObject();

    private int modifierMask = 0;

    public ModifierStates(final ControllerHost host) {
        shiftActive.addValueObserver(active -> setMask(MSK_SHIFT, active));
        clearActive.addValueObserver(active -> setMask(MSK_CLEAR, active));
    }

    private void setMask(final int mask, final boolean value) {
        if (value) {
            modifierMask |= mask;
        } else {
            modifierMask &= ~mask;
        }
    }

    public void setShift(final boolean active) {
        shiftActive.set(active);
    }

    public void setClear(final boolean active) {
        clearActive.set(active);
    }

    public BooleanValueObject getShiftActive() {
        return shiftActive;
    }

    public BooleanValueObject getClearActive() {
        return clearActive;
    }

    public boolean isShift() {
        return shiftActive.get();
    }

    public boolean isClear() {
        return clearActive.get();
    }

    public boolean anyModifierHeld() {
        return modifierMask > 0;
    }

    public boolean noModifier() {
        return modifierMask == 0;
    }

    public boolean onlyShift() {
        return modifierMask == MSK_SHIFT;
    }

    public void setDuplicate(final boolean active) {
        duplicateActive.set(active);
    }

    public boolean isDuplicate() {
        return duplicateActive.get();
    }

    public SettableBooleanValue getQuantizeActive() {
        return quantizeActive;
    }

    public BooleanValueObject getAltActive() {
        return altActive;
    }
}
