package com.bitwig.extensions.controllers.novation.slmk3.layer;

import java.util.Arrays;
import java.util.List;

import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.novation.slmk3.CcAssignment;
import com.bitwig.extensions.controllers.novation.slmk3.GlobalStates;
import com.bitwig.extensions.controllers.novation.slmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.slmk3.SlMk3HardwareElements;
import com.bitwig.extensions.controllers.novation.slmk3.ViewControl;
import com.bitwig.extensions.controllers.novation.slmk3.bindings.SliderNotificationBinding;
import com.bitwig.extensions.controllers.novation.slmk3.control.RgbButton;
import com.bitwig.extensions.controllers.novation.slmk3.display.ScreenHandler;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.values.TrackType;

@Component
public class TrackControlLayer extends Layer {
    
    private final SlRgbState[] trackColors = new SlRgbState[8];
    private final SlRgbState[] trackStateColors = new SlRgbState[8];
    private final int[] trackLevels = new int[8];
    private final TrackType[] trackTypes = new TrackType[8];
    private SoftButtonMode mode = SoftButtonMode.MUTE_SOLO;
    
    private final Layer muteSoloLayer;
    private final Layer armStopLayer;
    private final Layer monitorXFadeLayer;
    
    private final ScreenHandler screenHandler;
    private int soloHeld = 0;
    
    @Inject
    GlobalStates globalStates;
    
    public TrackControlLayer(final Layers layers, final ViewControl viewControl, final SlMk3HardwareElements hwElements,
        final MidiProcessor midiProcessor, final ScreenHandler screenHandler) {
        super(layers, "FADER_CONTROL");
        muteSoloLayer = new Layer(layers, "MUTE_SOLO_LAYER");
        armStopLayer = new Layer(layers, "ARM_STOP_LAYER");
        monitorXFadeLayer = new Layer(layers, "MONITOR_LAYER");
        this.screenHandler = screenHandler;
        
        Arrays.fill(trackColors, SlRgbState.OFF);
        Arrays.fill(trackStateColors, SlRgbState.OFF);
        final TrackBank trackBank = viewControl.getTrackBank();
        final List<HardwareSlider> sliders = hwElements.getSliders();
        final List<MultiStateHardwareLight> lights = hwElements.getTrackLights();
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final HardwareSlider slider = sliders.get(i);
            final MultiStateHardwareLight light = lights.get(index);
            final Track track = trackBank.getItemAt(i);
            track.color().addValueObserver((r, g, b) -> {
                trackColors[index] = SlRgbState.get(r, g, b);
                trackStateColors[index] = trackColors[index].reduced(trackLevels[index]);
            });
            track.volume().value().addValueObserver(128, value -> applyTrackLevel(index, value));
            this.bind(slider, track.volume());
            this.bindLightState(() -> trackStateColors[index], light);
            this.addBinding(new SliderNotificationBinding(slider, midiProcessor, track.volume(), track.name()));
        }
        
        final RgbButton softDown = hwElements.getButton(CcAssignment.SOFT_DOWN);
        final RgbButton softUp = hwElements.getButton(CcAssignment.SOFT_UP);
        softUp.bindLight(this, () -> mode == SoftButtonMode.MUTE_SOLO ? SlRgbState.OFF : SlRgbState.WHITE);
        softDown.bindLight(this, () -> mode == SoftButtonMode.STOP ? SlRgbState.OFF : SlRgbState.WHITE);
        softUp.bindPressed(this, this::prevMode);
        softDown.bindPressed(this, this::nextMode);
        
        final List<RgbButton> softButtons = hwElements.getSoftButtons();
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final Track track = trackBank.getItemAt(i);
            prepareTrackProperties(track, index);
            
            final RgbButton buttonRow1 = softButtons.get(i);
            final RgbButton buttonRow2 = softButtons.get(i + 8);
            
            buttonRow1.bindLight(muteSoloLayer, () -> getMuteState(track));
            buttonRow1.bindPressed(muteSoloLayer, () -> track.mute().toggle());
            buttonRow2.bindLight(muteSoloLayer, () -> getSoloState(track));
            buttonRow2.bindIsPressed(muteSoloLayer, pressed -> handleSolo(pressed, track));
            
            buttonRow1.bindLight(armStopLayer, () -> getMonitorState(track));
            buttonRow1.bindPressed(armStopLayer, () -> toggleMonitorMode(track));
            buttonRow2.bindLight(armStopLayer, () -> getArmState(index, track));
            buttonRow2.bindPressed(armStopLayer, () -> track.arm().toggle());
            
            buttonRow1.bindLight(monitorXFadeLayer, () -> getStopState(track));
            buttonRow1.bindPressed(monitorXFadeLayer, () -> track.stop());
            buttonRow2.bindDisabled(monitorXFadeLayer);
        }
    }
    
    private void prepareTrackProperties(final Track track, final int index) {
        track.mute().markInterested();
        track.solo().markInterested();
        track.arm().markInterested();
        track.isStopped().markInterested();
        track.isQueuedForStop().markInterested();
        track.monitorMode().markInterested();
        track.crossFadeMode().markInterested();
        track.trackType().addValueObserver(type -> this.trackTypes[index] = TrackType.toType(type));
    }
    
    private void toggleMonitorMode(final Track track) {
        final String monMode = track.monitorMode().get();
        final String newMode = switch (monMode) {
            case "ON" -> "AUTO";
            case "OFF" -> "ON";
            case "AUTO" -> "OFF";
            default -> "";
        };
        track.monitorMode().set(newMode);
        screenHandler.notifyMessage(track.name().get(), "Monitor Mode: %s".formatted(newMode));
    }
    
    private SlRgbState getMonitorState(final Track track) {
        if (track.exists().get()) {
            final String monMode = track.monitorMode().get();
            return switch (monMode) {
                case "ON" -> SlRgbState.ORANGE;
                case "OFF" -> SlRgbState.RED;
                case "AUTO" -> SlRgbState.BLUE;
                default -> SlRgbState.ORANGE;
            };
        }
        return SlRgbState.OFF;
    }
    
    private SlRgbState getXFadeState(final Track track) {
        if (track.exists().get()) {
            final String xFadeMode = track.crossFadeMode().get();
            return switch (xFadeMode) {
                case "A" -> SlRgbState.ORANGE;
                case "B" -> SlRgbState.RED;
                case "AB" -> SlRgbState.BLUE;
                default -> SlRgbState.OFF;
            };
        }
        return SlRgbState.OFF;
    }
    
    private SlRgbState getStopState(final Track track) {
        if (track.exists().get()) {
            if (track.isStopped().get()) {
                return SlRgbState.WHITE;
            }
            if (track.isQueuedForStop().get()) {
                return SlRgbState.WHITE.getBlink();
            }
            return SlRgbState.RED;
        }
        return SlRgbState.OFF;
    }
    
    private void nextMode() {
        if (mode == SoftButtonMode.MUTE_SOLO) {
            this.mode = SoftButtonMode.MONITOR_ARM;
            this.udpateMode();
            screenHandler.setSoftMode(this.mode);
        } else if (mode == SoftButtonMode.MONITOR_ARM) {
            this.mode = SoftButtonMode.STOP;
            this.udpateMode();
            screenHandler.setSoftMode(this.mode);
        }
    }
    
    private void prevMode() {
        if (mode == SoftButtonMode.MONITOR_ARM) {
            this.mode = SoftButtonMode.MUTE_SOLO;
            this.udpateMode();
            screenHandler.setSoftMode(this.mode);
        } else if (mode == SoftButtonMode.STOP) {
            this.mode = SoftButtonMode.MONITOR_ARM;
            this.udpateMode();
            screenHandler.setSoftMode(this.mode);
        }
    }
    
    private void handleSolo(final boolean pressed, final Track track) {
        if (pressed) {
            soloHeld++;
            track.solo().toggle(!globalStates.getShiftState().get() && soloHeld == 1);
        } else {
            if (soloHeld > 0) {
                soloHeld--;
            }
        }
    }
    
    private SlRgbState getMuteState(final Track track) {
        if (track.exists().get()) {
            return track.mute().get() ? SlRgbState.ORANGE : SlRgbState.ORANGE.reduced(20);
        }
        return SlRgbState.OFF;
    }
    
    private SlRgbState getArmState(final int index, final Track track) {
        if (track.exists().get()) {
            if (trackTypes[index].canBeArmed()) {
                return track.arm().get() ? SlRgbState.RED : SlRgbState.RED_DIM;
            }
            return SlRgbState.WHITE_DIM;
        }
        return SlRgbState.OFF;
    }
    
    private SlRgbState getSoloState(final Track track) {
        if (track.exists().get()) {
            return track.solo().get() ? SlRgbState.YELLOW : SlRgbState.YELLOW.reduced(20);
        }
        return SlRgbState.OFF;
    }
    
    private void applyTrackLevel(final int index, final int value) {
        trackLevels[index] = value;
        trackStateColors[index] = trackColors[index].reduced(value);
    }
    
    @Activate
    public void doActivate() {
        this.activate();
        udpateMode();
        screenHandler.setSoftMode(mode);
    }
    
    private void udpateMode() {
        muteSoloLayer.setIsActive(mode == SoftButtonMode.MUTE_SOLO);
        armStopLayer.setIsActive(mode == SoftButtonMode.MONITOR_ARM);
        monitorXFadeLayer.setIsActive(mode == SoftButtonMode.STOP);
    }
    
    
}
