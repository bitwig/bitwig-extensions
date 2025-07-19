package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.control.ModeButton;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.definition.AbstractKompleteKontrolExtensionDefinition;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class KompleteKontrolSMk2Extension extends KompleteKontrolExtension {
    
    public KompleteKontrolSMk2Extension(final AbstractKompleteKontrolExtensionDefinition definition,
        final ControllerHost host) {
        super(definition, host, false);
    }
    
    @Override
    public void init() {
        super.init();
        final ControllerHost host = getHost();
        
        midiProcessor.intoDawMode(0x3);
        layers = new Layers(this);
        mainLayer = new Layer(layers, "Main");
        arrangeFocusLayer = new Layer(layers, "ArrangeFocus");
        sessionFocusLayer = new Layer(layers, "SessionFocus");
        navigationLayer = new Layer(layers, "NavigationLayer");
        
        final MidiIn midiIn2 = host.getMidiInPort(1);
        
        final NoteInput noteInput =
            midiIn2.createNoteInput(
                "MIDI", "80????", "90????", "A0????", "D0????", "E0????", "B001??", "B00B??", "B040??", "B042??",
                "B1????");
        noteInput.setShouldConsumeEvents(true);
        
        initTrackBank();
        setUpTransport();
        initJogWheel();
        
        final PinnableCursorDevice cursorDevice = viewControl.getCursorDevice();
        bindMacroControl(cursorDevice, midiIn2);
        doHardwareLayout();
        mainLayer.activate();
        navigationLayer.activate();
    }
    
    @Override
    public void setUpChannelDisplayFeedback(final int index, final Track channel) {
        channel.volume().value().addValueObserver(value -> {
            final byte v = KontrolUtil.toSliderVal(value);
            midiProcessor.sendVolumeValue(index, v);
        });
        channel.pan().value().addValueObserver(value -> {
            final int v = (int) (value * 127);
            midiProcessor.sendPanValue(index, v);
        });
        channel.addVuMeterObserver(
            201, 0, true,
            leftValue -> midiProcessor.updateVuLeft(index, KontrolUtil.LEVEL_DB_LOOKUP[leftValue]));
        channel.addVuMeterObserver(
            201, 1, true,
            rightValue -> midiProcessor.updateVuRight(index, KontrolUtil.LEVEL_DB_LOOKUP[rightValue]));
    }
    
    @Override
    protected void initNavigation() {
        final Clip cursorClip = getHost().createLauncherCursorClip(8, 128);
        final Clip arrangerClip = getHost().createArrangerCursorClip(8, 128);
        
        arrangerClip.exists().markInterested();
        final Track rootTrack = viewControl.getProject().getRootTrackGroup();
        final ClipSceneCursor clipSceneCursor = viewControl.getClipSceneCursor();
        final CursorTrack cursorTrack = clipSceneCursor.getCursorTrack();
        final Application application = viewControl.getApplication();
        final NavigationState navigationState = viewControl.getNavigationState();
        application.panelLayout().addValueObserver(v -> {
            currentLayoutType = LayoutType.toType(v);
            midiProcessor.sendLayoutCommand(currentLayoutType);
            updateChanelLed();
            updateSceneLed();
        });
        
        navigationState.setStateChangeListener(() -> {
            this.updateSceneLed();
            this.updateChanelLed();
        });
        
        final MidiIn midiIn = midiProcessor.getMidiIn();
        final HardwareButton leftNavButton = surface.createHardwareButton("LEFT_NAV_BUTTON");
        leftNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x30, 1));
        
        final HardwareButton rightNavButton = surface.createHardwareButton("RIGHT_NAV_BUTTON");
        rightNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x30, 127));
        
        final HardwareButton upNavButton = surface.createHardwareButton("UP_NAV_BUTTON");
        upNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x32, 127));
        
        final HardwareButton downNavButton = surface.createHardwareButton("DOWN_NAV_BUTTON");
        downNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x32, 1));
        
        mainLayer.bindPressed(leftNavButton, () -> clipSceneCursor.navigateRight(currentLayoutType));
        mainLayer.bindPressed(rightNavButton, () -> clipSceneCursor.navigateLeft(currentLayoutType));
        mainLayer.bindPressed(upNavButton, () -> clipSceneCursor.navigateUp(currentLayoutType));
        mainLayer.bindPressed(downNavButton, () -> clipSceneCursor.navigateDown(currentLayoutType));
        
        cursorClip.exists().markInterested();
        final ModeButton quantizeButton = new ModeButton(midiProcessor, "QUANTIZE_BUTTON", CcAssignment.QUANTIZE);
        sessionFocusLayer.bindPressed(quantizeButton.getHwButton(), () -> cursorClip.quantize(1.0));
        
        sessionFocusLayer.bind(
            () -> cursorTrack.canHoldNoteData().get() && cursorClip.exists().get(),
            quantizeButton.getLed());
        
        arrangeFocusLayer.bindPressed(quantizeButton.getHwButton(), () -> arrangerClip.quantize(1.0));
        arrangeFocusLayer.bind(
            () -> cursorTrack.canHoldNoteData().get() && arrangerClip.exists().get(),
            quantizeButton.getLed());
        
        cursorTrack.canHoldNoteData().markInterested();
        cursorClip.exists().markInterested();
        
        final ModeButton clearButton = new ModeButton(midiProcessor, "CLEAR_BUTTON", CcAssignment.CLEAR);
        sessionFocusLayer.bindPressed(clearButton.getHwButton(), () -> cursorClip.clearSteps());
        sessionFocusLayer.bind(
            () -> cursorTrack.canHoldNoteData().get() && cursorClip.exists().get(),
            clearButton.getLed());
        
        arrangeFocusLayer.bindPressed(clearButton.getHwButton(), () -> arrangerClip.clearSteps());
        arrangeFocusLayer.bind(
            () -> cursorTrack.canHoldNoteData().get() && arrangerClip.exists().get(),
            clearButton.getLed());
        
        clearButton.getLed().isOn()
            .setValueSupplier(() -> cursorTrack.canHoldNoteData().get() && cursorClip.exists().get());
        final ModeButton knobPressed = controlElements.getKnobPressed();
        final ModeButton knobShiftPressed = controlElements.getKnobShiftPressed();
        mainLayer.bindPressed(knobPressed.getHwButton(), () -> clipSceneCursor.launch());
        mainLayer.bindPressed(knobShiftPressed.getHwButton(), () -> handle4DShiftPressed(rootTrack, cursorTrack));
    }
    
    
    private void updateSceneLed() {
        final int sceneValue = viewControl.getNavigationState().getSceneValue();
        switch (currentLayoutType) {
            case LAUNCHER -> midiProcessor.sendLedUpdate(0x32, sceneValue);
            case ARRANGER -> midiProcessor.sendLedUpdate(0x30, sceneValue);
            default -> {
            }
        }
    }
    
    
    private void updateChanelLed() {
        final int trackValue = viewControl.getNavigationState().getTrackValue();
        switch (currentLayoutType) {
            case LAUNCHER -> midiProcessor.sendLedUpdate(0x30, trackValue);
            case ARRANGER -> midiProcessor.sendLedUpdate(0x32, trackValue);
            default -> {
            }
        }
    }
    
    @Override
    public void exit() {
        midiProcessor.exit();
    }
    
    @Override
    public void flush() {
        midiProcessor.doFlush();
    }
    
    
}
