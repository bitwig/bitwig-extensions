package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.layer;

import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Arranger;
import com.bitwig.extension.controller.api.BeatTimeFormatter;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CueMarker;
import com.bitwig.extension.controller.api.CueMarkerBank;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.ScrollbarModel;
import com.bitwig.extension.controller.api.SettableBeatTimeValue;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.CcConstValues;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlXlHwElements;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchViewControl;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.BooleanLightValueBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.FixedLightValueBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.RelativeDisplayControl;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchButton;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchRelativeEncoder;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.DisplayControl;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.RgbColor;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.RgbColorState;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.FocusMode;
import com.bitwig.extensions.framework.values.LayoutType;
import com.bitwig.extensions.framework.values.ValueObject;

@Component
public class TransportHandler {
    
    private final BooleanValueObject shiftState;
    private final CursorTrack cursorTrack;
    private FocusMode focusMode = FocusMode.LAUNCHER;
    private final Transport transport;
    private final LaunchControlXlHwElements hwElements;
    private final LaunchViewControl viewControl;
    private final DisplayControl displayControl;
    private final Application application;
    private final Arranger arranger;
    private IntConsumer trackNavigator;
    
    protected double scrubDistance;
    protected final ScrollbarModel horizontalScrollbarModel;
    private boolean markerPositionChangePending = false;
    private final SceneBank sceneBank;
    protected final Scene focusScene;
    
    protected final ValueObject<LayoutType> panelLayout = new ValueObject<>(LayoutType.ARRANGER);
    private final BeatTimeFormatter formatter;
    protected BasicStringValue zoomValue = new BasicStringValue("");
    protected BasicStringValue zoomVerticalValue = new BasicStringValue("");
    protected BasicStringValue cueMarkerValue = new BasicStringValue("");
    protected CueMarkerBank cueMarkerBank;
    
    
    public TransportHandler(final ControllerHost host, final LaunchControlXlHwElements hwElements,
        final LaunchViewControl viewControl, final Transport transport, final DisplayControl displayControl,
        final Application application) {
        this.transport = transport;
        this.shiftState = hwElements.getShiftState();
        this.viewControl = viewControl;
        this.cursorTrack = viewControl.getCursorTrack();
        this.application = application;
        this.hwElements = hwElements;
        cursorTrack.position().markInterested();
        cursorTrack.channelIndex().markInterested();
        this.displayControl = displayControl;
        this.arranger = host.createArranger();
        
        this.formatter = host.createBeatTimeFormatter(":", 2, 1, 1, 0);
        horizontalScrollbarModel = this.arranger.getHorizontalScrollbarModel();
        horizontalScrollbarModel.getContentPerPixel().addValueObserver(this::handleZoomLevel);
        cueMarkerBank = arranger.createCueMarkerBank(1);
        
        sceneBank = viewControl.getTrackBank().sceneBank();
        focusScene = sceneBank.getScene(0);
        focusScene.name().markInterested();
        sceneBank.setIndication(true);
        
        final TrackBank trackBank = viewControl.getTrackBank();
        trackBank.itemCount().addValueObserver(count -> {
        });
        final DocumentState documentState = host.getDocumentState();
        final SettableEnumValue focusMode = documentState.getEnumSetting(
            "Focus", //
            "Recording/Automation",
            new String[] {FocusMode.LAUNCHER.getDescriptor(), FocusMode.ARRANGER.getDescriptor()},
            FocusMode.ARRANGER.getDescriptor());
        focusMode.addValueObserver(mode -> this.focusMode = FocusMode.toMode(mode));
        configureZoomAndMarkers();
        application.panelLayout().addValueObserver(layout -> this.panelLayout.set(LayoutType.toType(layout)));
    }
    
    public Transport getTransport() {
        return transport;
    }
    
    public void bindTransport(final Layer layer) {
        final LaunchButton playButton = hwElements.getButton(CcConstValues.PLAY);
        final LaunchButton recButton = hwElements.getButton(CcConstValues.RECORD);
        transport.isPlaying().markInterested();
        transport.isClipLauncherOverdubEnabled().markInterested();
        transport.isArrangerRecordEnabled().markInterested();
        playButton.bindLight(layer, this::getPlayState);
        playButton.bindPressed(layer, this::handlePlayPressed);
        recButton.bindLight(layer, this::getRecordState);
        recButton.bindPressed(layer, this::handleRecordPressed);
    }
    
    
    public void bindControl(final Layer specLayer, final LaunchControlXlHwElements hwElements, final int rowIndex) {
        final LaunchRelativeEncoder playbackPosEncoder = hwElements.getRelativeEncoder(rowIndex, 0);
        final LaunchRelativeEncoder loopStartEncoder = hwElements.getRelativeEncoder(rowIndex, 3);
        final LaunchRelativeEncoder looEndEncoder = hwElements.getRelativeEncoder(rowIndex, 4);
        final SettableBeatTimeValue playPosition = transport.getPosition();
        bindPosition(specLayer, playPosition, playbackPosEncoder, "PlaybackPosition", false, true, RgbColor.BLUE_LOW);
        
        bindPosition(
            specLayer, transport.arrangerLoopStart(), loopStartEncoder, "Loop Start", true, false, RgbColor.BLUE_LOW);
        bindPosition(
            specLayer, transport.arrangerLoopDuration(), looEndEncoder, "Loop Duration", true, false,
            RgbColor.BLUE_LOW);
        
        final BasicStringValue loopActive = new BasicStringValue("On");
        transport.isArrangerLoopEnabled().addValueObserver(active -> loopActive.set(active ? "ON" : "OFF"));
        final LaunchRelativeEncoder loopActiveEncoder = hwElements.getRelativeEncoder(rowIndex, 5);
        final RelativeDisplayControl loopControl =
            new RelativeDisplayControl(
                loopActiveEncoder.getTargetId(), displayControl, "Transport", "Loop", loopActive,
                inc -> this.handleLoopOnOff(transport, inc));
        loopActiveEncoder.bindIncrementAction(specLayer, loopControl::handleInc);
        specLayer.addBinding(loopControl);
        specLayer.addBinding(
            new BooleanLightValueBinding(
                loopActiveEncoder.getLight(), transport.isArrangerLoopEnabled(),
                RgbColor.BLUE_LOW, RgbColor.BLUE_DIM));
        
        final LaunchRelativeEncoder markerSelectionEncoder = hwElements.getRelativeEncoder(rowIndex, 6);
        specLayer.addBinding(new FixedLightValueBinding(markerSelectionEncoder.getLight(), RgbColor.YELLOW));
        final RelativeDisplayControl cueMarkerControl = new RelativeDisplayControl(
            markerSelectionEncoder.getTargetId(), displayControl, "Transport", "Marker Select", cueMarkerValue,
            this::handleCuePointSelection,
            () -> markerSelectionEncoder.setEncoderBehavior(LaunchRelativeEncoder.EncoderMode.ACCELERATED, 48));
        markerSelectionEncoder.bindIncrementAction(specLayer, cueMarkerControl::handleInc);
        specLayer.addBinding(cueMarkerControl);
        
        final LaunchRelativeEncoder tempoEncoder = hwElements.getRelativeEncoder(rowIndex, 7);
        specLayer.addBinding(new FixedLightValueBinding(tempoEncoder.getLight(), RgbColor.BLUE_LOW));
        final RelativeDisplayControl tempoControl =
            new RelativeDisplayControl(
                tempoEncoder.getTargetId(), displayControl, "Transport", "Tempo", transport.tempo().displayedValue(),
                inc -> transport.tempo().incRaw(inc));
        tempoEncoder.bindIncrementAction(specLayer, tempoControl::handleInc);
        specLayer.addBinding(tempoControl);
    }
    
    public void bindArrangerLayoutControl(final Layer layer, final LaunchControlXlHwElements hwElements,
        final int rowIndex) {
        final LaunchRelativeEncoder zoomHorizontalEncoder = hwElements.getRelativeEncoder(rowIndex, 1);
        final LaunchRelativeEncoder zoomVerticalEncoder = hwElements.getRelativeEncoder(rowIndex, 2);
        
        layer.addBinding(new FixedLightValueBinding(zoomHorizontalEncoder.getLight(), RgbColor.LOW_WHITE));
        final RelativeDisplayControl zoomArrangerControl = getHorizontalZoomControl(zoomHorizontalEncoder);
        zoomHorizontalEncoder.bindIncrementAction(layer, zoomArrangerControl::handleInc);
        layer.addBinding(zoomArrangerControl);
        
        layer.addBinding(new FixedLightValueBinding(zoomVerticalEncoder.getLight(), RgbColor.LOW_WHITE));
        setUpVerticalZoomEncoder(layer, zoomVerticalEncoder);
    }
    
    public void bindLauncherLayoutControl(final Layer layer, final LaunchControlXlHwElements hwElements,
        final int rowIndex) {
        final LaunchRelativeEncoder zoomHorizontalEncoder = hwElements.getRelativeEncoder(rowIndex, 1);
        final LaunchRelativeEncoder zoomVerticalEncoder = hwElements.getRelativeEncoder(rowIndex, 2);
        
        layer.addBinding(new FixedLightValueBinding(zoomHorizontalEncoder.getLight(), RgbColor.ORANGE));
        final RelativeDisplayControl trackScrollView =
            new RelativeDisplayControl(
                zoomHorizontalEncoder.getTargetId(), displayControl, "Transport", "Select Track", cursorTrack.name(),
                this::navigateTracks);
        zoomHorizontalEncoder.bindIncrementAction(layer, trackScrollView::handleInc);
        layer.addBinding(trackScrollView);
        
        // This is scene select
        layer.addBinding(new FixedLightValueBinding(zoomVerticalEncoder.getLight(), RgbColor.BLUE_LOW));
        setUpSceneEncoder(layer, zoomVerticalEncoder);
    }
    
    public void setTrackNavigation(final IntConsumer trackNavigator) {
        this.trackNavigator = trackNavigator;
    }
    
    private void navigateTracks(final int dir) {
        if (this.trackNavigator != null) {
            this.trackNavigator.accept(dir);
        }
    }
    
    public void bindTrackNavigation(final Layer layer) {
        final LaunchButton trackLeftButton = hwElements.getButton(CcConstValues.TRACK_LEFT);
        final LaunchButton trackRightButton = hwElements.getButton(CcConstValues.TRACK_RIGHT);
        
        trackLeftButton.bindLight(layer, () -> canNavLeft(cursorTrack) ? RgbState.WHITE : RgbState.OFF);
        trackRightButton.bindLight(layer, () -> canNavRight(cursorTrack) ? RgbState.WHITE : RgbState.OFF);
        
        trackRightButton.bindRepeatHold(layer, this::navRight);
        trackLeftButton.bindRepeatHold(layer, this::navLeft);
    }
    
    private void handlePlayPressed() {
        if (shiftState.get()) {
            transport.continuePlayback();
        } else {
            transport.play();
        }
    }
    
    private void handleRecordPressed() {
        if (focusMode == FocusMode.LAUNCHER) {
            transport.isClipLauncherOverdubEnabled().toggle();
        } else {
            transport.isArrangerRecordEnabled().toggle();
        }
    }
    
    private RgbColorState getPlayState() {
        return transport.isPlaying().get() ? RgbColorState.GREEN_FULL : RgbColorState.GREEN_DIM;
    }
    
    private RgbColorState getRecordState() {
        if (focusMode == FocusMode.LAUNCHER) {
            return transport.isClipLauncherOverdubEnabled().get()
                ? RgbColorState.RED_ORANGE_FULL
                : RgbColorState.RED_ORANGE_DIM;
        }
        return transport.isArrangerRecordEnabled().get() ? RgbColorState.RED_FULL : RgbColorState.RED_DIM;
    }
    
    public boolean canNavLeft(final CursorTrack cursorTrack) {
        return cursorTrack.hasPrevious().get();
    }
    
    public boolean canNavRight(final CursorTrack cursorTrack) {
        if (shiftState.get()) {
            return viewControl.canScrollBy(8);
        }
        return cursorTrack.hasNext().get();
    }
    
    public void navRight() {
        if (shiftState.get()) {
            viewControl.navigateCursorBy(8);
        } else {
            viewControl.navigateCursorBy(1);
        }
    }
    
    public void navLeft() {
        if (shiftState.get()) {
            viewControl.navigateCursorBy(-8);
        } else {
            viewControl.navigateCursorBy(-1);
        }
    }
    
    public ValueObject<LayoutType> getPanelLayout() {
        return panelLayout;
    }
    
    private void configureZoomAndMarkers() {
        this.panelLayout.addValueObserver(this::handlePanelLayoutUpdate);
        cursorTrack.name().addValueObserver(this::handleCursorTrackNameUpdate);
        final CueMarker marker = cueMarkerBank.getItemAt(0);
        marker.position().markInterested();
        marker.exists().addValueObserver(exists -> updateCueMarker(cueMarkerValue, marker.name().get(), exists));
        marker.name().addValueObserver(name -> updateCueMarker(cueMarkerValue, name, marker.exists().get()));
        marker.position().addValueObserver(this::updateMarkerPosition);
    }
    
    protected void handlePanelLayoutUpdate(final LayoutType newValue) {
        if (newValue == LayoutType.LAUNCHER) {
            zoomValue.set(cursorTrack.name().get());
            zoomVerticalValue.set(focusScene.name().get());
        }
    }
    
    protected void bindPosition(final Layer layer, final SettableBeatTimeValue position,
        final LaunchRelativeEncoder encoder, final String label, final boolean hasMinimum, final boolean deceleration,
        final RgbColor color) {
        
        final BasicStringValue transportPosition = new BasicStringValue("");
        position.addValueObserver(value -> transportPosition.set(position.getFormatted(formatter)));
        
        final IntConsumer valueModifier = hasMinimum ? inc -> incPositionBounded(position, inc * 4.0) : inc -> {
            handlePositionIncrementWithFocus(position, inc);
        };
        final RelativeDisplayControl positionControl =
            new RelativeDisplayControl(
                encoder.getTargetId(), displayControl, "Transport", label, transportPosition,
                valueModifier);
        
        encoder.bindIncrementAction(layer, positionControl::handleInc);
        layer.addBinding(positionControl);
        layer.addBinding(new FixedLightValueBinding(encoder.getLight(), color));
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
    
    
    private void handlePositionIncrementWithFocus(final SettableBeatTimeValue position, final int inc) {
        final double newPos = position.get() + (inc * scrubDistance);
        horizontalScrollbarModel.zoomAtPosition(newPos, 0);
        position.set(newPos);
    }
    
    private void incPositionBounded(final SettableBeatTimeValue position, final double inc) {
        final double newValue = position.get() + inc;
        if (newValue >= 0.0) {
            position.set(newValue);
        } else {
            position.set(0.0);
        }
    }
    
    private void updateCueMarker(final BasicStringValue cueMarkerValue, final String name, final boolean exists) {
        if (exists) {
            cueMarkerValue.set("Marker: %s".formatted(name));
        } else {
            cueMarkerValue.set("No Markers");
        }
    }
    
    protected void handleLoopOnOff(final Transport transport, final int inc) {
        transport.isArrangerLoopEnabled().set(inc > 0);
    }
    
    protected void handleCuePointSelection(final int inc) {
        if (inc < 0) {
            cueMarkerBank.scrollBackwards();
        } else {
            cueMarkerBank.scrollForwards();
        }
        markerPositionChangePending = true;
    }
    
    protected void handleHorizontalZoom(final BasicStringValue zoomValue, final int inc) {
        if (this.panelLayout.get() == LayoutType.ARRANGER) {
            final double newPos = transport.getPosition().get();
            if (inc > 0) {
                zoomValue.set("In");
                horizontalScrollbarModel.zoomAtPosition(newPos, 0.25);
            } else {
                zoomValue.set("Out");
                horizontalScrollbarModel.zoomAtPosition(newPos, -0.25);
            }
        } else {
            if (inc > 0) {
                cursorTrack.selectNext();
            } else {
                cursorTrack.selectPrevious();
            }
        }
    }
    
    protected void handleVerticalZoom(final BasicStringValue zoomVerticalValue, final int inc) {
        if (inc > 0) {
            zoomVerticalValue.set("Zoom In");
            arranger.zoomInLaneHeightsSelected();
        } else {
            zoomVerticalValue.set("Zoom Out");
            arranger.zoomOutLaneHeightsSelected();
        }
    }
    
    private void updateMarkerPosition(final double pos) {
        if (markerPositionChangePending) {
            markerPositionChangePending = false;
            transport.getPosition().set(pos);
        }
    }
    
    private void handleCursorTrackNameUpdate(final String name) {
        if (this.panelLayout.get() != LayoutType.ARRANGER) {
            zoomValue.set(name);
        }
    }
    
    protected RelativeDisplayControl getHorizontalZoomControl(final LaunchRelativeEncoder encoder) {
        final IncrementDecelerator horizontalZoomIncrementor =
            new IncrementDecelerator(inc -> handleHorizontalZoom(zoomValue, inc), 50);
        final RelativeDisplayControl zoomArrangerControl =
            new RelativeDisplayControl(
                encoder.getTargetId(), displayControl, "Transport", "Zoom Arranger", zoomValue,
                horizontalZoomIncrementor,
                () -> encoder.setEncoderBehavior(LaunchRelativeEncoder.EncoderMode.ACCELERATED, 64));
        return zoomArrangerControl;
    }
    
    protected void setUpVerticalZoomEncoder(final Layer layer, final LaunchRelativeEncoder encoder) {
        final IncrementDecelerator verticalZoomIncrementor =
            new IncrementDecelerator(inc -> handleVerticalZoom(zoomVerticalValue, inc), 60);
        final RelativeDisplayControl zoomVerticalControl = new RelativeDisplayControl(
            encoder.getTargetId(), displayControl, "Transport", "Zoom Tracks", zoomVerticalValue,
            verticalZoomIncrementor,
            () -> encoder.setEncoderBehavior(LaunchRelativeEncoder.EncoderMode.ACCELERATED, 32));
        encoder.bindIncrementAction(layer, zoomVerticalControl::handleInc);
        layer.addBinding(zoomVerticalControl);
    }
    
    protected void setUpSceneEncoder(final Layer layer, final LaunchRelativeEncoder encoder) {
        final IncrementDecelerator verticalZoomIncrementor = new IncrementDecelerator(this::handleSceneSelect, 60);
        final RelativeDisplayControl zoomVerticalControl = new RelativeDisplayControl(
            encoder.getTargetId(), displayControl, "Transport", "Scene Select", focusScene.name(),
            verticalZoomIncrementor,
            () -> encoder.setEncoderBehavior(LaunchRelativeEncoder.EncoderMode.ACCELERATED, 32));
        encoder.bindIncrementAction(layer, zoomVerticalControl::handleInc);
        layer.addBinding(zoomVerticalControl);
    }
    
    private void handleSceneSelect(final int inc) {
        if (inc > 0) {
            sceneBank.scrollForwards();
            focusScene.selectInEditor();
        } else {
            sceneBank.scrollBackwards();
            focusScene.selectInEditor();
        }
    }
    
}
