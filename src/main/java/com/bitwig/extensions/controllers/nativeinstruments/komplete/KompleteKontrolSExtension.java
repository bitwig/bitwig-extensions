package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Arranger;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.ScrollbarModel;
import com.bitwig.extension.controller.api.SettableBeatTimeValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.definition.AbstractKompleteKontrolExtensionDefinition;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.device.DeviceControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class KompleteKontrolSExtension extends KompleteKontrolExtension {
    
    private static final byte[] levelDbLookup = new byte[201]; // maps level values to align with KK display
    private DeviceControl deviceLayer;
    private Arranger arranger;
    private ScrollbarModel horizontalScrollbarModel;
    private double scrubDistance;
    private SettableBeatTimeValue playPosition;
    
    public KompleteKontrolSExtension(final AbstractKompleteKontrolExtensionDefinition definition,
        final ControllerHost host, final boolean hasDeviceControl) {
        super(definition, host, hasDeviceControl);
    }
    
    @Override
    public void init() {
        initSliderLookup();
        super.init();
        final ControllerHost host = getHost();
        
        midiProcessor.intoDawMode(hasDeviceControl ? 0x4 : 0x3);
        layers = new Layers(this);
        mainLayer = new Layer(layers, "Main");
        arrangeFocusLayer = new Layer(layers, "ArrangeFocus");
        sessionFocusLayer = new Layer(layers, "SessionFocus");
        navigationLayer = new Layer(layers, "NavigationLayer");
        this.arranger = host.createArranger();
        horizontalScrollbarModel = this.arranger.getHorizontalScrollbarModel();
        
        final MidiIn midiIn2 = host.getMidiInPort(1);
        
        final NoteInput noteInput =
            midiIn2.createNoteInput(
                "MIDI", "80????", "90????", "A0????", "D0????", "E0????", "B001??", "B00B??", "B040??", "B042??",
                "B1????");
        noteInput.setShouldConsumeEvents(true);
        
        initTrackBank();
        setUpTransport();
        initJogWheel();
        
        if (hasDeviceControl) {
            deviceLayer = new DeviceControl(host, midiProcessor, viewControl, layers, controlElements);
            initTempoControl();
            midiProcessor.addTempoListener(this::handleTempoIncoming);
            initSmk3Control();
            horizontalScrollbarModel.getContentPerPixel().addValueObserver(this::handleZoomLevel);
        } else {
            final PinnableCursorDevice cursorDevice = viewControl.getCursorDevice();
            bindMacroControl(cursorDevice, midiIn2);
        }
        mainLayer.activate();
        navigationLayer.activate();
    }
    
    private void initSmk3Control() {
        final RelativeHardwareKnob fourDKnob = controlElements.getFourDKnobMixer();
        final RelativeHardwarControlBindable binding = midiProcessor.createIncAction(this::handleFourDInc);
        mainLayer.bind(fourDKnob, binding);
        playPosition = viewControl.getTransport().getPosition();
        playPosition.markInterested();
    }
    
    private void handleFourDInc(final int inc) {
        if (controlElements.getShiftHeld().get()) {
            final double newPos = playPosition.get();
            if (inc > 0) {
                horizontalScrollbarModel.zoomAtPosition(newPos, 0.25);
            } else {
                horizontalScrollbarModel.zoomAtPosition(newPos, -0.25);
            }
        } else {
            handlePositionIncrementWithFocus(inc);
        }
    }
    
    private void handlePositionIncrementWithFocus(final int inc) {
        final double newPos = playPosition.get() + (inc * scrubDistance);
        horizontalScrollbarModel.zoomAtPosition(newPos, 0);
        playPosition.set(newPos);
    }
    
    
    private void handleZoomLevel(final double v) {
        if (v <= 0) {
            return;
        }
        this.scrubDistance = roundToNearestPowerOfTwo(80 * v);
    }
    
    public static double roundToNearestPowerOfTwo(final double value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Value must be greater than zero.");
        }
        final double log2 = Math.log(value) / Math.log(2);
        final double roundedPower = Math.round(log2);
        return Math.pow(2, roundedPower);
    }
    
    
    private void handleTempoIncoming(final double v) {
        viewControl.getTransport().tempo().value().setRaw(v);
    }
    
    private void initTempoControl() {
        viewControl.getTransport().tempo().value().addRawValueObserver(tempo -> midiProcessor.sendTempo(tempo));
    }
    
    private static void initSliderLookup() {
        final int[] intervals = {0, 25, 63, 100, 158, 200};
        final int[] pollValues = {0, 14, 38, 67, 108, 127};
        int currentIv = 1;
        int ivLb = 0;
        int plLb = 0;
        int ivUb = intervals[currentIv];
        int plUb = pollValues[currentIv];
        double ratio = (double) (plUb - plLb) / (double) (ivUb - ivLb);
        for (int i = 0; i < levelDbLookup.length; i++) {
            if (i > intervals[currentIv]) {
                currentIv++;
                ivLb = ivUb;
                plLb = plUb;
                ivUb = intervals[currentIv];
                plUb = pollValues[currentIv];
                ratio = (double) (plUb - plLb) / (double) (ivUb - ivLb);
            }
            levelDbLookup[i] = (byte) Math.round(plLb + (i - ivLb) * ratio);
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
    
    @Override
    public void setUpChannelDisplayFeedback(final int index, final Track channel) {
        channel.volume().value().addValueObserver(value -> {
            final byte v = toSliderVal(value);
            midiProcessor.sendVolumeValue(index, v);
        });
        channel.pan().value().addValueObserver(value -> {
            final int v = (int) (value * 127);
            midiProcessor.sendPanValue(index, v);
        });
        channel.addVuMeterObserver(
            201, 0, true,
            leftValue -> midiProcessor.updateVuLeft(index, levelDbLookup[leftValue]));
        channel.addVuMeterObserver(
            201, 1, true,
            rightValue -> midiProcessor.updateVuRight(index, levelDbLookup[rightValue]));
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
        
        mainLayer.bindPressed(leftNavButton, () -> clipSceneCursor.doNavigateRight(currentLayoutType));
        mainLayer.bindPressed(rightNavButton, () -> clipSceneCursor.doNavigateLeft(currentLayoutType));
        mainLayer.bindPressed(upNavButton, () -> clipSceneCursor.doNavigateUp(currentLayoutType));
        mainLayer.bindPressed(downNavButton, () -> clipSceneCursor.doNavigateDown(currentLayoutType));
        
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
        mainLayer.bindPressed(
            knobShiftPressed.getHwButton(), () -> {
                if (navigationState.isSceneNavMode()) {
                    rootTrack.stop();
                } else {
                    cursorTrack.stop();
                }
            });
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
    
    byte toSliderVal(final double value) {
        return levelDbLookup[(int) (value * 200)];
    }
    
}
