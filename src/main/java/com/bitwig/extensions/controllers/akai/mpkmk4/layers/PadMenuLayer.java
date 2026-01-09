package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.akai.apc.common.control.ClickEncoder;
import com.bitwig.extensions.controllers.akai.mpkmk4.GlobalStates;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkHwElements;
import com.bitwig.extensions.controllers.akai.mpkmk4.ScaleSetup;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkButton;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkCcAssignment;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkMultiStateButton;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.LineDisplay;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkDisplayFont;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.IntValueObject;
import com.bitwig.extensions.framework.values.Scale;
import com.bitwig.extensions.framework.values.ValueObject;

public class PadMenuLayer extends Layer {
    
    private final Color SELECT_BG = Color.fromRGB255(0 << 3, 0 << 2, 0 << 3);
    private final Color SELECT_BG_PARAM = Color.fromRGB255(0 << 3, 0, 0x15 << 3);
    private final Color SELECT_FG = Color.fromRGB255(255, 255, 255);
    private final Color BACKGROUND = Color.fromRGB255(200, 200, 200);
    private final Color FOREGROUND = Color.fromRGB255(0, 0, 0);
    
    
    private final LineDisplay display;
    private final LineDisplay menuDisplay;
    private final List<MenuEntry> menuEntries = new ArrayList<>();
    private int menuEntryPosition = 0;
    private boolean valueFocus = false;
    private final GlobalStates states;
    private final LayerCollection layerCollection;
    private MenuEntry currentMenu;
    private final DrumPadLayer padLayer;
    
    private static class MenuEntry {
        private final int index;
        private final String title;
        private final StringValue value;
        private IntConsumer incrementHandler;
        private Runnable clickHandler;
        
        public MenuEntry(final int index, final String title, final StringValue value,
            final IntConsumer incrementHandler) {
            this.index = index;
            this.title = title;
            this.value = value;
            this.incrementHandler = incrementHandler;
        }
        
        public MenuEntry(final int index, final String title, final Runnable clickHandler) {
            this.index = index;
            this.title = title;
            this.value = null;
            this.clickHandler = clickHandler;
        }
        
        public int getIndex() {
            return index;
        }
    }
    
    public PadMenuLayer(final Layers layers, final MpkHwElements hwElements, final LayerCollection layerCollection,
        final GlobalStates states) {
        super(layers, "PAD_MENU_LAYER");
        display = hwElements.getMainLineDisplay();
        menuDisplay = hwElements.getMenuLineDisplay();
        padLayer = layerCollection.getDrumPadLayer();
        this.layerCollection = layerCollection;
        this.states = states;
        final ScaleSetup scaleSetup = padLayer.getScaleSetup();
        final IntValueObject baseNote = scaleSetup.getBaseNote();
        final BasicStringValue baseNoteString = new BasicStringValue(ScaleSetup.toNote(baseNote.get()));
        baseNote.addValueObserver((o, v) -> baseNoteString.set(ScaleSetup.toNote(v)));
        
        final ValueObject<Scale> scaleValue = scaleSetup.getScale();
        final BasicStringValue scaleString = new BasicStringValue(scaleValue.get().getShortName());
        scaleValue.addValueObserver(scale -> scaleString.set(scale.getShortName()));
        
        final IntValueObject octaveValue = scaleSetup.getOctaveOffset();
        final BasicStringValue octaveString = new BasicStringValue(scaleSetup.getStartInfo());
        octaveValue.addValueObserver((o, v) -> octaveString.set(scaleSetup.getStartInfo()));
        
        final IntValueObject padOffsetValue = padLayer.getPadOffset();
        final BasicStringValue padOffsetString = new BasicStringValue(Integer.toString(padOffsetValue.get()));
        padOffsetValue.addValueObserver((o, v) -> padOffsetString.set(Integer.toString(v)));
        
        int index = 0;
        menuEntries.add(new MenuEntry(index++, "Exit", () -> setIsActive(false)));
        menuEntries.add(new MenuEntry(index++, "Scale", scaleString, scaleValue::increment));
        menuEntries.add(new MenuEntry(index++, "B.Note", baseNoteString, baseNote::increment));
        menuEntries.add(new MenuEntry(index++, "Oct", octaveString, octaveValue::increment));
        menuEntries.add(new MenuEntry(index++, "Pad Off", padOffsetString, inc -> padOffsetValue.increment(inc * 4)));
        menuEntries.add(new MenuEntry(
            index, "Enc.M", layerCollection.getEncoderModeValue(),
            inc -> layerCollection.incrementEncoderMode(inc, false)));
        
        currentMenu = menuEntries.get(0);
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
    }
    
    private void incrementEncoder(final int inc) {
        if (valueFocus) {
            incrementValue(inc);
        } else {
            final int nextEntry = menuEntryPosition + inc;
            if (nextEntry >= 0 && nextEntry < menuEntries.size()) {
                menuEntryPosition = nextEntry;
                currentMenu = menuEntries.get(menuEntryPosition);
                updateDisplay();
            }
        }
    }
    
    private void incrementValue(final int inc) {
        if (currentMenu.incrementHandler != null) {
            currentMenu.incrementHandler.accept(inc);
            updateMenuEntry(currentMenu);  // Maybe update only on change
        }
    }
    
    private void handleEncoderButton(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (states.getShiftHeld().get()) {
            this.setIsActive(false);
        } else {
            if (currentMenu.clickHandler != null) {
                currentMenu.clickHandler.run();
            } else {
                valueFocus = !valueFocus;
                updateDisplay();
            }
        }
    }
    
    
    private void updateDisplay() {
        final int scrollOffset = Math.max(0, menuEntryPosition - 2);
        for (int i = 0; i < 3; i++) {
            final int index = i + scrollOffset;
            if (index < menuEntries.size()) {
                updateMenuEntry(menuEntries.get(index));
            }
        }
    }
    
    private void updateMenuEntry(final MenuEntry entry) {
        final int i = entry.getIndex() - Math.max(0, menuEntryPosition - 2);
        final String value = entry.value != null ? entry.value.get() : "";
        if (entry.getIndex() == menuEntryPosition) {
            menuDisplay.setMenuLine(i, MpkDisplayFont.PT24, 0, SELECT_FG, valueFocus ? SELECT_BG_PARAM : SELECT_BG);
            if (entry.value == null) {
                menuDisplay.setText(i, "%s".formatted(entry.title));
            } else {
                menuDisplay.setText(i, "%s%s: %s".formatted(valueFocus ? ">" : "", entry.title, value));
            }
        } else {
            menuDisplay.setMenuLine(i, MpkDisplayFont.PT24, 0, FOREGROUND, BACKGROUND);
            if (entry.value == null) {
                menuDisplay.setText(i, "%s".formatted(entry.title));
            } else {
                menuDisplay.setText(i, "%s: %s".formatted(entry.title, value));
            }
        }
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
        display.setActive(false);
        menuDisplay.setActive(true);
        for (int i = 0; i < 3; i++) {
            menuDisplay.setMenuLine(i, MpkDisplayFont.PT24_BOLD, 0, FOREGROUND, BACKGROUND);
        }
        updateDisplay();
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        menuDisplay.setActive(false);
        display.setActive(true);
    }
}
