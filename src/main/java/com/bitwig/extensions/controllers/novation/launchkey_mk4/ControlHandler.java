package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.controller.api.Arranger;
import com.bitwig.extension.controller.api.BeatTimeFormatter;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CueMarker;
import com.bitwig.extension.controller.api.CueMarkerBank;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.SettableBeatTimeValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.bindings.AbsoluteEncoderBinding;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.bindings.RelativeDisplayControl;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.bindings.SliderBinding;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.LaunchRelEncoder;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.RelAbsEncoder;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.RgbButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.DisplayControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BasicStringValue;

@Component
public class ControlHandler {
    
    private final BasicStringValue fixedVolumeLabel = new BasicStringValue("Volume");
    private final BasicStringValue fixedPanLabel = new BasicStringValue("Pan");
    private final DisplayControl display;
    private final Layer sliderLayer;
    private final Map<EncMode, Layer> layerMap = new HashMap<>();
    private Layer currentLayer;
    private EncMode mode = EncMode.DEVICE;
    
    private String trackName = "";
    private String deviceName = "";
    private String sendName = "";
    private boolean pendingSendsUpdate = false;
    private final BeatTimeFormatter formatter;
    private final GlobalStates globalStates;
    private final CursorTrack cursorTrack;
    private boolean markerPositionChangePending = false;
    
    private enum EncMode {
        DEVICE("Plugin", 0x24),
        TRACK_REMOTES("Track Remotes", 0x24),
        PROJECT_REMOTES("Project Remotes", 0x24),
        PAN("Panning", 0x25),
        VOLUME("Volume", 0x25),
        SENDS("Sends", 0x26),
        TRANSPORT("Transport", 0x27);
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
        this.display = displayControl;
        this.globalStates = globalStates;
        this.formatter = host.createBeatTimeFormatter(":", 2, 1, 1, 0);
        
        Arrays.stream(EncMode.values())
            .forEach(mode -> layerMap.put(mode, new Layer(layers, "END_%s".formatted(mode))));
        final CursorRemoteControlsPage remotes = viewControl.getPrimaryRemotes();
        final CursorRemoteControlsPage trackRemotes = viewControl.getTrackRemotes();
        final CursorRemoteControlsPage projectRemotes = viewControl.getProjectRemotes();
        final RelAbsEncoder[] valueEncoders = hwElements.getValueEncoders();
        final HardwareSlider[] trackSliders = hwElements.getSliders();
        
        cursorTrack = viewControl.getCursorTrack();
        final PinnableCursorDevice cursorDevice = viewControl.getCursorDevice();
        
        cursorTrack.name().addValueObserver(name -> this.trackName = name);
        cursorDevice.name().addValueObserver(name -> this.deviceName = name);
        
        cursorTrack.name().addValueObserver(name -> displayControl.fixDisplayUpdate(0, name));
        cursorDevice.name().addValueObserver(name -> displayControl.fixDisplayUpdate(1, name));
        final TrackBank trackBank = viewControl.getTrackBank();
        final SendBank sendBank = trackBank.getItemAt(0).sendBank();
        final Send focusSend = sendBank.getItemAt(0);
        focusSend.name().addValueObserver(name -> triggerSendsUpdate(name));
        
        for (int i = 0; i < 8; i++) {
            final HardwareSlider slider = trackSliders[i];
            final RelAbsEncoder encoder = valueEncoders[i];
            final Track track = trackBank.getItemAt(i);
            final Send send = track.sendBank().getItemAt(0);
            bindRemote(EncMode.DEVICE, cursorTrack, i, encoder, remotes);
            bindRemote(EncMode.TRACK_REMOTES, cursorTrack, i, encoder, trackRemotes);
            bindRemote(EncMode.PROJECT_REMOTES, cursorTrack, i, encoder, projectRemotes);
            sliderLayer.addBinding(
                new SliderBinding(i, track.volume(), slider, displayControl, track.name(), fixedVolumeLabel));
            layerMap.get(EncMode.VOLUME).addBinding(
                new AbsoluteEncoderBinding(i, track.volume(), encoder, displayControl, track.name(), fixedVolumeLabel));
            layerMap.get(EncMode.PAN).addBinding(
                new AbsoluteEncoderBinding(i, track.pan(), encoder, displayControl, track.name(), fixedPanLabel));
            layerMap.get(EncMode.SENDS).addBinding(
                new AbsoluteEncoderBinding(i, send, encoder, displayControl, track.name(), focusSend.name()));
        }
        bindIncremental(hwElements, transport, host);
        bindRemoteNavigation(EncMode.DEVICE, remotes, hwElements, viewControl.getDeviceRemotesPages(),
            globalStates.getShiftState(), viewControl.getCursorDevice());
        bindRemoteNavigation(EncMode.TRACK_REMOTES, trackRemotes, hwElements, viewControl.getTrackRemotesPages());
        bindRemoteNavigation(EncMode.PROJECT_REMOTES, projectRemotes, hwElements, viewControl.getProjectRemotesPages());
        
        setupSendNavigation(hwElements, trackBank);
        
        midiProcessor.addModeListener(this::handleModeChange);
        currentLayer = layerMap.get(mode);
    }
    
    private void bindIncremental(final LaunchkeyHwElements hwElements, final Transport transport,
        final ControllerHost host) {
        final LaunchRelEncoder[] encoders = hwElements.getIncEncoders();
        final Layer layer = layerMap.get(EncMode.TRANSPORT);
        final Arranger arranger = host.createArranger();
        
        bindPosition(0, layer, transport.getPosition(), encoders[0], "Playback Position");
        bindPosition(3, layer, transport.arrangerLoopStart(), encoders[3], "Loop Start");
        bindPosition(4, layer, transport.arrangerLoopDuration(), encoders[4], "Loop Duration");
        
        final BasicStringValue zoomValue = new BasicStringValue("");
        final RelativeDisplayControl zoomArrangerControl =
            new RelativeDisplayControl(1, display, "Transport", "Zoom Arranger", zoomValue,
                inc -> handleHorizontalZoom(arranger, zoomValue, inc));
        encoders[1].bindIncrementAction(layer, zoomArrangerControl::handleInc);
        encoders[1].setEncoderBehavior(LaunchRelEncoder.EncoderMode.NONACCELERATED, 1);
        layer.addBinding(zoomArrangerControl);
        
        final BasicStringValue zoomVerticalValue = new BasicStringValue("");
        final RelativeDisplayControl zoomVerticalControl =
            new RelativeDisplayControl(2, display, "Transport", "Zoom Tracks", zoomVerticalValue,
                inc -> handleVerticalZoom(arranger, zoomVerticalValue, inc));
        encoders[2].setEncoderBehavior(LaunchRelEncoder.EncoderMode.NONACCELERATED, 1);
        encoders[2].bindIncrementAction(layer, zoomVerticalControl::handleInc);
        layer.addBinding(zoomVerticalControl);
        
        final BasicStringValue cueMarkerValue = new BasicStringValue("");
        final CueMarkerBank cueMarkerBank = arranger.createCueMarkerBank(1);
        final CueMarker marker = cueMarkerBank.getItemAt(0);
        marker.position().markInterested();
        marker.name().addValueObserver(name -> cueMarkerValue.set("Marker: %s".formatted(name)));
        marker.position().addValueObserver(pos -> {
            if (markerPositionChangePending) {
                markerPositionChangePending = false;
                transport.getPosition().set(pos);
            }
        });
        final RelativeDisplayControl cueMarkerControl =
            new RelativeDisplayControl(5, display, "Transport", "Cue Marker", cueMarkerValue, inc -> {
                if (inc < 0) {
                    cueMarkerBank.scrollBackwards();
                } else {
                    cueMarkerBank.scrollForwards();
                }
                markerPositionChangePending = true;
            });
        encoders[5].bindIncrementAction(layer, cueMarkerControl::handleInc);
        encoders[5].setEncoderBehavior(LaunchRelEncoder.EncoderMode.NONACCELERATED, 1);
        layer.addBinding(cueMarkerControl);
        
        final RelativeDisplayControl emptyControl =
            new RelativeDisplayControl(6, display, "", "", new BasicStringValue(""), inc -> {});
        layer.addBinding(emptyControl);
        
        final RelativeDisplayControl tempoControl =
            new RelativeDisplayControl(7, display, "Transport", "Tempo", transport.tempo().displayedValue(),
                inc -> transport.tempo().incRaw(inc));
        encoders[7].bindIncrementAction(layer, tempoControl::handleInc);
        layer.addBinding(tempoControl);
    }
    
    private void handleVerticalZoom(final Arranger arranger, final BasicStringValue zoomVerticalValue, final int inc) {
        if (inc < 0) {
            arranger.zoomInLaneHeightsSelected();
            zoomVerticalValue.set("Zoom In");
        } else {
            arranger.zoomOutLaneHeightsSelected();
            zoomVerticalValue.set("Zoom Out");
        }
    }
    
    private void handleHorizontalZoom(final Arranger arranger, final BasicStringValue zoomValue, final int inc) {
        if (this.globalStates.isArrangeMode()) {
            if (inc < 0) {
                arranger.zoomIn();
                zoomValue.set("In");
            } else {
                arranger.zoomOut();
                zoomValue.set("Out");
            }
        } else {
            if (inc < 0) {
                cursorTrack.selectNext();
                zoomValue.set("Track >>");
            } else {
                cursorTrack.selectPrevious();
                zoomValue.set("Track <<");
            }
        }
    }
    
    private void bindPosition(final int index, final Layer layer, final SettableBeatTimeValue position,
        final LaunchRelEncoder encoder, final String label) {
        
        final BasicStringValue transportPosition = new BasicStringValue("");
        position.addValueObserver(value -> transportPosition.set(position.getFormatted(formatter)));
        final RelativeDisplayControl positionControl =
            new RelativeDisplayControl(index, display, "Transport", label, transportPosition,
                inc -> position.inc(inc * 4.0));
        encoder.bindIncrementAction(layer, positionControl::handleInc);
        layer.addBinding(positionControl);
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
        }, 500, 200);
        
        paramDownButton.bindLightPressed(sendLayer, sendBank.canScrollForwards());
        paramDownButton.bindRepeatHold(sendLayer, () -> {
            scrollSends(1, trackBank);
            pendingSendsUpdate = true;
        }, 500, 200);
    }
    
    private void triggerSendsUpdate(final String name) {
        this.sendName = name;
        if (pendingSendsUpdate) {
            display.show2Line(trackName, sendName);
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
    
    private void bindRemote(final EncMode mode, final CursorTrack cursorTrack, final int i, final RelAbsEncoder encoder,
        final CursorRemoteControlsPage remotes) {
        final RemoteControl remote = remotes.getParameter(i);
        layerMap.get(mode)
            .addBinding(new AbsoluteEncoderBinding(i, remote, encoder, display, cursorTrack.name(), remote.name()));
    }
    
    private void bindRemoteNavigation(final EncMode mode, final CursorRemoteControlsPage remotes,
        final LaunchkeyHwElements hwElements, final RemotePageName pageName, final BooleanValue shiftState,
        final PinnableCursorDevice cursorDevice) {
        final RgbButton paramUpButton = hwElements.getButton(CcAssignments.PARAM_UP);
        final RgbButton paramDownButton = hwElements.getButton(CcAssignments.PARAM_DOWN);
        final Layer layer = layerMap.get(mode);
        cursorDevice.hasNext().markInterested();
        cursorDevice.hasPrevious().markInterested();
        remotes.hasPrevious().markInterested();
        remotes.hasNext().markInterested();
        
        paramUpButton.bindLightPressed(
            layer, () -> shiftState.get() ? cursorDevice.hasPrevious().get() : remotes.hasPrevious().get());
        paramUpButton.bindRepeatHold(layer, () -> {
            if (shiftState.get()) {
                cursorDevice.selectPrevious();
                updateTrackDeviceInfo();
            } else {
                remotes.selectPrevious();
                display.show2Line(pageName.getTitle(), pageName.get(-1));
            }
        }, 500, 200);
        
        paramDownButton.bindLightPressed(
            layer, () -> shiftState.get() ? cursorDevice.hasNext().get() : remotes.hasNext().get());
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
        LaunchkeyMk4Extension.println(" %s %d %s".formatted(modeType, id, EncoderMode.toMode(id)));
        if (modeType == ModeType.ENCODER) {
            final EncoderMode newEncoderMode = EncoderMode.toMode(id);
            switch (newEncoderMode) {
                case PLUGIN -> handlePluginMode();
                case MIXER -> handleMixerMode();
                case SENDS -> handleSendsMode();
                case TRANSPORT -> handleTransportMode();
            }
        }
    }
    
    private void handleMixerMode() {
        if (this.mode == EncMode.PAN) {
            switchToLayer(EncMode.VOLUME);
        } else {
            switchToLayer(EncMode.PAN);
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
    
    
    private void switchToLayer(final EncMode mode) {
        this.currentLayer.setIsActive(false);
        this.mode = mode;
        this.currentLayer = layerMap.get(mode);
        display.setText(mode.getDisplayId(), 0, mode.getTitle());
        display.showDisplay(mode.getDisplayId());
        this.currentLayer.setIsActive(true);
    }
    
    @Activate
    public void activate() {
        currentLayer.setIsActive(true);
        sliderLayer.setIsActive(true);
    }
}
