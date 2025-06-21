package com.bitwig.extensions.controllers.novation.slmk3.bindings;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;
import com.bitwig.extensions.controllers.novation.slmk3.display.panel.KnobPanel;
import com.bitwig.extensions.controllers.novation.slmk3.value.ObservableColor;
import com.bitwig.extensions.framework.Binding;

public class ParameterPanelBinding extends Binding<Parameter, KnobPanel> {
    
    private int value;
    private final SlRgbState knobColor;
    private SlRgbState topColor;
    private boolean exists;
    private String name = "";
    private String displayValue = "";
    
    public ParameterPanelBinding(final Parameter parameter, final KnobPanel panel, final StringValue nameSource,
        final SlRgbState knobColor) {
        this(parameter, panel, nameSource, knobColor, null);
    }
    
    public ParameterPanelBinding(final Parameter parameter, final KnobPanel panel, final StringValue nameSource,
        final SlRgbState knobColor, final ObservableColor topColorSource) {
        super(panel, parameter, panel);
        this.knobColor = knobColor;
        if (topColorSource == null) {
            topColor = this.knobColor;
        } else {
            topColor = topColorSource.get();
            topColorSource.addValueObserver(value -> handleTopColorChanged(value));
        }
        parameter.value().addValueObserver(128, this::handleValue);
        parameter.exists().addValueObserver(this::handleExists);
        nameSource.addValueObserver(this::handleNameChange);
        parameter.displayedValue().addValueObserver(this::handleDisplayValue);
    }
    
    private void handleTopColorChanged(final SlRgbState value) {
        this.topColor = value;
        if (isActive()) {
            getTarget().setTopBarColor(exists ? topColor : SlRgbState.OFF);
        }
    }
    
    private void handleDisplayValue(final String value) {
        this.displayValue = value;
        if (isActive()) {
            getTarget().setText(1, this.displayValue);
        }
    }
    
    private void handleNameChange(final String name) {
        this.name = name;
        if (isActive()) {
            getTarget().setText(0, this.name);
        }
    }
    
    private void handleExists(final boolean exists) {
        this.exists = exists;
        if (isActive()) {
            getTarget().setKnobIconColor(exists ? knobColor : SlRgbState.OFF);
            getTarget().setTopBarColor(exists ? topColor : SlRgbState.OFF);
        }
    }
    
    private void handleValue(final int value) {
        this.value = value;
        if (isActive()) {
            getTarget().setKnobValue(value);
        }
    }
    
    @Override
    protected void deactivate() {
        
    }
    
    @Override
    protected void activate() {
        getTarget().setKnobIconColor(exists ? knobColor : SlRgbState.OFF);
        getTarget().setKnobValue(value);
        getTarget().setText(0, this.name);
        getTarget().setText(1, this.displayValue);
        getTarget().setTopBarColor(exists ? topColor : SlRgbState.OFF);
    }
}
