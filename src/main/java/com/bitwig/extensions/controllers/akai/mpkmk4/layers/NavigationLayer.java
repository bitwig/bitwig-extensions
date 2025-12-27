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
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkOnOffButton;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.LineDisplay;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkColorLookup;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkDisplayFont;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class NavigationLayer extends Layer {
    
    private final Layer shiftLayer;
    private final Layer menuLayer;
    private final LayerCollection layerCollection;
    private final SceneBank scenesBank;
    private final TrackBank trackBank;
    private final Scene focusScene;
    private final LineDisplay display;
    private final LineDisplay menuDisplay;
    private int sceneColor = 0;
    
    private int selectedMenu = 0;
    private int currentSelection = 0;
    private final String[] entries = {"Remote Control", "Track Mixer", "Grid Mixer"};
    
    
    private final MpkFocusClip focusClip;
    
    public NavigationLayer(final Layers layers, final MpkHwElements hwElements, final GlobalStates globalStates,
        final LayerCollection layerCollection, final MpkViewControl viewControl, final MpkFocusClip focusClip) {
        super(layers, "NAVIGATION");
        this.layerCollection = layerCollection;
        this.trackBank = viewControl.getTrackBank();
        this.focusClip = focusClip;
        this.trackBank.sceneBank().scrollPosition().markInterested();
        shiftLayer = new Layer(layers, "NAVIGATION_SHIFT_LAYER");
        menuLayer = new Layer(layers, "MENU");
        globalStates.getShiftHeld().addValueObserver(shift -> {
            if (isActive()) {
                shiftLayer.setIsActive(shift);
            }
        });
        display = hwElements.getMainLineDisplay();
        menuDisplay = hwElements.getMenuLineDisplay();
        updateMenu();
        scenesBank = viewControl.getFocusTrackBank().sceneBank();
        scenesBank.setIndication(true);
        scenesBank.scrollPosition().addValueObserver(this::handleSceneScrollPosition);
        focusScene = scenesBank.getScene(0);
        focusScene.color().addValueObserver((r, g, b) -> this.updateSceneColor(MpkColorLookup.rgbToIndex(r, g, b)));
        focusScene.name().addValueObserver(this::handleSceneNameChanged);
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        final MpkOnOffButton leftButton = hwElements.getButton(MpkCcAssignment.BANK_LEFT);
        final MpkOnOffButton rightButton = hwElements.getButton(MpkCcAssignment.BANK_RIGHT);
        leftButton.bindLight(this, cursorTrack.hasPrevious());
        leftButton.bindRepeatHold(this, () -> cursorTrack.selectPrevious());
        rightButton.bindLight(this, cursorTrack.hasNext());
        rightButton.bindRepeatHold(this, () -> cursorTrack.selectNext());
        
        leftButton.bindLight(shiftLayer, () -> layerCollection.canNavigateLeft());
        leftButton.bindRepeatHold(shiftLayer, () -> layerCollection.navigateLeft());
        rightButton.bindLight(shiftLayer, () -> layerCollection.canNavigateRight());
        rightButton.bindRepeatHold(shiftLayer, () -> layerCollection.navigateRight());
        
        final ClickEncoder encoder = hwElements.getMainEncoder();
        final MpkButton encoderButton = hwElements.getMainEncoderPressButton();
        encoder.bind(this, this::sceneSelection);
        encoderButton.bindIsPressed(this, this::handleEncoderPressed);
        encoder.bind(shiftLayer, this::deviceSelection);
        encoderButton.bindIsPressed(shiftLayer, this::handleIntoMenu);
        
        encoder.bind(menuLayer, this::handleMenuScroll);
        encoderButton.bindIsPressed(menuLayer, this::handleMenuSelect);
    }
    
    private void updateMenu() {
        for (int i = 0; i < entries.length; i++) {
            menuDisplay.setText(
                i, entries[i], i == selectedMenu ? MpkDisplayFont.PT16_BOLD : MpkDisplayFont.PT16,
                i == selectedMenu ? 69 : (i == currentSelection ? 2 : 0));
        }
    }
    
    private void handleMenuSelect(final Boolean pressed) {
        if (pressed) {
            menuDisplay.setActive(false);
            display.setActive(true);
            menuLayer.setIsActive(false);
            currentSelection = selectedMenu;
            if (currentSelection == 0) {
                layerCollection.backToDeviceControl();
            } else if (currentSelection == 1) {
                layerCollection.setEncoderLayerMode(LayerId.TRACK_CONTROL);
            } else if (currentSelection == 2) {
                layerCollection.setEncoderLayerMode(LayerId.MIX_CONTROL);
            }
            updateMenu();
        }
    }
    
    private void handleMenuScroll(final int inc) {
        final int next = selectedMenu + inc;
        if (next >= 0 && next < entries.length) {
            selectedMenu = next;
            updateMenu();
        }
    }
    
    private void updateSceneColor(final int colorIndex) {
        sceneColor = colorIndex == 102 ? 0 : colorIndex;
        display.setColorIndex(1, 1, sceneColor);
    }
    
    private void handleSceneNameChanged(final String sceneName) {
        display.setText(1, 1, focusScene.name().get());
    }
    
    private void handleSceneScrollPosition(final int pos) {
        final int trackBankPosition = trackBank.sceneBank().scrollPosition().get();
        trackBank.sceneBank().scrollPosition().set(pos);
        if (pos < trackBankPosition || pos >= (trackBankPosition + 4)) {
            trackBank.sceneBank().scrollPosition().set(pos);
        }
        focusClip.setSelectedSlotIndex(pos);
    }
    
    private void handleIntoMenu(final Boolean pressed) {
        if (pressed) {
            if (menuLayer.isActive()) {
                menuDisplay.setActive(false);
                display.setActive(true);
            } else {
                display.setActive(false);
                menuDisplay.setActive(true);
            }
            menuLayer.setIsActive(!menuLayer.isActive());
        }
    }
    
    private void handleEncoderPressed(final Boolean pressed) {
        if (!pressed) {
            return;
        }
        scenesBank.getScene(0).launch();
    }
    
    private void deviceSelection(final int inc) {
        layerCollection.selectDevice(inc);
    }
    
    private void sceneSelection(final int inc) {
        scenesBank.scrollBy(inc);
        display.activateTemporary(1);
        display.setText(1, 1, focusScene.name().get());
        display.setColorIndex(1, 1, sceneColor);
        display.setText(1, 2, "");
    }
    
    @Override
    protected void onDeactivate() {
        this.shiftLayer.setIsActive(false);
    }
}
