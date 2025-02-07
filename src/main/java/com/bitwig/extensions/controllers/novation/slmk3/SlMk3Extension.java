package com.bitwig.extensions.controllers.novation.slmk3;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.novation.slmk3.control.RgbButton;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;
import com.bitwig.extensions.controllers.novation.slmk3.layer.GridMode;
import com.bitwig.extensions.controllers.novation.slmk3.value.BufferedObservableValue;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;
import com.bitwig.extensions.framework.values.FocusMode;

public class SlMk3Extension extends ControllerExtension {
    
    public static final int REPEAT_FREQUENCY = 50;
    public static final int REPEAT_DELAY = 250;
    private static ControllerHost debugHost;
    private HardwareSurface surface;
    private Layer mainLayer;
    private Layer shiftLayer;
    private Transport transport;
    private GlobalStates globalStates;
    private FocusMode recordFocusMode = FocusMode.LAUNCHER;
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            debugHost.println(String.format(format, args));
        }
    }
    
    protected SlMk3Extension(final SlMk3ExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }
    
    @Override
    public void init() {
        final Context diContext = new Context(this);
        final ControllerHost host = diContext.getService(ControllerHost.class);
        debugHost = host;
        surface = diContext.getService(HardwareSurface.class);
        final MidiProcessor midiProcessor = new MidiProcessor(host);
        diContext.registerService(MidiProcessor.class, midiProcessor);
        globalStates = diContext.getService(GlobalStates.class);
        mainLayer = diContext.createLayer("MAIN");
        shiftLayer = diContext.createLayer("SHIFT");
        transport = diContext.getService(Transport.class);
        initPreferences(host);
        initTransport(diContext);
        initGridMode(diContext);
        initGlobalNavigation(diContext);
        midiProcessor.init();
        mainLayer.activate();
        diContext.activate();
    }
    
    private void initPreferences(final ControllerHost host) {
        final DocumentState documentState = host.getDocumentState(); // THIS
        final SettableEnumValue recordButtonAssignment = documentState.getEnumSetting("Record Button assignment", //
            "Transport", new String[] {FocusMode.LAUNCHER.getDescriptor(), FocusMode.ARRANGER.getDescriptor()},
            recordFocusMode.getDescriptor());
        recordButtonAssignment.addValueObserver(value -> {
            recordFocusMode = FocusMode.toMode(value);
        });
    }
    
    private void initGlobalNavigation(final Context diContext) {
        final SlMk3HardwareElements hwElements = diContext.getService(SlMk3HardwareElements.class);
        final ViewControl viewControl = diContext.getService(ViewControl.class);
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        cursorTrack.position().markInterested();
        
        final RgbButton trackLeft = hwElements.getButton(CcAssignment.TRACK_LEFT);
        final RgbButton trackRight = hwElements.getButton(CcAssignment.TRACK_RIGHT);
        trackRight.bindLight(mainLayer, () -> cursorTrack.hasNext().get() ? SlRgbState.BLUE : SlRgbState.OFF);
        trackLeft.bindLight(mainLayer, () -> cursorTrack.hasPrevious().get() ? SlRgbState.BLUE : SlRgbState.OFF);
        
        trackRight.bindRepeatHold(mainLayer, () -> cursorTrack.selectNext(), REPEAT_DELAY, REPEAT_FREQUENCY);
        trackLeft.bindRepeatHold(mainLayer, () -> cursorTrack.selectPrevious(), REPEAT_DELAY, REPEAT_FREQUENCY);
        trackRight.bindRepeatHold(shiftLayer, () -> moveBy8Next(cursorTrack), REPEAT_DELAY, REPEAT_FREQUENCY);
        trackLeft.bindRepeatHold(shiftLayer, () -> moveBy8Previous(cursorTrack), REPEAT_DELAY, REPEAT_FREQUENCY);
    }
    
    private void moveBy8Next(final CursorTrack cursorTrack) {
        for (int i = 0; i < 8; i++) {
            cursorTrack.selectNext();
        }
    }
    
    private void moveBy8Previous(final CursorTrack cursorTrack) {
        for (int i = 0; i < 8; i++) {
            cursorTrack.selectPrevious();
        }
    }
    
    private void initGridMode(final Context diContext) {
        final SlMk3HardwareElements hwElements = diContext.getService(SlMk3HardwareElements.class);
        final RgbButton gridButton = hwElements.getButton(CcAssignment.GRID);
        gridButton.bindLight(mainLayer, this::getGridColor);
        gridButton.bindTimedPressRelease(mainLayer, this::handleGridPressed, this::handleGridReleased);
        gridButton.bindIsPressed(mainLayer, globalStates.getGridModeHeld());
        final RgbButton optionsButton = hwElements.getButton(CcAssignment.OPTIONS);
        optionsButton.bindLight(mainLayer, this::getOptionColor);
        optionsButton.bindTimedPressRelease(mainLayer, this::handleOptionsPressed, this::handleOptionsReleased);
    }
    
    private SlRgbState getOptionColor() {
        if (globalStates.getBaseMode().get() == GridMode.OPTION) {
            return SlRgbState.WHITE;
        }
        if (globalStates.getBaseMode().get() == GridMode.OPTION_SHIFT) {
            return SlRgbState.ORANGE;
        }
        return SlRgbState.OFF;
    }
    
    private boolean justEnteredMode = true;
    
    private void handleGridPressed() {
        final BufferedObservableValue<GridMode> gridMode = globalStates.getBaseMode();
        final GridMode incomingMode = gridMode.get();
        if (globalStates.getShiftState().get()) {
            switch (gridMode.get()) {
                case LAUNCH -> gridMode.set(GridMode.SEQUENCER);
                case PAD -> {
                    gridMode.stash();
                    gridMode.set(GridMode.SEQUENCER);
                }
                case SEQUENCER -> {
                    gridMode.stash();
                    gridMode.set(GridMode.PAD);
                }
            }
        } else if (gridMode.get() == GridMode.LAUNCH) {
            gridMode.set(GridMode.PAD);
            gridMode.clearStash();
        } else if (gridMode.get() == GridMode.SEQUENCER) {
            gridMode.stash();
            gridMode.set(GridMode.LAUNCH);
        } else if (gridMode.get() == GridMode.PAD) {
            gridMode.stash();
            gridMode.set(GridMode.LAUNCH);
        }
        justEnteredMode = incomingMode == GridMode.LAUNCH;
    }
    
    private void handleGridReleased(final int diff) {
        final BufferedObservableValue<GridMode> baseMode = globalStates.getBaseMode();
        if (diff > 200) {
            if (baseMode.getStashedValue() == GridMode.SEQUENCER || baseMode.getStashedValue() == GridMode.PAD) {
                baseMode.restoreFromStash();
            } else {
                baseMode.set(GridMode.LAUNCH);
            }
        } else if (!justEnteredMode) {
            baseMode.set(GridMode.LAUNCH);
            justEnteredMode = false;
        }
    }
    
    public void handleOptionsPressed() {
        final BufferedObservableValue<GridMode> baseMode = globalStates.getBaseMode();
        if (globalStates.getShiftState().get() && baseMode.get() != GridMode.OPTION_SHIFT) {
            baseMode.set(GridMode.OPTION_SHIFT);
            justEnteredMode = true;
        } else if (baseMode.get() == GridMode.OPTION_SHIFT) {
            globalStates.getBaseMode().restorePrevious();
        } else if (baseMode.get() != GridMode.OPTION) {
            baseMode.set(GridMode.OPTION);
            justEnteredMode = true;
        } else {
            justEnteredMode = false;
        }
    }
    
    public void handleOptionsReleased(final int diff) {
        if (diff > 200) {
            globalStates.getBaseMode().restorePrevious();
        } else if (!justEnteredMode) {
            //globalStates.getBaseMode().set(GridMode.LAUNCH);
            globalStates.getBaseMode().restorePrevious();
            justEnteredMode = false;
        }
    }
    
    private SlRgbState getGridColor() {
        return switch (globalStates.getBaseMode().get()) {
            case PAD -> SlRgbState.GREEN;
            case SEQUENCER -> globalStates.getModeColor().get();
            default -> SlRgbState.OFF;
        };
    }
    
    public void initTransport(final Context diContext) {
        final ViewControl viewControl = diContext.getService(ViewControl.class);
        final SlMk3HardwareElements hwElements = diContext.getService(SlMk3HardwareElements.class);
        final HardwareButton shiftButton = hwElements.getShiftButton();
        final RgbButton playButton = hwElements.getButton(CcAssignment.PLAY);
        final RgbButton stopButton = hwElements.getButton(CcAssignment.STOP);
        final RgbButton loopButton = hwElements.getButton(CcAssignment.LOOP);
        final RgbButton recordButton = hwElements.getButton(CcAssignment.RECORD);
        final RgbButton rewindButton = hwElements.getButton(CcAssignment.REWIND);
        final RgbButton fastForwardButton = hwElements.getButton(CcAssignment.FAST_FORWARD);
        transport.isArrangerRecordEnabled().markInterested();
        transport.isArrangerOverdubEnabled().markInterested();
        transport.isClipLauncherOverdubEnabled().markInterested();
        transport.isClipLauncherAutomationWriteEnabled().markInterested();
        
        mainLayer.bind(shiftButton, shiftButton.pressedAction(), () -> handleShift(true));
        mainLayer.bind(shiftButton, shiftButton.releasedAction(), () -> handleShift(false));
        
        transport.isPlaying().markInterested();
        transport.isArrangerLoopEnabled().markInterested();
        transport.isArrangerRecordEnabled().markInterested();
        transport.isClipLauncherOverdubEnabled().markInterested();
        
        playButton.bindLight(mainLayer, () -> transport.isPlaying().get() ? SlRgbState.GREEN : SlRgbState.GREEN_DIM);
        playButton.bindPressed(mainLayer, () -> handleRestartPlay(transport));
        playButton.bindLight(shiftLayer, () -> transport.isPlaying().get() ? SlRgbState.GREEN : SlRgbState.GREEN_DIM);
        playButton.bind(shiftLayer, transport.playAction());
        
        stopButton.bindLight(mainLayer, () -> transport.isPlaying().get() ? SlRgbState.WHITE : SlRgbState.WHITE_DIM);
        stopButton.bindPressed(mainLayer, () -> transport.stop());
        stopButton.bindPressed(shiftLayer, () -> viewControl.getRootTrack().stop());
        
        recordButton.bindLight(mainLayer, this::recordButtonState);
        recordButton.bindPressed(mainLayer, this::handleRecordPressed);
        
        recordButton.bindLight(shiftLayer, this::recordButtonStateShift);
        recordButton.bindPressed(shiftLayer, this::handleRecordShiftPressed);
        
        rewindButton.bindLightOnPressed(mainLayer, SlRgbState.WHITE, SlRgbState.WHITE_DIM);
        rewindButton.bindRepeatHold(mainLayer, () -> transport.rewind(), 400, 50);
        fastForwardButton.bindRepeatHold(mainLayer, () -> transport.fastForward(), 400, 50);
        fastForwardButton.bindLightOnPressed(mainLayer, SlRgbState.WHITE, SlRgbState.WHITE_DIM);
        loopButton.bindLight(
            mainLayer, () -> transport.isArrangerLoopEnabled().get() ? SlRgbState.BLUE : SlRgbState.BLUE_DIM);
        loopButton.bindPressed(mainLayer, () -> transport.isArrangerLoopEnabled().toggle());
        
        final RgbButton duplicateButton = hwElements.getButton(CcAssignment.DUPLICATE);
        duplicateButton.bindLightOnPressed(mainLayer, SlRgbState.YELLOW, SlRgbState.YELLOW.reduced(20));
        duplicateButton.bind(mainLayer, this.globalStates.getDuplicateState());
        final RgbButton clearButton = hwElements.getButton(CcAssignment.CLEAR);
        clearButton.bindLightOnPressed(mainLayer, SlRgbState.YELLOW, SlRgbState.YELLOW.reduced(20));
        clearButton.bind(mainLayer, this.globalStates.getClearState());
    }
    
    private void handleRecordPressed() {
        if (recordFocusMode == FocusMode.ARRANGER) {
            transport.isArrangerRecordEnabled().toggle();
        } else {
            transport.isClipLauncherOverdubEnabled().toggle();
        }
    }
    
    private void handleRecordShiftPressed() {
        if (recordFocusMode == FocusMode.ARRANGER) {
            transport.isArrangerOverdubEnabled().toggle();
        } else {
            transport.isArrangerRecordEnabled().toggle();
        }
    }
    
    private SlRgbState recordButtonState() {
        if (recordFocusMode == FocusMode.ARRANGER) {
            return transport.isArrangerRecordEnabled().get() ? SlRgbState.RED : SlRgbState.RED_DIM;
        } else {
            return transport.isClipLauncherOverdubEnabled().get() ? SlRgbState.RED_CL : SlRgbState.RED_CL_DIM;
        }
    }
    
    private SlRgbState recordButtonStateShift() {
        if (recordFocusMode == FocusMode.ARRANGER) {
            return transport.isArrangerOverdubEnabled().get() ? SlRgbState.RED_CL : SlRgbState.RED_CL_DIM;
        }
        return transport.isArrangerRecordEnabled().get() ? SlRgbState.RED_CL : SlRgbState.RED_CL_DIM;
        
    }
    
    private void handleRestartPlay(final Transport transport) {
        if (transport.isPlaying().get()) {
            transport.stop();
        } else {
            transport.play();
        }
    }
    
    private void handleShift(final boolean active) {
        globalStates.getShiftState().set(active);
        shiftLayer.setIsActive(active);
    }
    
    @Override
    public void exit() {
    }
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
    
}
