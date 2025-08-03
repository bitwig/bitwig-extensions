package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Arranger;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.ScrollbarModel;
import com.bitwig.extension.controller.api.SettableBeatTimeValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.binding.EncoderParameterBinding;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.control.ModeButton;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.definition.AbstractKompleteKontrolExtensionDefinition;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.device.DeviceControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class KontrolSMk3Extension extends KompleteKontrolExtension {
    
    private ClipSceneCursor clipSceneCursor;
    protected ScrollbarModel horizontalScrollbarModel;
    protected double scrubDistance;
    protected Arranger arranger;
    
    public KontrolSMk3Extension(final AbstractKompleteKontrolExtensionDefinition definition,
        final ControllerHost host) {
        super(definition, host);
    }
    
    @Override
    protected boolean hasDeviceControl() {
        return true;
    }
    
    @Override
    public void init() {
        super.init();
        final ControllerHost host = getHost();
        
        this.arranger = host.createArranger();
        horizontalScrollbarModel = this.arranger.getHorizontalScrollbarModel();
        horizontalScrollbarModel.getContentPerPixel().addValueObserver(this::handleZoomLevel);
        midiProcessor.intoDawMode(0x4);
        layers = new Layers(this);
        mainLayer = new Layer(layers, "Main");
        arrangeFocusLayer = new Layer(layers, "ArrangeFocus");
        sessionFocusLayer = new Layer(layers, "SessionFocus");
        navigationLayer = new Layer(layers, "NavigationLayer");
        clipSceneCursor = viewControl.getClipSceneCursor();
        
        final MidiIn midiIn2 = host.getMidiInPort(1);
        final NoteInput noteInput =
            midiIn2.createNoteInput(
                "MIDI", "80????", "90????", "A0????", "D0????", "E0????", "B001??", "B00B??", "B040??", "B042??",
                "B1????");
        noteInput.setShouldConsumeEvents(true);
        
        initTrackBank();
        setUpTransport();
        
        new DeviceControl(host, midiProcessor, viewControl, layers, controlElements);
        initTempoControl();
        midiProcessor.addTempoListener(this::handleTempoIncoming);
        initScrubZoomControl();
        activateStandardLayers();
    }
    
    protected void initScrubZoomControl() {
        final RelativeHardwareKnob fourDKnob = controlElements.getFourDKnob();
        final RelativeHardwarControlBindable binding = midiProcessor.createIncAction(this::handleFourDInc);
        mainLayer.bind(fourDKnob, binding);
        
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        final RelativeHardwareKnob daw4dKnob = controlElements.getFourDKnobMixer();
        final EncoderParameterBinding volumeBinding = new EncoderParameterBinding(daw4dKnob, cursorTrack.volume());
        mainLayer.addBinding(volumeBinding);
        
        final RelativeHardwareKnob loopKnob = controlElements.getLoopModKnob();
        mainLayer.bind(loopKnob, midiProcessor.createIncAction(this::handleLoop));
    }
    
    private void handleLoop(final int inc) {
        final SettableBeatTimeValue position = controlElements.getShiftHeld().get()
            ? viewControl.getTransport().arrangerLoopDuration()
            : viewControl.getTransport().arrangerLoopStart();
        final double newPos = incrementPosition(position, inc);
        position.set(newPos);
    }
    
    private void handleFourDInc(final int inc) {
        if (controlElements.getShiftHeld().get()) {
            final double newPos = viewControl.getTransport().getPosition().get();
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
        final SettableBeatTimeValue playPosition = viewControl.getTransport().getPosition();
        final double newPos = incrementPosition(playPosition, inc);
        horizontalScrollbarModel.zoomAtPosition(newPos, 0);
        playPosition.set(newPos);
    }
    
    private double incrementPosition(final SettableBeatTimeValue position, final int inc) {
        return Math.round((position.get() + (inc * scrubDistance)) / scrubDistance) * scrubDistance;
    }
    
    private void handleZoomLevel(final double v) {
        if (v <= 0) {
            return;
        }
        this.scrubDistance = KontrolUtil.roundToNearestPowerOfTwo(80 * v);
    }
    
    private void handleTempoIncoming(final double v) {
        viewControl.getTransport().tempo().value().setRaw(v);
    }
    
    private void initTempoControl() {
        viewControl.getTransport().tempo().value().addRawValueObserver(tempo -> midiProcessor.sendTempo(tempo));
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
        final CursorTrack cursorTrack = clipSceneCursor.getCursorTrack();
        final Application application = viewControl.getApplication();
        final NavigationState navigationState = viewControl.getNavigationState();
        application.panelLayout().addValueObserver(this::handleLayoutChange);
        
        controlElements.getLeftNavButton()
            .bind(mainLayer, this::handleLeftNavigation, () -> !navigationState.isSceneNavMode());
        controlElements.getRightNavButton()
            .bind(mainLayer, this::handleRightNavigation, navigationState::canGoTrackRight);
        controlElements.getUpNavButton().bind(mainLayer, this::handleUpNavigation, navigationState::canScrollSceneUp);
        controlElements.getDownNavButton()
            .bind(mainLayer, this::handleDownNavigation, navigationState::canScrollSceneDown);
        
        cursorClip.exists().markInterested();
        final ModeButton quantizeButton = controlElements.getButton(CcAssignment.QUANTIZE);
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
        
        final ModeButton clearButton = controlElements.getButton(CcAssignment.CLEAR);
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
    
    private void handleLeftNavigation() {
        clipSceneCursor.navigateTrackLeft();
    }
    
    private void handleRightNavigation() {
        clipSceneCursor.navigateTrackRight();
    }
    
    private void handleUpNavigation() {
        clipSceneCursor.navigateSceneUp();
    }
    
    private void handleDownNavigation() {
        clipSceneCursor.navigateSceneDown();
    }
    
    private void handleLayoutChange(final String v) {
        currentLayoutType = LayoutType.toType(v);
        midiProcessor.sendLayoutCommand(currentLayoutType);
    }
    
    @Override
    public void flush() {
        midiProcessor.doFlush();
    }
    
}
