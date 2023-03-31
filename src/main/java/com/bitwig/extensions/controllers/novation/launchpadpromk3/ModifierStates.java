package com.bitwig.extensions.controllers.novation.launchpadpromk3;

import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BooleanValueObject;

@Component
public class ModifierStates {

    public static final int MSK_SHIFT = 0x1;
    public static final int MSK_CLEAR = 0x2;
    public static final int MSK_DUP = 0x4;
    public static final int MSK_QUANTIZE = 0x8;

    private final BooleanValueObject shiftActive = new BooleanValueObject();
    private final BooleanValueObject clearActive = new BooleanValueObject();
    private final BooleanValueObject duplicateActive = new BooleanValueObject();
    private final BooleanValueObject quantizeActive = new BooleanValueObject();

    private int modifierMask = 0;

    public ModifierStates() {
        shiftActive.addValueObserver(active -> setMask(MSK_SHIFT, active));
        clearActive.addValueObserver(active -> setMask(MSK_CLEAR, active));
        duplicateActive.addValueObserver(active -> setMask(MSK_DUP, active));
        quantizeActive.addValueObserver(active -> setMask(MSK_QUANTIZE, active));
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

    public void setDuplicate(final boolean active) {
        duplicateActive.set(active);
    }

    public void setQuantize(final boolean active) {
        quantizeActive.set(active);
    }

    public BooleanValueObject getShiftActive() {
        return shiftActive;
    }


    public boolean isShift() {
        return shiftActive.get();
    }

    public boolean isClear() {
        return clearActive.get();
    }

    public boolean isDuplicate() {
        return duplicateActive.get();
    }

    public boolean isQuantize() {
        return quantizeActive.get();
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
}
