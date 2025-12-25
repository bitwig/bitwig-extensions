package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extensions.controllers.akai.mpkmk4.GlobalStates;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkHwElements;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkViewControl;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkCcAssignment;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkOnOffButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class NavigationLayer extends Layer {
    
    private final Layer shiftLayer;
    
    public NavigationLayer(Layers layers,
        MpkHwElements hwElements, GlobalStates globalStates,
        RemotesLayerHandler remotesLayerHandler,
        final MpkMidiProcessor midiProcessor, MpkViewControl viewControl) {
        super(layers, "NAVIGATION");
        shiftLayer = new Layer(layers, "NAVIGATION_SHIFT_LAYER");
        globalStates.getShiftHeld().addValueObserver(shift-> {
            if(isActive()) {
                shiftLayer.setIsActive(shift);
            }
        });
        
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        final MpkOnOffButton leftButton = hwElements.getButton(MpkCcAssignment.BANK_LEFT);
        final MpkOnOffButton rightButton = hwElements.getButton(MpkCcAssignment.BANK_RIGHT);
        leftButton.bindLight(this, cursorTrack.hasPrevious());
        leftButton.bindRepeatHold(this, () -> cursorTrack.selectPrevious());
        rightButton.bindLight(this, cursorTrack.hasNext());
        rightButton.bindRepeatHold(this, () -> cursorTrack.selectNext());
        
        leftButton.bindLight(shiftLayer, () -> remotesLayerHandler.canNavigateLeft());
        leftButton.bindRepeatHold(shiftLayer, () -> remotesLayerHandler.navigateLeft());
        rightButton.bindLight(shiftLayer, () -> remotesLayerHandler.canNavigateRight());
        rightButton.bindRepeatHold(shiftLayer, () -> remotesLayerHandler.navigateRight());
        
        
    }
    
    @Override
    protected void onDeactivate() {
        this.shiftLayer.setIsActive(false);
    }
}
