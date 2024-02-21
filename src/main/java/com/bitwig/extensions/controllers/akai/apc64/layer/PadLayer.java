package com.bitwig.extensions.controllers.akai.apc64.layer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.akai.apc.common.control.RgbButton;
import com.bitwig.extensions.controllers.akai.apc.common.led.ColorLookup;
import com.bitwig.extensions.controllers.akai.apc.common.led.LedBehavior;
import com.bitwig.extensions.controllers.akai.apc.common.led.RgbLightState;
import com.bitwig.extensions.controllers.akai.apc64.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.di.PostConstruct;

import java.util.Arrays;

@Component
public class PadLayer extends Layer {

    private static final int[] VEL_TABLE = {5, 10, 25, 60, 75, 90, 100, 127};
    private static final int[] FIXED_COLORS = {42, 42, 41, 41, 46, 46, 45, 45};

    private final double[] rateTable = {0.0833333, 0.125, 0.1666666, 0.25, 0.33333, 0.5, 0.666666, 1.0};

    //private final double[] rateTable = {0.125, 0.25, 0.5, 1.0, 2.0};
    //private final String[] rateDisplayValues = {"1/32T", "1/32", "1/16T", "1/16", "1/8T", "1/8", "1/4T", "1/4"};

    private final double[] arpRateTable = {1.0, 0.5, 0.33333, 0.25, 0.1666666, 0.125, 0.0833333, 0.0625};
    private final String[] rateDisplayValues = {"1/4", "1/8", "1/8T", "1/16", "1/16T", "1/32", "1/32T", "1/64"};
    private static final int[] ARP_COLORS = {53, 53, 56, 53, 56, 53, 56, 53};

    @Inject
    private MainDisplay mainDisplay;
    @Inject
    private FocusClip focusClip;
    private final ModifierStates states;

    private final Apc64MidiProcessor midiProcessor;
    private final ViewControl viewControl;
    private final DrumPadBank drumPadBank;
    private final NoteInput noteInput;
    private PadMode currentMode = PadMode.SESSION;
    private boolean inDrumMode = false;

    private final Layer shiftLayer;
    private final Layer clearLayer;
    private final Layer muteLayer;
    private final Layer soloLayer;

    protected final int[] padToNote = new int[16];
    private final Integer[] noteTable = new Integer[128];
    private final Integer[] velocityTable = new Integer[128];
    private final int[] padColors = new int[16];
    private final boolean[] isSelected = new boolean[16];
    private final boolean[] isPlaying = new boolean[128];
    private int padOffset = 36;
    private int fixedVelocity = -1;
    private int selectedVelocityIndex = -1;
    private int selectedNoteRepeatIndex = -1;
    private int soloHeld = 0;
    private final Arpeggiator arp;

    public PadLayer(Layers layers, ViewControl viewControl, Apc64MidiProcessor midiProcessor, ModifierStates states) {
        super(layers, "PAD_LAYER");

        this.shiftLayer = new Layer(layers, "PAD_SHIFT_LAYER");
        this.clearLayer = new Layer(layers, "PAD_CLEAR_LAYER");
        this.muteLayer = new Layer(layers, "PAD_MUTE_LAYER");
        this.soloLayer = new Layer(layers, "PAD_SOLO_LAYER");
        this.midiProcessor = midiProcessor;
        this.noteInput = midiProcessor.getNoteInput();
        arp = noteInput.arpeggiator();
        initArp();
        this.viewControl = viewControl;
        this.states = states;
        viewControl.getCursorTrack().playingNotes().addValueObserver(this::handleNotes);
        this.states.getShiftActive().addValueObserver(mod -> applyLayers());
        this.states.getClearActive().addValueObserver(mod -> applyLayers());
        PinnableCursorDevice primaryDevice = viewControl.getDeviceControl().getPrimaryDevice();
        primaryDevice.hasDrumPads().addValueObserver(this::handleHasDrumPadsChanged);
        drumPadBank = viewControl.getDeviceControl().getDrumPadBank();
        drumPadBank.scrollPosition().addValueObserver(this::handlePadBankScrolling);

        Arrays.fill(padColors, 0);
        Arrays.fill(noteTable, -1);
        Arrays.fill(padToNote, -1);
        setVelocity(-1);
        midiProcessor.addModeChangeListener(currentMode -> {
            this.currentMode = currentMode;
            if (isActive()) {
                midiProcessor.restoreState();
            }
        });
    }

    public void duplicateContent() {
        if (inDrumMode) {
            focusClip.duplicateContent();
        }
    }

    private void initArp() {
        arp.isEnabled().markInterested();
        arp.usePressureToVelocity().markInterested();
        arp.usePressureToVelocity().set(true);
        arp.octaves().markInterested();
        arp.rate().markInterested();
        arp.mode().markInterested();
        arp.rate().set(arpRateTable[0]);
    }

    private void setVelocity(int fixedValue) {
        for (int i = 0; i < 128; i++) {
            velocityTable[i] = fixedValue == -1 ? i : fixedValue;
        }
    }

    @PostConstruct
    public void init(HardwareElements hwElements) {
        for (int i = 0; i < 4; i++) {
            final int columnIndex = i;
            for (int j = 0; j < 4; j++) {
                final int rowIndex = j;
                int padIndex = rowIndex * 4 + columnIndex;
                DrumPad pad = drumPadBank.getItemAt(padIndex);
                setUpPad(padIndex, pad);
                final RgbButton button = hwElements.getGridButton(7 - rowIndex, columnIndex);
                button.bindLight(this, () -> getPadLight(padIndex, pad));
                button.bindPressed(muteLayer, () -> pad.mute().toggle());
                button.bindLight(muteLayer, () -> getPadMuteLight(padIndex, pad));
                button.bindIsPressed(soloLayer, pressed -> handleSolo(pressed, pad));
                button.bindLight(soloLayer, () -> getPadSoloLight(padIndex, pad));
                button.bindPressed(shiftLayer, () -> handleSelect(padIndex, pad));
                button.bindPressed(clearLayer, () -> clearNotes(padIndex));
            }
        }
        for (int row = 4; row < 6; row++) {
            for (int col = 4; col < 8; col++) {
                final RgbButton button = hwElements.getGridButton(row, col);
                int index = (5 - row) * 4 + (col - 4);
                button.bindPressed(this, () -> selectVelocity(index));
                button.bindLight(this, () -> getVelocityColors(index));
            }
        }
        for (int row = 6; row < 8; row++) {
            for (int col = 4; col < 8; col++) {
                final RgbButton button = hwElements.getGridButton(row, col);
                int index = (7 - row) * 4 + (col - 4);
                button.bindIsPressed(this, pressed -> setNoteRepeat(index, pressed));
                button.bindLight(this, () -> getNoteRepeatColors(index));
            }
        }
    }

    private void handleSelect(int padIndex, DrumPad pad) {
        if (isSelected[padIndex]) {
            PinnableCursorDevice cursorDevice = viewControl.getDeviceControl().getCursorDevice();
            if (cursorDevice.hasDrumPads().get()) {
                cursorDevice.selectFirstInKeyPad(padToNote[padIndex]);
            } else {
                cursorDevice.selectParent();
            }
        } else {
            pad.selectInEditor();
        }
    }

    private void handleSolo(boolean pressed, DrumPad pad) {
        if (pressed) {
            pad.solo().toggle(soloHeld == 0);
            soloHeld++;
        } else {
            soloHeld--;
        }
    }

    private void setNoteRepeat(int index, boolean pressed) {
        if (pressed) {
            if (index == selectedNoteRepeatIndex) {
                selectedNoteRepeatIndex = -1;
                arp.isEnabled().set(false);
                mainDisplay.enterMode(MainDisplay.ScreenMode.INFO, "Note Repeat", "Off");
            } else {
                selectedNoteRepeatIndex = index;
                mainDisplay.enterMode(MainDisplay.ScreenMode.INFO, "Note Repeat",
                        rateDisplayValues[selectedNoteRepeatIndex]);
                double arpRate = arpRateTable[selectedNoteRepeatIndex];
                arp.rate().set(arpRate);
                arp.mode().set("all"); // that's the note repeat way
                arp.octaves().set(0);
                arp.humanize().set(0);
                arp.isFreeRunning().set(false);
                arp.isEnabled().set(true);
            }
        }
    }

    private RgbLightState getNoteRepeatColors(int padIndex) {
        if (selectedNoteRepeatIndex == padIndex) {
            return RgbLightState.WHITE;
        }
        return RgbLightState.of(ARP_COLORS[padIndex]);
    }


    private void selectVelocity(int index) {
        if (index == selectedVelocityIndex) {
            selectedVelocityIndex = -1;
            fixedVelocity = -1;
            setVelocity(-1);
            this.noteInput.setVelocityTranslationTable(velocityTable);
            mainDisplay.enterMode(MainDisplay.ScreenMode.INFO, "Fixed Velocity", "Off");
        } else {
            selectedVelocityIndex = index;
            fixedVelocity = VEL_TABLE[selectedVelocityIndex];
            mainDisplay.enterMode(MainDisplay.ScreenMode.INFO, "Fixed Velocity", "%d".formatted(fixedVelocity));
            setVelocity(fixedVelocity);
            this.noteInput.setVelocityTranslationTable(velocityTable);
        }
    }

    private RgbLightState getVelocityColors(int padIndex) {
        if (selectedVelocityIndex == padIndex) {
            return RgbLightState.WHITE;
        }
        LedBehavior behavior = padIndex % 2 == 0 ? LedBehavior.LIGHT_50 : LedBehavior.FULL;
        return RgbLightState.of(FIXED_COLORS[padIndex], behavior);
    }

    private void clearNotes(int padIndex) {
        if (padToNote[padIndex] != -1) {
            focusClip.clearNotes(padToNote[padIndex]);
        }
    }

    private RgbLightState getPadMuteLight(int padIndex, DrumPad pad) {
        if (pad.exists().get()) {
            if (pad.mute().get()) {
                return isPlaying(padIndex) ? RgbLightState.MUTE_PLAY_FULL : RgbLightState.ORANGE_FULL;
            } else {
                return isPlaying(padIndex) ? RgbLightState.MUTE_PLAY_DIM : RgbLightState.ORANGE_DIM;
            }
        }
        return isPlaying(padIndex) ? RgbLightState.WHITE : RgbLightState.WHITE_DIM;
    }

    private RgbLightState getPadSoloLight(int padIndex, DrumPad pad) {
        if (pad.exists().get()) {
            if (pad.solo().get()) {
                return isPlaying(padIndex) ? RgbLightState.SOLO_PLAY_FULL : RgbLightState.YELLOW_FULL;
            } else {
                return isPlaying(padIndex) ? RgbLightState.SOLO_PLAY_YELLOW_DIM : RgbLightState.YELLOW_DIM;
            }
        }
        return isPlaying(padIndex) ? RgbLightState.WHITE : RgbLightState.WHITE_DIM;
    }

    private RgbLightState getPadLight(int padIndex, DrumPad pad) {
        if (isSelected[padIndex]) {
            return isPlaying(padIndex) ? RgbLightState.WHITE : RgbLightState.WHITE_SEL;
        }
        if (pad.exists().get()) {
            LedBehavior lightState = isPlaying(padIndex) ? LedBehavior.FULL : LedBehavior.LIGHT_25;
            if (padColors[padIndex] != 0) {
                return RgbLightState.of(padColors[padIndex], lightState);
            } else {
                return RgbLightState.of(viewControl.getCursorTrackColor(), lightState);
            }
        }
        return isPlaying(padIndex) ? RgbLightState.WHITE : RgbLightState.WHITE_DIM;
    }

    private void handleHasDrumPadsChanged(boolean hasDrumPads) {
        this.inDrumMode = hasDrumPads;
        if (isActive() && currentMode.isKeyRelated()) {
            midiProcessor.setDrumMode(hasDrumPads);
        }
    }

    private void handleNotes(final PlayingNote[] playingNotes) {
        if (!isActive()) {
            return;
        }
        Arrays.fill(isPlaying, false);
        for (final PlayingNote playingNote : playingNotes) {
            isPlaying[playingNote.pitch()] = true;
        }
    }

    public void activateMute(boolean activated) {
        if (!isActive()) {
            return;
        }
        soloHeld = 0;
        muteLayer.setIsActive(activated);
        padActivation(activated);
    }

    public void activateSolo(boolean activated) {
        if (!isActive()) {
            return;
        }
        soloLayer.setIsActive(activated);
        padActivation(activated);
    }

    private void padActivation(boolean activated) {
        if (activated) {
            deactivateNotes();
        } else if (!shiftLayer.isActive() && !clearLayer.isActive()) {
            applyScale();
        }
    }

    public boolean isPlaying(final int index) {
        final int offset = padOffset + index;
        if (offset < 128) {
            return isPlaying[offset];
        }
        return false;
    }

    private void handlePadBankScrolling(int scrollPos) {
        padOffset = scrollPos;
        selectPad(getSelectedIndex());
        if (isActive()) {
            applyScale();
        }
    }

    void selectPad(final int index) {
        final DrumPad pad = drumPadBank.getItemAt(index);
        pad.selectInEditor();
    }

    private int getSelectedIndex() {
        for (int i = 0; i < 16; i++) {
            if (isSelected[i]) {
                return i;
            }
        }
        return 0;
    }

    public void navigateBy(int amount) {
        drumPadBank.scrollBy(amount);
    }

    public boolean canNavigateBy(int amount) {
        int newOffset = amount + padOffset;
        return newOffset >= 0 && newOffset < 112;
    }

    void applyScale() {
        Arrays.fill(noteTable, -1);
        if (inDrumMode) {
            for (int i = 0; i < 16; i++) {
                int noteIndex = (i / 4) * 8 + i % 4;
                noteTable[noteIndex] = padOffset + i;
                padToNote[i] = padOffset + i;
            }
        }
        if (isActive()) {
            noteInput.setKeyTranslationTable(noteTable);
            noteInput.setVelocityTranslationTable(velocityTable);
            this.noteInput.setShouldConsumeEvents(true);
        }
    }

    private void setUpPad(int index, DrumPad pad) {
        pad.color().addValueObserver((r, g, b) -> padColors[index] = ColorLookup.toColor(r, g, b));
        pad.name().markInterested();
        pad.exists().markInterested();
        pad.solo().markInterested();
        pad.mute().markInterested();
        pad.addIsSelectedInEditorObserver(selected -> isSelected[index] = selected);
    }

    private void applyLayers() {
        if (!isActive()) {
            return;
        }
        if (states.isClear()) {
            clearLayer.setIsActive(true);
            shiftLayer.setIsActive(false);
            deactivateNotes();
        } else if (states.isShift()) {
            shiftLayer.setIsActive(true);
            clearLayer.setIsActive(false);
            deactivateNotes();
        } else {
            clearLayer.setIsActive(false);
            shiftLayer.setIsActive(false);
            applyScale();
        }
    }

    @Override
    protected void onActivate() {
        super.onActivate();
        if ((currentMode.isKeyRelated()) && inDrumMode) {
            midiProcessor.setDrumMode(true);
            applyScale();
        }
    }

    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        deactivateNotes();
        soloHeld = 0;
        shiftLayer.setIsActive(false);
        clearLayer.setIsActive(false);
        muteLayer.setIsActive(false);
        soloLayer.setIsActive(false);
    }

    private void deactivateNotes() {
        Arrays.fill(noteTable, -1);
        noteInput.setKeyTranslationTable(noteTable);
    }


}
