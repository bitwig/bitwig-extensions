package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.Arranger;
import com.bitwig.extension.controller.api.BeatTimeFormatter;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CueMarker;
import com.bitwig.extension.controller.api.CueMarkerBank;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.SettableBeatTimeValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.bindings.RelativeDisplayControl;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.bindings.RelativeEncoderBinding;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.bindings.SliderBinding;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.LaunchRelEncoder;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.RgbButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.DisplayControl;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer.OverlayEncoderLayer;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.values.IncrementDecelerator;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.LayoutType;
import com.bitwig.extensions.framework.values.ValueObject;

@Component
public class ControlHandler {
    
    private final DisplayControl display;
    private final Layer sliderLayer;
    private final Layer mainLayer;
    private final Map<EncMode, Layer> layerMap = new HashMap<>();
    private Layer currentLayer;
    private Layer stashedLayer;
    private EncMode mode = EncMode.DEVICE;
    private EncMode lastMixMode = EncMode.VOLUME;
    
    private String trackName = "";
    private String deviceName = "";
    private boolean pendingSendsUpdate = false;
    private final BeatTimeFormatter formatter;
    private final GlobalStates globalStates;
    private final CursorTrack cursorTrack;
    private boolean markerPositionChangePending = false;
    private long trackViewBlocked = -1;
    private final Clip arrangerClip;
    private final SceneBank sceneBank;
    private final Arranger arranger;
    private final ValueObject<LayoutType> panelLayout;
    private final Scene focusScene;
    
    private enum EncMode {
        DEVICE("Plugin", 0x24),
        TRACK_REMOTES("Track Remotes", 0x24),
        PROJECT_REMOTES("Project Remotes", 0x24),
        PAN("Panning", 0x25),
        VOLUME("Volume", 0x25),
        SENDS("Sends", 0x26),
        TRANSPORT("Transport", 0x27),
        CUSTOM("Custom", 0x27);
        private final String title;
        private final int displayId;
        
        EncMode(final String title, final int displayId) {
            this.title = title;
            this.displayId = displayId;
        }
        
        public int getDisplayId() {
            return displayId;
        }
        
        public String getTitle() {
            return title;
        }
    }
    
    public ControlHandler(final Layers layers, final LaunchkeyHwElements hwElements, final ViewControl viewControl,
        final MidiProcessor midiProcessor, final DisplayControl displayControl, final GlobalStates globalStates,
        final Transport transport, final ControllerHost host) {
        sliderLayer = new Layer(layers, "SLIDERS");
        mainLayer = new Layer(layers, "CONTROL_MAIN");
        this.display = displayControl;
        this.globalStates = globalStates;
        this.formatter = host.createBeatTimeFormatter(":", 2, 1, 1, 0);
        arrangerClip = viewControl.getArrangerClip();
        sceneBank = viewControl.getSceneBank();
        this.arranger = host.createArranger();
        this.panelLayout = globalStates.getPanelLayout();
        
        Arrays.stream(EncMode.values())
            .forEach(mode -> layerMap.put(mode, new Layer(layers, "END_%s".formatted(mode))));
        final CursorRemoteControlsPage remotes = viewControl.getPrimaryRemotes();
        final CursorRemoteControlsPage trackRemotes = viewControl.getTrackRemotes();
        final CursorRemoteControlsPage projectRemotes = viewControl.getProjectRemotes();
        final LaunchRelEncoder[] incEncoders = hwElements.getIncEncoders();
        final HardwareSlider[] trackSliders = hwElements.getSliders();
        
        cursorTrack = viewControl.getCursorTrack();
        focusScene = viewControl.getFocusScene();
        setUpTrackControlView(viewControl, displayControl);
        
        final TrackBank trackBank = viewControl.getTrackBank();
        final SendBank sendBank = trackBank.getItemAt(0).sendBank();
        final Send focusSend = sendBank.getItemAt(0);
        focusSend.name().addValueObserver(this::triggerSendsUpdate);
        
        for (int i = 0; i < 8; i++) {
            final HardwareSlider slider = trackSliders[i];
            final Track track = trackBank.getItemAt(i);
            final Send send = track.sendBank().getItemAt(0);
            bindRemote(EncMode.DEVICE, cursorTrack, i, incEncoders[i], remotes);
            bindRemote(EncMode.TRACK_REMOTES, cursorTrack, i, incEncoders[i], trackRemotes);
            bindRemote(EncMode.PROJECT_REMOTES, cursorTrack, i, incEncoders[i], projectRemotes);
            final BasicStringValue fixedVolumeLabel = new BasicStringValue("Volume");
            sliderLayer.addBinding(
                new SliderBinding(i, track.volume(), slider, displayControl, track.name(), fixedVolumeLabel));
            layerMap.get(EncMode.VOLUME).addBinding(
                new RelativeEncoderBinding(i, track.volume(), incEncoders[i], displayControl, track.name(),
                    fixedVolumeLabel));
            final BasicStringValue fixedPanLabel = new BasicStringValue("Pan");
            layerMap.get(EncMode.PAN).addBinding(
                new RelativeEncoderBinding(i, track.pan(), incEncoders[i], displayControl, track.name(),
                    fixedPanLabel));
            layerMap.get(EncMode.SENDS).addBinding(
                new RelativeEncoderBinding(i, send, incEncoders[i], displayControl, track.name(), focusSend.name()));
        }
        
        bindIncremental(hwElements, transport);
        bindRemoteNavigation(remotes, hwElements, viewControl.getDeviceRemotesPages(), globalStates.getShiftState(),
            viewControl.getCursorDevice());
        bindRemoteNavigation(EncMode.TRACK_REMOTES, trackRemotes, hwElements, viewControl.getTrackRemotesPages());
        bindRemoteNavigation(EncMode.PROJECT_REMOTES, projectRemotes, hwElements, viewControl.getProjectRemotesPages());
        
        setupSendNavigation(hwElements, trackBank);
        setupMixerNavigation(hwElements);
        initQuantize(hwElements, viewControl);
        setUpTrackNavigation(hwElements, midiProcessor.isMiniVersion());
        midiProcessor.addModeListener(this::handleModeChange);
        currentLayer = layerMap.get(mode);
    }
    
    private void initQuantize(final LaunchkeyHwElements hwElements, final ViewControl viewControl) {
        final RgbButton quantizeButton = hwElements.getButton(CcAssignments.QUANTIZE);
        
        final Clip mainClip = viewControl.getCursorClip();
        mainClip.clipLauncherSlot().name().markInterested();
        mainClip.exists().markInterested();
        arrangerClip.clipLauncherSlot().name().markInterested();
        arrangerClip.exists().markInterested();
        quantizeButton.bindPressed(mainLayer, () -> this.quantizeClip(mainClip));
        quantizeButton.bindLightPressed(mainLayer, () -> {
            if (globalStates.getShiftState().get()) {
                return arrangerClip.exists().get();
            }
            return mainClip.exists().get();
        });
    }
    
    private void quantizeClip(final Clip clip) {
        if (globalStates.getShiftState().get()) {
            arrangerClip.quantize(1.0);
            if (arrangerClip.exists().get()) {
                display.getTemporaryDisplay().show2Lines("Quantize", "Arranger Clip");
            } else {
                display.getTemporaryDisplay().show2Lines("Quantize", "No Clip");
            }
        } else {
            clip.quantize(1.0);
            if (clip.exists().get()) {
                display.getTemporaryDisplay().show2Lines("Quantize", "Clip");
            } else {
                display.getTemporaryDisplay().show2Lines("Quantize", "No Clip");
            }
        }
    }
    
    private void setUpTrackNavigation(final LaunchkeyHwElements hwElements, final boolean isMini) {
        final RgbButton trackLeftButton = hwElements.getButton(CcAssignments.TRACK_LEFT);
        final RgbButton trackRightButton = hwElements.getButton(CcAssignments.TRACK_RIGHT);
        
        if (isMini) {
            trackRightButton.bindLightPressed(mainLayer, cursorTrack.hasPrevious());
            trackLeftButton.bindLightPressed(mainLayer, cursorTrack.hasNext());
            trackRightButton.bindRepeatHold(mainLayer, this::previousTrack, 400, 50);
            trackLeftButton.bindRepeatHold(mainLayer, this::nextTrack, 400, 50);
        } else {
            trackLeftButton.bindLightPressed(mainLayer, cursorTrack.hasPrevious());
            trackRightButton.bindLightPressed(mainLayer, cursorTrack.hasNext());
            trackLeftButton.bindRepeatHold(mainLayer, this::previousTrack, 400, 50);
            trackRightButton.bindRepeatHold(mainLayer, this::nextTrack, 400, 50);
        }
    }
    
    private void nextTrack() {
        trackViewBlocked = -1;
        cursorTrack.selectNext();
    }
    
    private void previousTrack() {
        trackViewBlocked = -1;
        cursorTrack.selectPrevious();
    }
    
    private void setUpTrackControlView(final ViewControl viewControl, final DisplayControl displayControl) {
        final PinnableCursorDevice cursorDevice = viewControl.getCursorDevice();
        cursorTrack.name().addValueObserver(name -> {
            this.trackName = name;
            displayControl.fixDisplayUpdate(0, name, trackViewBlocked);
        });
        cursorDevice.name().addValueObserver(name -> {
            this.deviceName = name;
            displayControl.fixDisplayUpdate(1, name, trackViewBlocked);
        });
    }
    
    private void bindIncremental(final LaunchkeyHwElements hwElements, final Transport transport) {
        final LaunchRelEncoder[] encoders = hwElements.getIncEncoders();
        final Layer layer = layerMap.get(EncMode.TRANSPORT);
        
        bindPosition(0, layer, transport.getPosition(), encoders[0], "PlaybackPosition", false);
        bindPosition(3, layer, transport.arrangerLoopStart(), encoders[3], "Loop Start", true);
        bindPosition(4, layer, transport.arrangerLoopDuration(), encoders[4], "Loop Duration", true);
        
        final BasicStringValue zoomValue = new BasicStringValue("");
        final BasicStringValue zoomParamName = new BasicStringValue("Zoom Arranger");
        final IncrementDecelerator horizontalZoomIncrementor =
            new IncrementDecelerator(inc -> handleHorizontalZoom(zoomValue, inc), 50);
        final RelativeDisplayControl zoomArrangerControl =
            new RelativeDisplayControl(1, display, "Transport", zoomParamName, zoomValue, horizontalZoomIncrementor,
                () -> encoders[1].setEncoderBehavior(LaunchRelEncoder.EncoderMode.ACCELERATED, 64));
        encoders[1].bindIncrementAction(layer, zoomArrangerControl::handleInc);
        layer.addBinding(zoomArrangerControl);
        
        final BasicStringValue zoomVerticalValue = new BasicStringValue("");
        final BasicStringValue zoomVerticalParamName = new BasicStringValue("Zoom Tracks");
        final IncrementDecelerator verticalZoomIncrementor =
            new IncrementDecelerator(inc -> handleVerticalZoom(zoomVerticalValue, inc), 60);
        final RelativeDisplayControl zoomVerticalControl =
            new RelativeDisplayControl(2, display, "Transport", zoomVerticalParamName, zoomVerticalValue,
                verticalZoomIncrementor,
                () -> encoders[2].setEncoderBehavior(LaunchRelEncoder.EncoderMode.ACCELERATED, 32));
        
        encoders[2].bindIncrementAction(layer, zoomVerticalControl::handleInc);
        layer.addBinding(zoomVerticalControl);
        this.panelLayout.addValueObserver((old, newValue) -> {
            if (newValue == LayoutType.ARRANGER) {
                zoomParamName.set("Zoom Arranger");
                zoomVerticalParamName.set("Zoom Tracks");
            } else {
                zoomParamName.set("Track Select");
                zoomVerticalParamName.set("Scene Select");
                zoomValue.set(cursorTrack.name().get());
                zoomVerticalValue.set(focusScene.name().get());
            }
        });
        cursorTrack.name().addValueObserver(name -> {
            if (this.panelLayout.get() != LayoutType.ARRANGER) {
                zoomValue.set(name);
            }
        });
        focusScene.name().addValueObserver(name -> {
            if (this.panelLayout.get() != LayoutType.ARRANGER) {
                zoomVerticalValue.set(name);
            }
        });
        
        final BasicStringValue cueMarkerValue = new BasicStringValue("");
        final CueMarkerBank cueMarkerBank = arranger.createCueMarkerBank(1);
        final CueMarker marker = cueMarkerBank.getItemAt(0);
        marker.position().markInterested();
        marker.exists().addValueObserver(exists -> updateCueMarker(cueMarkerValue, marker.name().get(), exists));
        marker.name().addValueObserver(name -> updateCueMarker(cueMarkerValue, name, marker.exists().get()));
        marker.position().addValueObserver(pos -> {
            if (markerPositionChangePending) {
                markerPositionChangePending = false;
                transport.getPosition().set(pos);
            }
        });
        final RelativeDisplayControl cueMarkerControl =
            new RelativeDisplayControl(5, display, "Transport", "Marker Select", cueMarkerValue, inc -> {
                if (inc < 0) {
                    cueMarkerBank.scrollBackwards();
                } else {
                    cueMarkerBank.scrollForwards();
                }
                markerPositionChangePending = true;
            }, () -> encoders[5].setEncoderBehavior(LaunchRelEncoder.EncoderMode.ACCELERATED, 48));
        encoders[5].bindIncrementAction(layer, cueMarkerControl::handleInc);
        layer.addBinding(cueMarkerControl);
        
        final RelativeDisplayControl emptyControl =
            new RelativeDisplayControl(6, display, "-", "-", new BasicStringValue("-"), inc -> {
            });
        layer.addBinding(emptyControl);
        
        final RelativeDisplayControl tempoControl =
            new RelativeDisplayControl(7, display, "Transport", "Tempo", transport.tempo().displayedValue(),
                inc -> transport.tempo().incRaw(inc));
        encoders[7].bindIncrementAction(layer, tempoControl::handleInc);
        layer.addBinding(tempoControl);
    }
    
    private void updateCueMarker(final BasicStringValue cueMarkerValue, final String name, final boolean exists) {
        if (exists) {
            cueMarkerValue.set("Marker: %s".formatted(name));
        } else {
            cueMarkerValue.set("No Markers");
        }
    }
    
    private void handleVerticalZoom(final BasicStringValue zoomVerticalValue, final int inc) {
        if (this.panelLayout.get() == LayoutType.ARRANGER) {
            if (inc > 0) {
                zoomVerticalValue.set("Zoom In");
                arranger.zoomInLaneHeightsSelected();
            } else {
                zoomVerticalValue.set("Zoom Out");
                arranger.zoomOutLaneHeightsSelected();
            }
        } else {
            if (inc > 0) {
                sceneBank.scrollForwards();
            } else {
                sceneBank.scrollBackwards();
            }
        }
    }
    
    private void handleHorizontalZoom(final BasicStringValue zoomValue, final int inc) {
        if (this.panelLayout.get() == LayoutType.ARRANGER) {
            if (inc > 0) {
                zoomValue.set("In");
                arranger.zoomIn();
            } else {
                zoomValue.set("Out");
                arranger.zoomOut();
            }
        } else {
            if (inc > 0) {
                trackViewBlocked = System.currentTimeMillis();
                cursorTrack.selectNext();
            } else {
                trackViewBlocked = System.currentTimeMillis();
                cursorTrack.selectPrevious();
            }
        }
    }
    
    private void bindPosition(final int index, final Layer layer, final SettableBeatTimeValue position,
        final LaunchRelEncoder encoder, final String label, final boolean hasMinimum) {
        
        final BasicStringValue transportPosition = new BasicStringValue("");
        position.addValueObserver(value -> transportPosition.set(position.getFormatted(formatter)));
        final IntConsumer incHandler =
            hasMinimum ? inc -> incPositionBounded(position, inc * 4.0) : inc -> position.inc(inc * 4.0);
        final RelativeDisplayControl positionControl =
            new RelativeDisplayControl(index, display, "Transport", label, transportPosition, incHandler);
        encoder.bindIncrementAction(layer, positionControl::handleInc);
        layer.addBinding(positionControl);
    }
    
    private void incPositionBounded(final SettableBeatTimeValue position, final double inc) {
        final double newValue = position.get() + inc;
        if (newValue >= 0.0) {
            position.inc(inc);
        } else {
            position.set(0.0);
        }
    }
    
    private void setupMixerNavigation(final LaunchkeyHwElements hwElements) {
        final RgbButton paramUpButton = hwElements.getButton(CcAssignments.PARAM_UP);
        final RgbButton paramDownButton = hwElements.getButton(CcAssignments.PARAM_DOWN);
        final Layer panLayer = layerMap.get(EncMode.PAN);
        paramUpButton.bindLightPressed(panLayer, () -> true);
        paramDownButton.bindLightPressed(panLayer, () -> false);
        paramDownButton.bindPressed(panLayer, () -> {
        });
        paramUpButton.bindPressed(panLayer, () -> switchToLayerDirect(EncMode.VOLUME, "Mixer"));
        
        final Layer volumeLayer = layerMap.get(EncMode.VOLUME);
        paramUpButton.bindLightPressed(volumeLayer, () -> false);
        paramDownButton.bindLightPressed(volumeLayer, () -> true);
        paramDownButton.bindPressed(volumeLayer, () -> switchToLayerDirect(EncMode.PAN, "Mixer"));
        paramUpButton.bindPressed(volumeLayer, () -> {
        });
    }
    
    private void setupSendNavigation(final LaunchkeyHwElements hwElements, final TrackBank trackBank) {
        final RgbButton paramUpButton = hwElements.getButton(CcAssignments.PARAM_UP);
        final RgbButton paramDownButton = hwElements.getButton(CcAssignments.PARAM_DOWN);
        final SendBank sendBank = trackBank.getItemAt(0).sendBank();
        final Layer sendLayer = layerMap.get(EncMode.SENDS);
        paramUpButton.bindLightPressed(sendLayer, sendBank.canScrollBackwards());
        paramUpButton.bindRepeatHold(sendLayer, () -> {
            scrollSends(-1, trackBank);
            pendingSendsUpdate = true;
        }, 400, 100);
        
        paramDownButton.bindLightPressed(sendLayer, sendBank.canScrollForwards());
        paramDownButton.bindRepeatHold(sendLayer, () -> {
            scrollSends(1, trackBank);
            pendingSendsUpdate = true;
        }, 400, 100);
    }
    
    private void triggerSendsUpdate(final String name) {
        if (pendingSendsUpdate) {
            display.show2Line(trackName, name);
            pendingSendsUpdate = false;
        }
    }
    
    private void scrollSends(final int dir, final TrackBank trackBank) {
        for (int i = 0; i < 8; i++) {
            trackBank.getItemAt(i).sendBank().scrollBy(dir);
        }
    }
    
    private void updateTrackDeviceInfo() {
        display.show2Line(trackName, deviceName);
    }
    
    private void bindRemote(final EncMode mode, final CursorTrack cursorTrack, final int i,
        final LaunchRelEncoder encoder, final CursorRemoteControlsPage remotes) {
        final RemoteControl remote = remotes.getParameter(i);
        final Layer layer = layerMap.get(mode);
        encoder.bind(layer, remote);
        layerMap.get(mode)
            .addBinding(new RelativeEncoderBinding(i, remote, encoder, display, cursorTrack.name(), remote.name()));
    }
    
    private void bindRemoteNavigation(final CursorRemoteControlsPage remotes, final LaunchkeyHwElements hwElements,
        final RemotePageName pageName, final BooleanValue shiftState, final PinnableCursorDevice cursorDevice) {
        final RgbButton paramUpButton = hwElements.getButton(CcAssignments.PARAM_UP);
        final RgbButton paramDownButton = hwElements.getButton(CcAssignments.PARAM_DOWN);
        final Layer layer = layerMap.get(EncMode.DEVICE);
        cursorDevice.hasNext().markInterested();
        cursorDevice.hasPrevious().markInterested();
        remotes.hasPrevious().markInterested();
        remotes.hasNext().markInterested();
        
        paramUpButton.bindLightPressed(layer,
            () -> shiftState.get() ? cursorDevice.hasPrevious().get() : remotes.hasPrevious().get());
        paramUpButton.bindRepeatHold(layer, () -> {
            if (shiftState.get()) {
                cursorDevice.selectPrevious();
                updateTrackDeviceInfo();
            } else {
                remotes.selectPrevious();
                display.show2Line(pageName.getTitle(), pageName.get(-1));
            }
        }, 500, 200);
        
        paramDownButton.bindLightPressed(layer,
            () -> shiftState.get() ? cursorDevice.hasNext().get() : remotes.hasNext().get());
        paramDownButton.bindRepeatHold(layer, () -> {
            if (shiftState.get()) {
                cursorDevice.selectNext(); // Cursor Track Needed
                updateTrackDeviceInfo();
            } else {
                remotes.selectNext();
                display.show2Line(pageName.getTitle(), pageName.get(1));
            }
        }, 500, 200);
    }
    
    private void bindRemoteNavigation(final EncMode mode, final CursorRemoteControlsPage remotes,
        final LaunchkeyHwElements hwElements, final RemotePageName pageName) {
        final RgbButton paramUpButton = hwElements.getButton(CcAssignments.PARAM_UP);
        final RgbButton paramDownButton = hwElements.getButton(CcAssignments.PARAM_DOWN);
        final Layer layer = layerMap.get(mode);
        
        paramUpButton.bindLightPressed(layer, remotes.hasPrevious());
        paramUpButton.bindRepeatHold(layer, () -> {
            remotes.selectPrevious();
            display.show2Line(pageName.getTitle(), pageName.get(-1));
        }, 500, 200);
        
        paramDownButton.bindLightPressed(layer, remotes.hasNext());
        paramDownButton.bindRepeatHold(layer, () -> {
            remotes.selectNext();
            display.show2Line(pageName.getTitle(), pageName.get(1));
        }, 500, 200);
    }
    
    private void handleModeChange(final ModeType modeType, final int id) {
        if (modeType == ModeType.ENCODER) {
            final EncoderMode newEncoderMode = EncoderMode.toMode(id);
            switch (newEncoderMode) {
                case PLUGIN -> handlePluginMode();
                case MIXER -> handleMixerMode();
                case SENDS -> handleSendsMode();
                case TRANSPORT -> handleTransportMode();
                case CUSTOM -> handleCustomMode();
            }
        }
    }
    
    private void handleCustomMode() {
        currentLayer.setIsActive(false);
        this.mode = EncMode.CUSTOM;
    }
    
    private void handleMixerMode() {
        if (this.mode == EncMode.PAN) {
            switchToLayer(EncMode.VOLUME);
            lastMixMode = EncMode.VOLUME;
        } else if (this.mode == EncMode.VOLUME) {
            switchToLayer(EncMode.PAN);
            lastMixMode = EncMode.PAN;
        } else {
            switchToLayer(lastMixMode);
        }
    }
    
    private void handlePluginMode() {
        if (this.mode == EncMode.DEVICE) {
            switchToLayer(EncMode.TRACK_REMOTES);
        } else if (this.mode == EncMode.TRACK_REMOTES) {
            switchToLayer(EncMode.PROJECT_REMOTES);
        } else {
            switchToLayer(EncMode.DEVICE);
        }
    }
    
    private void handleSendsMode() {
        if (this.mode != EncMode.SENDS) {
            switchToLayer(EncMode.SENDS);
        }
    }
    
    private void handleTransportMode() {
        if (this.mode != EncMode.TRANSPORT) {
            switchToLayer(EncMode.TRANSPORT);
        }
    }
    
    public void activateLayer(final OverlayEncoderLayer layer) {
        if (this.currentLayer != layer) {
            this.stashedLayer = this.currentLayer;
            this.currentLayer.setIsActive(false);
            this.currentLayer = layer;
            this.currentLayer.setIsActive(true);
        }
    }
    
    public void releaseLayer(final OverlayEncoderLayer layer) {
        if (this.currentLayer == layer && stashedLayer != null) {
            this.currentLayer.setIsActive(false);
            this.currentLayer = stashedLayer;
            stashedLayer = null;
            this.currentLayer.setIsActive(true);
        }
    }
    
    private void switchToLayerDirect(final EncMode mode, final String title) {
        handleMainDisplay(mode);
        this.currentLayer.setIsActive(false);
        this.mode = mode;
        this.currentLayer = layerMap.get(mode);
        if (mode == EncMode.VOLUME || mode == EncMode.PAN) {
            lastMixMode = mode;
        }
        display.show2Line(title, mode.getTitle());
        this.currentLayer.setIsActive(true);
    }
    
    private void switchToLayer(final EncMode mode) {
        handleMainDisplay(mode);
        this.currentLayer.setIsActive(false);
        this.mode = mode;
        this.currentLayer = layerMap.get(mode);
        display.setText(mode.getDisplayId(), 0, mode.getTitle());
        display.showDisplay(mode.getDisplayId());
        this.currentLayer.setIsActive(true);
    }
    
    private void handleMainDisplay(final EncMode nextMode) {
        if (nextMode == EncMode.TRANSPORT) {
            display.displayParamNames("Transport", "Scrb", "ZmH", "ZmV", "LPS", "LPE", "Cue", "", "BPM");
        } else {
            display.releaseParamState();
        }
    }
    
    public String getTrackName() {
        return trackName;
    }
    
    @Activate
    public void activate() {
        currentLayer.setIsActive(true);
        sliderLayer.setIsActive(true);
        mainLayer.setIsActive(true);
    }
    
}
