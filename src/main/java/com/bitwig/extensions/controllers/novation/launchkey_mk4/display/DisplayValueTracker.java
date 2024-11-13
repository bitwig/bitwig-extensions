package com.bitwig.extensions.controllers.novation.launchkey_mk4.display;

import java.util.Arrays;
import java.util.List;

import com.bitwig.extension.controller.api.StringValue;

public class DisplayValueTracker {
    
    private final DisplayControl control;
    private final List<StringValue> values;
    private boolean expectUpdate = false;
    
    public DisplayValueTracker(final DisplayControl control, final StringValue... values) {
        this.control = control;
        this.values = Arrays.stream(values).toList();
        for (int i = 0; i < this.values.size(); i++) {
            final int index = i;
            this.values.get(i).addValueObserver(val -> handleValueChanged(val, index));
        }
    }
    
    private void handleValueChanged(final String stringValue, final int index) {
        if (this.expectUpdate) {
            if (values.size() == 2) {
                control.show2Line(index == 0 ? stringValue : values.get(0).get(),
                    index == 1 ? stringValue : values.get(1).get());
            } else if (values.size() == 3) {
                control.showTempParamLines(index == 0 ? stringValue : values.get(0).get(),
                    index == 1 ? stringValue : values.get(1).get(), //
                    index == 2 ? stringValue : values.get(2).get());
            }
            expectUpdate = false;
        }
    }
    
    public void notifyUpdate() {
        this.expectUpdate = true;
    }
    
    public void show() {
        if (values.size() == 2) {
            control.show2Line(values.get(0).get(), values.get(1).get());
        } else if (values.size() == 3) {
            control.showTempParamLines(values.get(0).get(), values.get(1).get(), //
                values.get(2).get());
        }
    }
}
