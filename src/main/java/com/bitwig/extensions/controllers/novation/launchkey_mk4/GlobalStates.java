package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BooleanValueObject;

@Component
public class GlobalStates {
    public static final String ARRANGE_MODE = "ARRANGE";
    
    private final BooleanValueObject shiftState = new BooleanValueObject();
    private String panelLayout = ARRANGE_MODE;
    
    public GlobalStates(final Application application) {
        application.panelLayout().addValueObserver(layout -> {
            this.panelLayout = layout;
        });
    }
    
    public BooleanValueObject getShiftState() {
        return shiftState;
    }
    
    public boolean isArrangeMode() {
        return ARRANGE_MODE.equals(this.panelLayout);
    }
}
