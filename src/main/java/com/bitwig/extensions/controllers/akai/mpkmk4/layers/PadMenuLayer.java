package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extensions.controllers.akai.apc.common.control.ClickEncoder;
import com.bitwig.extensions.controllers.akai.mpkmk4.GlobalStates;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkHwElements;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkViewControl;
import com.bitwig.extensions.controllers.akai.mpkmk4.ScaleSetup;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkButton;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkCcAssignment;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkMultiStateButton;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.LineDisplay;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MenuEntry;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MenuList;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkDisplayFont;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.IntValueObject;
import com.bitwig.extensions.framework.values.Scale;
import com.bitwig.extensions.framework.values.ValueObject;

public class PadMenuLayer extends Layer {
    
    private final LineDisplay display;
    private final LineDisplay menuDisplay;
    
    private final Layer internalShiftLayer;
    
    private final MenuList noteMenuList = new MenuList();
    private final MenuList padMenuList = new MenuList();
    private MenuList currentMenuList = padMenuList;
    
    private final GlobalStates states;
    private MenuEntry currentMenu;
    private final DrumPadLayer padLayer;
    private boolean hasDrumPads;
    
    
    public PadMenuLayer(final Layers layers, final MpkHwElements hwElements, final LayerCollection layerCollection,
        final MpkViewControl viewControl, final GlobalStates states) {
        super(layers, "PAD_MENU_LAYER");
        display = hwElements.getMainLineDisplay();
        menuDisplay = hwElements.getMenuLineDisplay();
        padLayer = layerCollection.getDrumPadLayer();
        internalShiftLayer = new Layer(layers, "MENU_SHIFT");
        this.states = states;
        final ScaleSetup scaleSetup = padLayer.getScaleSetup();
        final IntValueObject baseNote = scaleSetup.getBaseNote();
        final BasicStringValue baseNoteString = new BasicStringValue(ScaleSetup.toNote(baseNote.get()));
        baseNote.addValueObserver((o, v) -> baseNoteString.set(ScaleSetup.toNote(v)));
        
        viewControl.getPrimaryDevice().hasDrumPads().addValueObserver(this::handleHasDrumPadsChanged);
        
        final ValueObject<Scale> scaleValue = scaleSetup.getScale();
        final BasicStringValue scaleString = new BasicStringValue(scaleValue.get().getShortName());
        scaleValue.addValueObserver(scale -> scaleString.set(scale.getShortName()));
        
        final IntValueObject octaveValue = scaleSetup.getOctaveOffset();
        final BasicStringValue octaveString = new BasicStringValue(scaleSetup.getStartInfo());
        octaveValue.addValueObserver((o, v) -> octaveString.set(scaleSetup.getStartInfo()));
        
        final IntValueObject padOffsetValue = padLayer.getPadOffset();
        final BasicStringValue padOffsetString = new BasicStringValue(Integer.toString(padOffsetValue.get()));
        padOffsetValue.addValueObserver((o, v) -> padOffsetString.set(Integer.toString(v)));
        
        final RemotesControlHandler remotesHandler = layerCollection.getRemotesHandler();
        
        noteMenuList.add("Exit", () -> setIsActive(false));
        noteMenuList.add("Scale", scaleString, scaleValue::increment);
        noteMenuList.add("Root", baseNoteString, baseNote::increment);
        noteMenuList.add("Oct", octaveString, octaveValue::increment);
        noteMenuList.add(
            "Enc.M", remotesHandler.getEncoderModeValue(),
            inc -> remotesHandler.incrementEncoderMode(inc, false));
        
        padMenuList.add("Exit", () -> setIsActive(false));
        padMenuList.add("Pad Off", padOffsetString, inc -> padOffsetValue.increment(inc * 4));
        padMenuList.add(
            "Enc.M", remotesHandler.getEncoderModeValue(),
            inc -> remotesHandler.incrementEncoderMode(inc, false));
        
        currentMenu = currentMenuList.get(0);
        final ClickEncoder encoder = hwElements.getMainEncoder();
        final MpkButton encoderButton = hwElements.getMainEncoderPressButton();
        encoder.bind(this, this::incrementEncoder);
        encoderButton.bindIsPressed(this, this::handleEncoderButton);
        final MpkMultiStateButton leftButton = hwElements.getButton(MpkCcAssignment.BANK_LEFT);
        final MpkMultiStateButton rightButton = hwElements.getButton(MpkCcAssignment.BANK_RIGHT);
        leftButton.bindLightPressedOnDimmed(this);
        leftButton.bindRepeatHold(this, () -> incrementValue(-1));
        rightButton.bindLightPressedOnDimmed(this);
        rightButton.bindRepeatHold(this, () -> incrementValue(1));
        
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        leftButton.bindLightOnOff(internalShiftLayer, cursorTrack.hasPrevious());
        leftButton.bindRepeatHold(internalShiftLayer, () -> cursorTrack.selectPrevious());
        rightButton.bindLightOnOff(internalShiftLayer, cursorTrack.hasNext());
        rightButton.bindRepeatHold(internalShiftLayer, () -> cursorTrack.selectNext());
        
        states.getShiftHeld().addValueObserver(shift -> {
            if (isActive()) {
                internalShiftLayer.setIsActive(shift);
            }
        });
        layerCollection.getPadMode().addValueObserver(this::handlePadModChange);
    }
    
    private void handleHasDrumPadsChanged(final boolean hasDrumPads) {
        this.hasDrumPads = hasDrumPads;
        if (isActive()) {
            updateCurrentMenuList();
        }
    }
    
    private void updateCurrentMenuList() {
        currentMenuList.resetValueFocus();
        currentMenuList = hasDrumPads ? padMenuList : noteMenuList;
        currentMenuList.updateDisplay(menuDisplay);
    }
    
    private void handlePadModChange(final LayerId layerId) {
        if (isActive() && layerId != LayerId.DRUM_PAD_CONTROL) {
            setIsActive(false);
        }
    }
    
    private void incrementEncoder(final int inc) {
        if (currentMenuList.onValue()) {
            incrementValue(inc);
        } else {
            if (currentMenuList.increment(inc)) {
                currentMenu = currentMenuList.getCurrent();
                currentMenuList.updateDisplay(menuDisplay);
            }
        }
    }
    
    private void incrementValue(final int inc) {
        if (currentMenu.getIncrementHandler() != null) {
            currentMenu.getIncrementHandler().accept(inc);
            currentMenuList.updateMenuEntry(currentMenu, menuDisplay);  // Maybe update only on change
        }
    }
    
    private void handleEncoderButton(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (states.getShiftHeld().get()) {
            this.setIsActive(false);
        } else {
            if (currentMenu.getClickHandler() != null) {
                currentMenu.getClickHandler().run();
            } else {
                currentMenuList.toggleValueFocus();
                currentMenuList.updateDisplay(menuDisplay);
            }
        }
    }
    
    
    @Override
    protected void onActivate() {
        super.onActivate();
        display.setActive(false);
        menuDisplay.setActive(true);
        for (int i = 0; i < 3; i++) {
            menuDisplay.setMenuLine(i, MpkDisplayFont.PT24_BOLD, 0, MenuList.FOREGROUND, MenuList.BACKGROUND);
        }
        updateCurrentMenuList();
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        menuDisplay.setActive(false);
        display.setActive(true);
        internalShiftLayer.setIsActive(false);
    }
}
