package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.display;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extensions.framework.Binding;

public class FooterIconDisplayBinding extends Binding<BooleanValue, ContextPageConfiguration> {
    
    private final LcdDisplay display;
    private final ContextPart.FrameType frameTypeActive;
    private final ContextPart.FrameType frameTypeInactive;
    private final int footerIndex;
    
    public FooterIconDisplayBinding(final ContextPageConfiguration target, final LcdDisplay display,
        final BooleanValue source, final int footerIndex, final ContextPart.FrameType frameTypeActive,
        final ContextPart.FrameType frameTypeInactive) {
        super(source, source, target);
        this.display = display;
        this.frameTypeActive = frameTypeActive;
        this.frameTypeInactive = frameTypeInactive;
        this.footerIndex = footerIndex;
        source.addValueObserver(this::handleValueChanged);
    }
    
    private void handleValueChanged(final boolean active) {
        if (!isActive()) {
            return;
        }
        getTarget().setFramed(footerIndex, active ? frameTypeActive : frameTypeInactive);
        display.updateFooter(getTarget());
    }
    
    @Override
    protected void deactivate() {
        // nothing to do
    }
    
    @Override
    protected void activate() {
        getTarget().setFramed(footerIndex, getSource().get() ? frameTypeActive : frameTypeInactive);
        display.updateFooter(getTarget());
    }
}
