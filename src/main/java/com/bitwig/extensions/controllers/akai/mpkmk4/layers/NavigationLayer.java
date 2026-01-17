package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.akai.apc.common.control.ClickEncoder;
import com.bitwig.extensions.controllers.akai.mpkmk4.GlobalStates;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkFocusClip;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkHwElements;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkViewControl;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkButton;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkCcAssignment;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkMultiStateButton;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.LineDisplay;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkColorLookup;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class NavigationLayer extends Layer {
    
    private final Layer shiftLayer;
    private final LayerCollection layerCollection;
    private final SceneBank scenesBank;
    private final TrackBank trackBank;
    private final Scene focusScene;
    private final LineDisplay display;
    private final LineDisplay menuDisplay;
    private int sceneColor = 0;
    
    private final MpkFocusClip focusClip;
    private boolean encoderSceneAction;
    private final RemotesControlHandler remotesControlHandler;
    
    public NavigationLayer(final Layers layers, final MpkHwElements hwElements, final GlobalStates globalStates,
        final LayerCollection layerCollection, final MpkViewControl viewControl, final MpkFocusClip focusClip) {
        super(layers, "NAVIGATION");
        this.layerCollection = layerCollection;
        remotesControlHandler = layerCollection.getRemotesHandler();
        this.trackBank = viewControl.getTrackBank();
        this.focusClip = focusClip;
        this.trackBank.sceneBank().scrollPosition().markInterested();
        shiftLayer = new Layer(layers, "NAVIGATION_SHIFT_LAYER");
        globalStates.getShiftHeld().addValueObserver(shift -> {
            if (isActive()) {
                shiftLayer.setIsActive(shift);
            }
        });
        display = hwElements.getMainLineDisplay();
        menuDisplay = hwElements.getMenuLineDisplay();
        scenesBank = viewControl.getFocusTrackBank().sceneBank();
        scenesBank.setIndication(true);
        scenesBank.scrollPosition().addValueObserver(this::handleSceneScrollPosition);
        focusScene = scenesBank.getScene(0);
        focusScene.color().addValueObserver((r, g, b) -> this.updateSceneColor(MpkColorLookup.rgbToIndex(r, g, b)));
        focusScene.name().addValueObserver(this::handleSceneNameChanged);
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        final MpkMultiStateButton leftButton = hwElements.getButton(MpkCcAssignment.BANK_LEFT);
        final MpkMultiStateButton rightButton = hwElements.getButton(MpkCcAssignment.BANK_RIGHT);
        leftButton.bindLightOnOff(this, cursorTrack.hasPrevious());
        leftButton.bindRepeatHold(this, () -> cursorTrack.selectPrevious());
        rightButton.bindLightOnOff(this, cursorTrack.hasNext());
        rightButton.bindRepeatHold(this, () -> cursorTrack.selectNext());
        
        leftButton.bindLightOnOff(shiftLayer, () -> remotesControlHandler.canNavigateLeft());
        leftButton.bindRepeatHold(shiftLayer, () -> remotesControlHandler.navigateLeft());
        rightButton.bindLightOnOff(shiftLayer, () -> remotesControlHandler.canNavigateRight());
        rightButton.bindRepeatHold(shiftLayer, () -> remotesControlHandler.navigateRight());
        
        final ClickEncoder encoder = hwElements.getMainEncoder();
        final MpkButton encoderButton = hwElements.getMainEncoderPressButton();
        encoder.bind(this, this::sceneSelection);
        encoderButton.bindIsPressed(this, this::handleEncoderPressed);
        encoder.bind(shiftLayer, remotesControlHandler::handleShiftEncoderTurn);
        encoderButton.bindIsPressed(shiftLayer, this::changeMode);
    }
    
    private void updateSceneColor(final int colorIndex) {
        sceneColor = colorIndex == 102 ? 0 : colorIndex;
        display.setColorIndex(1, 1, sceneColor);
    }
    
    private void handleSceneNameChanged(final String sceneName) {
        if (encoderSceneAction) {
            display.setText(1, 1, focusScene.name().get());
            encoderSceneAction = false;
        }
    }
    
    private void handleSceneScrollPosition(final int pos) {
        final int trackBankPosition = trackBank.sceneBank().scrollPosition().get();
        if (pos < trackBankPosition) {
            trackBank.sceneBank().scrollPosition().set(pos);
        } else if (pos >= (trackBankPosition + 4)) {
            trackBank.sceneBank().scrollPosition().set(pos - 3);
        }
        focusClip.setSelectedSlotIndex(pos);
    }
    
    private void changeMode(final Boolean pressed) {
        if (pressed) {
            if (layerCollection.getPadMode().get() == LayerId.DRUM_PAD_CONTROL) {
                final Layer menuLayer = layerCollection.get(LayerId.PAD_MENU_LAYER);
                menuLayer.setIsActive(true);
            } else {
                remotesControlHandler.incrementEncoderMode(1, true);
                display.temporaryInfo(1, "Knob Mode", remotesControlHandler.getEncoderModeValue().get());
            }
        }
    }
    
    private void handleEncoderPressed(final Boolean pressed) {
        if (!pressed) {
            return;
        }
        scenesBank.getScene(0).launch();
    }
    
    private void sceneSelection(final int inc) {
        scenesBank.scrollBy(inc);
        display.activateTemporary(1);
        display.setText(1, 1, focusScene.name().get());
        display.setColorIndex(1, 1, sceneColor);
        display.setText(1, 2, "");
        encoderSceneAction = true;
    }
    
    @Override
    protected void onDeactivate() {
        this.shiftLayer.setIsActive(false);
        layerCollection.get(LayerId.PAD_MENU_LAYER).setIsActive(false);
    }
}
