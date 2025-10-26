package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.layer;

import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Arranger;
import com.bitwig.extension.controller.api.BeatTimeFormatter;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CueMarker;
import com.bitwig.extension.controller.api.CueMarkerBank;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.ScrollbarModel;
import com.bitwig.extension.controller.api.SettableBeatTimeValue;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.CcConstValues;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlXlHwElements;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchViewControl;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.BooleanLightValueBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.FixedLightValueBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.RelativeDisplayControl;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.SegmentDisplayBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchButton;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchRelativeEncoder;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.DisplayControl;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.RgbColor;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.LayoutType;
import com.bitwig.extensions.framework.values.ValueObject;

@Component
public class DawControlLayer extends Layer {
    
    private final BeatTimeFormatter formatter;
    private final PinnableCursorDevice cursorDevice;
    private final BooleanValueObject shiftState;
    private final DisplayControl display;
    private final ValueObject<LayoutType> panelLayout = new ValueObject<>(LayoutType.ARRANGER);
    private boolean markerPositionChangePending = false;
    
    private final Remotes deviceRemotes;
    private final CursorTrack cursorTrack;
    private final Arranger arranger;
    private final SceneBank sceneBank;
    private final Scene focusScene;
    private double scrubDistance;
    private final Layer launcherLayer;
    private final Layer arrangerLayer;
    private final SegmentDisplayBinding selectTrackBinding;
    private final SegmentDisplayBinding deviceDisplayBinding;
    private final TransportHandler transportHandler;
    private final ScrollbarModel horizontalScrollbarModel;
    
    final BasicStringValue zoomValue = new BasicStringValue("");
    final BasicStringValue zoomVerticalValue = new BasicStringValue("");
    final BasicStringValue cueMarkerValue = new BasicStringValue("");
    final BasicStringValue deviceName = new BasicStringValue("");
    final CueMarkerBank cueMarkerBank;
    
    public DawControlLayer(final Layers layers, final ControllerHost host, final LaunchControlXlHwElements hwElements,
        final LaunchViewControl viewControl, final DisplayControl displayControl,
        final TransportHandler transportHandler, final Application application) {
        super(layers, "DAW");
        application.panelLayout().addValueObserver(layout -> this.panelLayout.set(LayoutType.toType(layout)));
        this.launcherLayer = new Layer(layers, "LAUNCHER");
        this.arrangerLayer = new Layer(layers, "ARRANGER");
        this.formatter = host.createBeatTimeFormatter(":", 2, 1, 1, 0);
        this.display = displayControl;
        this.cursorTrack = viewControl.getCursorTrack();
        this.transportHandler = transportHandler;
        sceneBank = viewControl.getTrackBank().sceneBank();
        focusScene = sceneBank.getScene(0);
        sceneBank.setIndication(true);
        cursorDevice = viewControl.getCursorDevice();
        cursorDevice.name().addValueObserver(deviceName::set);
        cursorDevice.hasPrevious().markInterested();
        cursorDevice.hasNext().markInterested();
        shiftState = hwElements.getShiftState();
        deviceRemotes = new Remotes(cursorDevice);
        this.arranger = host.createArranger();
        horizontalScrollbarModel = this.arranger.getHorizontalScrollbarModel();
        horizontalScrollbarModel.getContentPerPixel().addValueObserver(this::handleZoomLevel);
        
        cueMarkerBank = arranger.createCueMarkerBank(1);
        deviceRemotes.bind(this, hwElements, displayControl);
        transportHandler.bindTrackNavigation(this);
        bindNavigation(hwElements);
        bindTransport(hwElements);
        
        deviceDisplayBinding = new SegmentDisplayBinding(
            this.deviceName, deviceRemotes.getDevicePageName(),
            displayControl.getFixedDisplay());
        this.addBinding(deviceDisplayBinding);
        selectTrackBinding =
            new SegmentDisplayBinding("Select Track", cursorTrack.name(), displayControl.getTemporaryDisplay());
        this.addBinding(selectTrackBinding);
        
        configureZoomAndMarkers();
    }
    
    public static double roundToNearestPowerOfTwo(final double value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Value must be greater than zero.");
        }
        final double log2 = Math.log(value) / Math.log(2);
        final double roundedPower = Math.round(log2);
        return Math.pow(2, roundedPower);
    }
    
    private void handleZoomLevel(final double v) {
        if (v <= 0) {
            return;
        }
        this.scrubDistance = roundToNearestPowerOfTwo(80 * v);
    }
    
    private void bindTransport(final LaunchControlXlHwElements hwElements) {
        final LaunchRelativeEncoder playbackPosEncoder = hwElements.getRelativeEncoder(2, 0);
        final LaunchRelativeEncoder loopStartEncoder = hwElements.getRelativeEncoder(2, 3);
        final LaunchRelativeEncoder looEndEncoder = hwElements.getRelativeEncoder(2, 4);
        final Transport transport = transportHandler.getTransport();
        final SettableBeatTimeValue playPosition = transport.getPosition();
        bindPosition(this, playPosition, playbackPosEncoder, "PlaybackPosition", false, true, RgbColor.BLUE_LOW);
        
        bindPosition(
            this, transport.arrangerLoopStart(), loopStartEncoder, "Loop Start", true, false, RgbColor.BLUE_LOW);
        bindPosition(
            this, transport.arrangerLoopDuration(), looEndEncoder, "Loop Duration", true, false, RgbColor.BLUE_LOW);
        
        final LaunchRelativeEncoder zoomHorizontalEncoder = hwElements.getRelativeEncoder(2, 1);
        arrangerLayer.addBinding(new FixedLightValueBinding(zoomHorizontalEncoder.getLight(), RgbColor.LOW_WHITE));
        
        final RelativeDisplayControl zoomArrangerControl = getHorizontalZoomControl(zoomHorizontalEncoder);
        zoomHorizontalEncoder.bindIncrementAction(arrangerLayer, zoomArrangerControl::handleInc);
        this.addBinding(zoomArrangerControl);
        
        launcherLayer.addBinding(new FixedLightValueBinding(zoomHorizontalEncoder.getLight(), RgbColor.ORANGE));
        final RelativeDisplayControl trackScrollView =
            new RelativeDisplayControl(
                zoomHorizontalEncoder.getTargetId(), display, "Transport", "Select Track", cursorTrack.name(),
                this::navigateTracks);
        zoomHorizontalEncoder.bindIncrementAction(launcherLayer, trackScrollView::handleInc);
        launcherLayer.addBinding(trackScrollView);
        
        // This is scene select
        final LaunchRelativeEncoder zoomVerticalEncoder = hwElements.getRelativeEncoder(2, 2);
        
        arrangerLayer.addBinding(new FixedLightValueBinding(zoomVerticalEncoder.getLight(), RgbColor.LOW_WHITE));
        setUpVerticalZoomEncoder(arrangerLayer, zoomVerticalEncoder);
        
        launcherLayer.addBinding(new FixedLightValueBinding(zoomVerticalEncoder.getLight(), RgbColor.BLUE_LOW));
        setUpSceneEncoder(launcherLayer, zoomVerticalEncoder);
        
        final BasicStringValue loopActive = new BasicStringValue("On");
        transport.isArrangerLoopEnabled().addValueObserver(active -> loopActive.set(active ? "ON" : "OFF"));
        final LaunchRelativeEncoder loopActiveEncoder = hwElements.getRelativeEncoder(2, 5);
        final RelativeDisplayControl loopControl =
            new RelativeDisplayControl(
                loopActiveEncoder.getTargetId(), display, "Transport", "Loop", loopActive,
                inc -> this.handleLoopOnOff(transport, inc));
        loopActiveEncoder.bindIncrementAction(this, loopControl::handleInc);
        this.addBinding(loopControl);
        this.addBinding(new BooleanLightValueBinding(
            loopActiveEncoder.getLight(), transport.isArrangerLoopEnabled(),
            RgbColor.BLUE_LOW, RgbColor.BLUE_DIM));
        
        final LaunchRelativeEncoder markerSelectionEncoder = hwElements.getRelativeEncoder(2, 6);
        this.addBinding(new FixedLightValueBinding(markerSelectionEncoder.getLight(), RgbColor.YELLOW));
        final RelativeDisplayControl cueMarkerControl = new RelativeDisplayControl(
            markerSelectionEncoder.getTargetId(), display, "Transport", "Marker Select", cueMarkerValue,
            this::handleCuePointSelection,
            () -> markerSelectionEncoder.setEncoderBehavior(LaunchRelativeEncoder.EncoderMode.ACCELERATED, 48));
        markerSelectionEncoder.bindIncrementAction(this, cueMarkerControl::handleInc);
        this.addBinding(cueMarkerControl);
        
        final LaunchRelativeEncoder tempoEncoder = hwElements.getRelativeEncoder(2, 7);
        this.addBinding(new FixedLightValueBinding(tempoEncoder.getLight(), RgbColor.BLUE_LOW));
        final RelativeDisplayControl tempoControl =
            new RelativeDisplayControl(
                tempoEncoder.getTargetId(), display, "Transport", "Tempo", transport.tempo().displayedValue(),
                inc -> transport.tempo().incRaw(inc));
        tempoEncoder.bindIncrementAction(this, tempoControl::handleInc);
        this.addBinding(tempoControl);
    }
    
    private void navigateTracks(final int inc) {
        if (inc > 0) {
            cursorTrack.selectNext();
        } else {
            cursorTrack.selectPrevious();
        }
        selectTrackBinding.blockUpdate();
        deviceDisplayBinding.blockUpdate();
    }
    
    private void handleLoopOnOff(final Transport transport, final int inc) {
        transport.isArrangerLoopEnabled().set(inc > 0);
    }
    
    private void handleCuePointSelection(final int inc) {
        if (inc < 0) {
            cueMarkerBank.scrollBackwards();
        } else {
            cueMarkerBank.scrollForwards();
        }
        markerPositionChangePending = true;
    }
    
    private RelativeDisplayControl getHorizontalZoomControl(final LaunchRelativeEncoder encoder) {
        final IncrementDecelerator horizontalZoomIncrementor =
            new IncrementDecelerator(inc -> handleHorizontalZoom(zoomValue, inc), 50);
        final RelativeDisplayControl zoomArrangerControl =
            new RelativeDisplayControl(
                encoder.getTargetId(), display, "Transport", "Zoom Arranger", zoomValue, horizontalZoomIncrementor,
                () -> encoder.setEncoderBehavior(LaunchRelativeEncoder.EncoderMode.ACCELERATED, 64));
        return zoomArrangerControl;
    }
    
    private void setUpVerticalZoomEncoder(final Layer layer, final LaunchRelativeEncoder encoder) {
        final IncrementDecelerator verticalZoomIncrementor =
            new IncrementDecelerator(inc -> handleVerticalZoom(zoomVerticalValue, inc), 60);
        final RelativeDisplayControl zoomVerticalControl =
            new RelativeDisplayControl(
                encoder.getTargetId(), display, "Transport", "Zoom Tracks", zoomVerticalValue, verticalZoomIncrementor,
                () -> encoder.setEncoderBehavior(LaunchRelativeEncoder.EncoderMode.ACCELERATED, 32));
        encoder.bindIncrementAction(layer, zoomVerticalControl::handleInc);
        layer.addBinding(zoomVerticalControl);
    }
    
    private void setUpSceneEncoder(final Layer layer, final LaunchRelativeEncoder encoder) {
        final IncrementDecelerator verticalZoomIncrementor = new IncrementDecelerator(this::handleSceneSelect, 60);
        final RelativeDisplayControl zoomVerticalControl =
            new RelativeDisplayControl(
                encoder.getTargetId(), display, "Transport", "Scene Select", focusScene.name(), verticalZoomIncrementor,
                () -> encoder.setEncoderBehavior(LaunchRelativeEncoder.EncoderMode.ACCELERATED, 32));
        encoder.bindIncrementAction(layer, zoomVerticalControl::handleInc);
        layer.addBinding(zoomVerticalControl);
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
    
    private void updateMarkerPosition(final double pos) {
        if (markerPositionChangePending) {
            markerPositionChangePending = false;
            transportHandler.getTransport().getPosition().set(pos);
        }
    }
    
    private void handleCursorTrackNameUpdate(final String name) {
        if (this.panelLayout.get() != LayoutType.ARRANGER) {
            zoomValue.set(name);
        }
    }
    
    private void handlePanelLayoutUpdate(final LayoutType newValue) {
        if (newValue == LayoutType.LAUNCHER) {
            zoomValue.set(cursorTrack.name().get());
            zoomVerticalValue.set(focusScene.name().get());
        }
        if (isActive()) {
            launcherLayer.setIsActive(newValue == LayoutType.LAUNCHER);
            arrangerLayer.setIsActive(newValue == LayoutType.ARRANGER);
        }
    }
    
    private void bindNavigation(final LaunchControlXlHwElements hwElements) {
        final LaunchButton pageUpButton = hwElements.getButton(CcConstValues.PAGE_UP);
        final LaunchButton pageDownButton = hwElements.getButton(CcConstValues.PAGE_DOWN);
        pageUpButton.bindLight(this, this::canPageNavigateBackward);
        pageDownButton.bindLight(this, this::canPageNavigateForward);
        pageUpButton.bindRepeatHold(this, this::navigateBackward);
        pageDownButton.bindRepeatHold(this, this::navigateForward);
    }
    
    private void bindPosition(final Layer layer, final SettableBeatTimeValue position,
        final LaunchRelativeEncoder encoder, final String label, final boolean hasMinimum, final boolean deceleration,
        final RgbColor color) {
        
        final BasicStringValue transportPosition = new BasicStringValue("");
        position.addValueObserver(value -> transportPosition.set(position.getFormatted(formatter)));
        
        final IntConsumer valueModifier = hasMinimum ? inc -> incPositionBounded(position, inc * 4.0) : inc -> {
            handlePositionIncrementWithFocus(position, inc);
        };
        final RelativeDisplayControl positionControl =
            new RelativeDisplayControl(
                encoder.getTargetId(), display, "Transport", label, transportPosition,
                valueModifier);
        encoder.bindIncrementAction(layer, positionControl::handleInc);
        layer.addBinding(positionControl);
        layer.addBinding(new FixedLightValueBinding(encoder.getLight(), color));
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
    
    private void handleHorizontalZoom(final BasicStringValue zoomValue, final int inc) {
        if (this.panelLayout.get() == LayoutType.ARRANGER) {
            final double newPos = transportHandler.getTransport().getPosition().get();
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
    
    private void handleVerticalZoom(final BasicStringValue zoomVerticalValue, final int inc) {
        if (inc > 0) {
            zoomVerticalValue.set("Zoom In");
            arranger.zoomInLaneHeightsSelected();
        } else {
            zoomVerticalValue.set("Zoom Out");
            arranger.zoomOutLaneHeightsSelected();
        }
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
    
    private RgbState canPageNavigateBackward() {
        if (shiftState.get()) {
            return cursorDevice.hasPrevious().get() ? RgbState.DIM_WHITE : RgbState.OFF;
        }
        return deviceRemotes.canGoBack() ? RgbState.DIM_WHITE : RgbState.OFF;
    }
    
    private RgbState canPageNavigateForward() {
        if (shiftState.get()) {
            return cursorDevice.hasNext().get() ? RgbState.DIM_WHITE : RgbState.OFF;
        }
        return deviceRemotes.canGoForward() ? RgbState.DIM_WHITE : RgbState.OFF;
    }
    
    private void navigateBackward() {
        if (shiftState.get()) {
            cursorDevice.selectPrevious();
        } else {
            deviceRemotes.selectPreviousPage();
        }
    }
    
    private void navigateForward() {
        if (shiftState.get()) {
            cursorDevice.selectNext();
        } else {
            deviceRemotes.selectNextPage();
        }
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
        launcherLayer.setIsActive(panelLayout.get() == LayoutType.LAUNCHER);
        arrangerLayer.setIsActive(panelLayout.get() == LayoutType.ARRANGER);
        deviceRemotes.setActive(true);
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        launcherLayer.setIsActive(false);
        arrangerLayer.setIsActive(false);
        deviceRemotes.setActive(false);
    }
}
