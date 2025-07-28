package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings;

import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.DisplaySegment;
import com.bitwig.extensions.framework.Binding;
import com.bitwig.extensions.framework.values.BasicStringValue;

public class SegmentDisplayBinding extends Binding<StringValue, DisplaySegment> {
    
    private String title;
    private String name;
    private long blockTime;
    
    public SegmentDisplayBinding(final StringValue title, final StringValue name, final DisplaySegment display) {
        super(name, name, display);
        title.addValueObserver(this::handleTitleUpdate);
        name.addValueObserver(this::handleNameUpdate);
        this.title = title.get();
        this.name = name.get();
    }
    
    public SegmentDisplayBinding(final String title, final StringValue name, final DisplaySegment display) {
        this(new BasicStringValue(title), name, display);
    }
    
    public void blockUpdate() {
        this.blockTime = System.currentTimeMillis();
    }
    
    private boolean isBlocked() {
        return System.currentTimeMillis() - blockTime < 100;
    }
    
    private void handleNameUpdate(final String name) {
        this.name = name;
        if (isActive() && !isBlocked()) {
            getTarget().show2Lines(title, name);
        }
    }
    
    private void handleTitleUpdate(final String name) {
        this.title = name;
        if (isActive() && !isBlocked()) {
            getTarget().show2Lines(title, name);
        }
    }
    
    @Override
    protected void deactivate() {
    }
    
    @Override
    protected void activate() {
        getTarget().show2Lines(title, name);
    }
}
