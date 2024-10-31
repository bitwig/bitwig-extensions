package com.bitwig.extensions.controllers.novation.launchkey_mk4.display;

import com.bitwig.extension.controller.api.StringValue;

public class TempDisplayValue {
    
    private final DisplayControl control;
    private String value;
    private final String title;
    private boolean expectUpdate = false;
    
    public TempDisplayValue(final DisplayControl control, final String title, final StringValue value) {
        this.control = control;
        this.title = title;
        value.addValueObserver(this::handleValueChanged);
    }
    
    private void handleValueChanged(final String stringValue) {
        this.value = stringValue;
        if (this.expectUpdate) {
            control.show2Line(title, value);
        }
    }
    
    public void notifyUpdate() {
        this.expectUpdate = true;
    }
    
}
