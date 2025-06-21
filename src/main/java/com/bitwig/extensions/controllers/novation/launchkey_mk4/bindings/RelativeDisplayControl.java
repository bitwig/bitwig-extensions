package com.bitwig.extensions.controllers.novation.launchkey_mk4.bindings;

import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.DisplayControl;
import com.bitwig.extensions.framework.Binding;
import com.bitwig.extensions.framework.values.BasicStringValue;

public class RelativeDisplayControl extends Binding<DisplayId, Object> {
    
    private String paramName;
    private final String title;
    private long incTime = 0;
    private String displayValue;
    private final boolean pending = false;
    private final int targetId;
    private final DisplayControl display;
    private final IntConsumer incAction;
    private final Runnable activateAction;
    
    private RelativeDisplayControl(final DisplayId displayId, final String title, final StringValue paramName,
        final StringValue paramValue, final IntConsumer incAction, final Runnable activateAction) {
        // This has to become some kind of binding
        super(displayId, displayId, incAction);
        this.targetId = 0x15 + displayId.index();
        this.title = title;
        this.paramName = paramName.get();
        paramValue.addValueObserver(this::handleDisplayValue);
        paramName.addValueObserver(this::handleParamNameChanged);
        this.displayValue = paramValue.get();
        this.display = displayId.display();
        this.incAction = incAction;
        this.activateAction = activateAction;
    }
    
    public RelativeDisplayControl(final int index, final DisplayControl display, final String title,
        final String paramName, final StringValue paramValue, final IntConsumer incAction,
        final Runnable activateAction) {
        // This has to become some kind of binding
        this(new DisplayId(index, display), title, new BasicStringValue(paramName), paramValue, incAction,
            activateAction);
    }
    
    public RelativeDisplayControl(final int index, final DisplayControl display, final String title,
        final StringValue paramName, final StringValue paramValue, final IntConsumer incAction,
        final Runnable activateAction) {
        // This has to become some kind of binding
        this(new DisplayId(index, display), title, paramName, paramValue, incAction, activateAction);
    }
    
    public RelativeDisplayControl(final int index, final DisplayControl display, final String title,
        final String paramName, final StringValue paramValue, final IntConsumer incAction) {
        // This has to become some kind of binding
        this(new DisplayId(index, display), title, new BasicStringValue(paramName), paramValue, incAction, null);
    }
    
    private void handleParamNameChanged(final String value) {
        this.paramName = value;
        if (isActive()) {
            display.setText(targetId, 1, paramName);
            final long diff = System.currentTimeMillis() - incTime;
            if (diff < 200) {
                display.showDisplay(targetId);
            }
        }
    }
    
    private void handleDisplayValue(final String value) {
        this.displayValue = value;
        if (isActive()) {
            display.setText(targetId, 2, displayValue);
            final long diff = System.currentTimeMillis() - incTime;
            if (diff < 200) {
                display.showDisplay(targetId);
            }
        }
    }
    
    public void handleInc(final int inc) {
        incAction.accept(inc);
        incTime = System.currentTimeMillis();
    }
    
    @Override
    protected void deactivate() {
    }
    
    @Override
    protected void activate() {
        display.setText(targetId, 0, title);
        display.setText(targetId, 1, paramName);
        display.setText(targetId, 2, displayValue);
        if (activateAction != null) {
            activateAction.run();
        }
    }
}
