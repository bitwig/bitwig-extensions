package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.AbsoluteEncoderBinding;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.RelAbsEncoder;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.SliderBinding;
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
        final MidiProcessor midiProcessor, final DisplayControl displayControl) {
        sliderLayer = new Layer(layers, "SLIDERS");
        
        Arrays.stream(EncMode.values())
            .forEach(mode -> layerMap.put(mode, new Layer(layers, "END_%s".formatted(mode))));
        final CursorRemoteControlsPage remotes = viewControl.getPrimaryRemotes();
        final RelAbsEncoder[] valueEncoders = hwElements.getValueEncoders();
        final HardwareSlider[] trackSliders = hwElements.getSliders();
        
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        final PinnableCursorDevice cursorDevice = viewControl.getCursorDevice();
        this.display = displayControl;
        
        cursorTrack.name().addValueObserver(name -> displayControl.fixDisplayUpdate(0, name));
        cursorDevice.name().addValueObserver(name -> displayControl.fixDisplayUpdate(1, name));
        final TrackBank trackBank = viewControl.getTrackBank();
        
        for (int i = 0; i < 8; i++) {
            final HardwareSlider slider = trackSliders[i];
            final RelAbsEncoder encoder = valueEncoders[i];
            final RemoteControl remote = remotes.getParameter(i);
            final Track track = trackBank.getItemAt(i);
            sliderLayer.addBinding(
                new SliderBinding(i, track.volume(), slider, displayControl, track.name(), fixedVolumeLabel));
            layerMap.get(EncMode.DEVICE).addBinding(
                new AbsoluteEncoderBinding(i, remote, encoder, displayControl, cursorTrack.name(), remote.name()));
            layerMap.get(EncMode.VOLUME).addBinding(
                new AbsoluteEncoderBinding(i, track.volume(), encoder, displayControl, track.name(), fixedVolumeLabel));
            layerMap.get(EncMode.PAN).addBinding(
                new AbsoluteEncoderBinding(i, track.pan(), encoder, displayControl, track.name(), fixedPanLabel));
        }
        
        midiProcessor.addModeListener(this::handleModeChange);
        currentLayer = layerMap.get(mode);
    }
    
    private void handleModeChange(final ModeType modeType, final int id) {
        if (modeType == ModeType.ENCODER) {
            final EncoderMode newEncoderMode = EncoderMode.toMode(id);
            LaunchkeyMk4Extension.println(" MODE Change => %s %s <= %d", modeType, newEncoderMode, id);
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
        if (this.mode == EncMode.SENDS) {
        
        } else {
            switchToLayer(EncMode.SENDS);
        }
    }
    
    private void handleTransportMode() {
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
