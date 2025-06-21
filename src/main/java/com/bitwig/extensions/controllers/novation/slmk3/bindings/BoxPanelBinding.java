package com.bitwig.extensions.controllers.novation.slmk3.bindings;

import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;
import com.bitwig.extensions.controllers.novation.slmk3.display.panel.BoxPanel;
import com.bitwig.extensions.framework.Binding;

public class BoxPanelBinding extends Binding<StringValue, BoxPanel> {
    
    private String name;
    private String displayValue;
    private final SlRgbState frameColor;
    
    public BoxPanelBinding(final StringValue parameterDisplay, final BoxPanel panel, final StringValue nameSource,
        final SlRgbState frameColor) {
        super(panel, parameterDisplay, panel);
        parameterDisplay.markInterested();
        this.name = nameSource.get();
        this.displayValue = parameterDisplay.get();
        this.frameColor = frameColor;
        nameSource.addValueObserver(this::handleNameChange);
        parameterDisplay.addValueObserver(this::handleDisplayValue);
    }
    
    private void handleNameChange(final String name) {
        this.name = name;
        if (isActive()) {
            getTarget().setText(0, this.name);
        }
    }
    
    private void handleDisplayValue(final String value) {
        this.displayValue = value;
        if (isActive()) {
            getTarget().setText(1, this.displayValue);
        }
    }
    
    @Override
    protected void deactivate() {
    }
    
    @Override
    protected void activate() {
        getTarget().setText(0, this.name);
        getTarget().setTopSelected(false);
        getTarget().setText(1, this.displayValue);
        getTarget().setColor(0, frameColor);
    }
}
