package com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer;

import com.bitwig.extensions.controllers.novation.launchkey_mk4.CcAssignments;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.GlobalStates;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.LaunchkeyHwElements;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.ViewControl;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.RgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;

@Component
public class SequencerLayer extends Layer {
    
    public SequencerLayer(final Layers layers, final ViewControl viewControl, final LaunchkeyHwElements hwElements,
        final GlobalStates globalStates) {
        super(layers, "SEQUENCER");
        
        final RgbButton navUpButton = hwElements.getButton(CcAssignments.NAV_UP);
        final RgbButton navDownButton = hwElements.getButton(CcAssignments.NAV_DOWN);
        navUpButton.bindRepeatHold(this, () -> navigate(1), 500, 100);
        navUpButton.bindLightPressed(this, this::canScrollUp);
        navDownButton.bindRepeatHold(this, () -> navigate(-1), 500, 100);
        navDownButton.bindLightPressed(this, this::canScrollDown);
    }
    
    private boolean canScrollDown() {
        return false;
    }
    
    private boolean canScrollUp() {
        return false;
    }
    
    private void navigate(final int dir) {
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
    }
}
