package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extensions.controllers.akai.mpkmk4.GlobalStates;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkFocusClip;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkHwElements;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkVariant;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkViewControl;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkCcAssignment;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkMultiStateButton;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.LineDisplay;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class PluginNavigationLayer extends Layer {
    
    private final Layer shiftLayer;
    private final LayerCollection layerCollection;
    private final LineDisplay display;
    private final RemotesControlHandler remotesControlHandler;
    
    // TODO We will probably chuck this class
    public PluginNavigationLayer(final Layers layers, final MpkHwElements hwElements, final GlobalStates globalStates,
        final LayerCollection layerCollection, final MpkViewControl viewControl, final MpkFocusClip focusClip) {
        super(layers, "PLUGIN_NAVIGATION");
        this.layerCollection = layerCollection;
        remotesControlHandler = layerCollection.getRemotesHandler();
        shiftLayer = new Layer(layers, "NAVIGATION_SHIFT_LAYER");
        globalStates.getShiftHeld().addValueObserver(shift -> {
            if (isActive()) {
                shiftLayer.setIsActive(shift);
            }
        });
        display = hwElements.getMainLineDisplay();
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        
        if (globalStates.getVariant().getLayoutStyle() == MpkVariant.LayoutStyle.STANDARD) {
            bindStandard(hwElements, cursorTrack);
        } else {
            bindMiniPlus(hwElements, cursorTrack);
        }
    }
    
    private void bindStandard(MpkHwElements hwElements, final CursorTrack cursorTrack) {
        final MpkMultiStateButton leftButton = hwElements.getButton(MpkCcAssignment.BANK_LEFT);
        final MpkMultiStateButton rightButton = hwElements.getButton(MpkCcAssignment.BANK_RIGHT);
        
        leftButton.bindLightOnOff(this, cursorTrack.hasPrevious());
        leftButton.bindRepeatHold(this, cursorTrack::selectPrevious);
        rightButton.bindLightOnOff(this, cursorTrack.hasNext());
        rightButton.bindRepeatHold(this, cursorTrack::selectNext);
        
        leftButton.bindLightOnOff(shiftLayer, remotesControlHandler::canNavigateLeft);
        leftButton.bindRepeatHold(shiftLayer, remotesControlHandler::navigateLeft);
        rightButton.bindLightOnOff(shiftLayer, remotesControlHandler::canNavigateRight);
        rightButton.bindRepeatHold(shiftLayer, remotesControlHandler::navigateRight);
    }
    
    private void bindMiniPlus(MpkHwElements hwElements, final CursorTrack cursorTrack) {
        final MpkMultiStateButton leftButton = hwElements.getButton(MpkCcAssignment.BANK_LEFT);
        final MpkMultiStateButton rightButton = hwElements.getButton(MpkCcAssignment.BANK_RIGHT);
        final MpkMultiStateButton abChangeButton = hwElements.getButton(MpkCcAssignment.BANK_AB_PLUS);
        
        abChangeButton.bindIsPressed(shiftLayer, this::changeMode);
        
        rightButton.bindRepeatHold(this, remotesControlHandler::selectNextDevice);
        rightButton.bindLightOnOff(this, remotesControlHandler::canSelectNextDevice);
        leftButton.bindRepeatHold(this, remotesControlHandler::selectPreviousDevice);
        leftButton.bindLightOnOff(this, remotesControlHandler::canSelectPreviousDevice);
        
        leftButton.bindRepeatHold(shiftLayer, remotesControlHandler::navigateLeft);
        rightButton.bindRepeatHold(shiftLayer, remotesControlHandler::navigateRight);
        leftButton.bindLightOnOff(shiftLayer, remotesControlHandler::canNavigateLeft);
        rightButton.bindLightOnOff(shiftLayer, remotesControlHandler::canNavigateRight);
    }
    
    private void changeMode(final Boolean pressed) {
        if (!pressed) {
            return;
        }
        remotesControlHandler.incrementEncoderMode(1, true);
        final PadMenuLayer padMenuLayer = (PadMenuLayer) layerCollection.get(LayerId.PAD_MENU_LAYER);
        if (padMenuLayer.isActive()) {
            padMenuLayer.switchModeTemporary();
        }
        display.temporaryInfo(1, "Knob Mode", remotesControlHandler.getEncoderModeValue().get());
    }
    
    @Override
    protected void onDeactivate() {
        this.shiftLayer.setIsActive(false);
        layerCollection.get(LayerId.PAD_MENU_LAYER).setIsActive(false);
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
    }
}
