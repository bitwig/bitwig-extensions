package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extensions.controllers.akai.mpkmk4.GlobalStates;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkFocusClip;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkHwElements;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkViewControl;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkMultiStateButton;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.RemotesDisplayControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BasicStringValue;

@Component
public class LayerCollection {
    
    public static final int BASIC_ENCODER_MODE_NO = 3;
    private LayerId encoderLayerMode = LayerId.DEVICE_REMOTES;
    
    private final Map<LayerId, Layer> layerMap = new HashMap();
    private final LayerId[] initLayers =
        {LayerId.NAVIGATION, LayerId.MAIN, LayerId.CLIP_LAUNCHER, LayerId.DEVICE_REMOTES};
    private EncoderLayer remotesControlLayer;
    private final PinnableCursorDevice cursorDevice;
    private LayerId lastControlDeviceMode = LayerId.DEVICE_REMOTES;
    private final List<MpkMultiStateButton> gridButtons;
    private int currentPadMode = 1;
    private final MixEncoderLayer mixerLayer;
    private final DrumPadLayer drumPadLayer;
    private final BasicStringValue encoderModeValue = new BasicStringValue();
    private int selectedEncoderMode = 0;
    
    
    public LayerCollection(final Layers layers, final MpkHwElements hwElements, final MpkMidiProcessor midiProcessor,
        final MpkViewControl viewControl, final GlobalStates globalStates, final MpkFocusClip focusClip) {
        
        cursorDevice = viewControl.getCursorDevice();
        gridButtons = hwElements.getGridButtons();
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        
        final RemotesControlLayer deviceControlLayer = new RemotesControlLayer(
            "DEVICE", layers, cursorDevice.name(), cursorDevice.createCursorRemoteControlsPage(8), hwElements,
            midiProcessor);
        final RemotesControlLayer trackControlLayer = new RemotesControlLayer(
            "TRACK", layers, new BasicStringValue("Track Remotes"), cursorTrack.createCursorRemoteControlsPage(8),
            hwElements, midiProcessor);
        final RemotesControlLayer projectControlLayer = new RemotesControlLayer(
            "PROJECT", layers, new BasicStringValue("Project Remotes"),
            viewControl.getRootTrack().createCursorRemoteControlsPage(8), hwElements, midiProcessor);
        remotesControlLayer = deviceControlLayer;
        
        remotesControlLayer.getDisplayControl() //
            .ifPresent(control -> control.setActive(true));
        final NavigationLayer navigationLayer =
            new NavigationLayer(layers, hwElements, globalStates, this, viewControl, focusClip);
        
        midiProcessor.registerMainDisplay(hwElements.getMainLineDisplay());
        final CursorTrackMixLayer cursorTrackMixerLayer =
            new CursorTrackMixLayer(layers, hwElements, midiProcessor, viewControl);
        mixerLayer = new MixEncoderLayer(layers, hwElements, midiProcessor, viewControl);
        midiProcessor.addModeChangeListener(this::handlePadModeChange);
        midiProcessor.addUpdateListeners(this::handleUpdateNeeded);
        drumPadLayer = new DrumPadLayer(layers, hwElements, this, viewControl, midiProcessor);
        drumPadLayer.init();
        final PadMenuLayer padMenuLayer = new PadMenuLayer(layers, hwElements, this, globalStates);
        encoderModeValue.set(encoderModeToString());
        
        for (final LayerId layerId : LayerId.values()) {
            switch (layerId) {
                case DEVICE_REMOTES -> layerMap.put(layerId, deviceControlLayer);
                case TRACK_REMOTES -> layerMap.put(layerId, trackControlLayer);
                case PROJECT_REMOTES -> layerMap.put(layerId, projectControlLayer);
                case TRACK_CONTROL -> layerMap.put(layerId, cursorTrackMixerLayer);
                case MIX_CONTROL -> layerMap.put(layerId, mixerLayer);
                case NAVIGATION -> layerMap.put(layerId, navigationLayer);
                case DRUM_PAD_CONTROL -> layerMap.put(layerId, drumPadLayer);
                case PAD_MENU_LAYER -> layerMap.put(layerId, padMenuLayer);
                default -> layerMap.put(layerId, new Layer(layers, layerId.toString()));
            }
        }
    }
    
    private void handlePadModeChange(final int mode) {
        if (mode == currentPadMode) {
            return;
        }
        final Layer currentLayer = get(padModeToLayerId(currentPadMode));
        currentLayer.setIsActive(false);
        final Layer newLayer = get(padModeToLayerId(mode));
        newLayer.setIsActive(true);
        this.currentPadMode = mode;
    }
    
    public int getCurrentPadMode() {
        return currentPadMode;
    }
    
    private LayerId padModeToLayerId(final int mode) {
        return switch (mode) {
            case 2 -> LayerId.TRACK_PAD_CONTROL;
            case 0 -> LayerId.DRUM_PAD_CONTROL;
            default -> LayerId.CLIP_LAUNCHER;
        };
    }
    
    private void handleUpdateNeeded() {
        for (final MpkMultiStateButton button : gridButtons) {
            button.forceUpdate();
        }
    }
    
    public Layer get(final LayerId layerId) {
        final Layer layer = layerMap.get(layerId);
        if (layer == null) {
            throw new RuntimeException("Layer %s does not exists".formatted(layerId));
        }
        return layer;
    }
    
    @Activate
    public void init() {
        for (final LayerId id : initLayers) {
            layerMap.get(id).setIsActive(true);
        }
    }
    
    public boolean canNavigateLeft() {
        return this.remotesControlLayer.canScrollLeft();
    }
    
    public DrumPadLayer getDrumPadLayer() {
        return drumPadLayer;
    }
    
    public boolean canNavigateRight() {
        return this.remotesControlLayer.canScrollRight();
    }
    
    public void navigateLeft() {
        this.remotesControlLayer.navigateLeft();
    }
    
    public void navigateRight() {
        this.remotesControlLayer.navigateRight();
    }
    
    public void handleShiftEncoderTurn(final int inc) {
        if (encoderLayerMode == LayerId.MIX_CONTROL) {
            mixerLayer.toggleSends();
        } else if (encoderLayerMode == LayerId.TRACK_CONTROL) {
            if (inc > 0) {
                this.cursorDevice.selectNext();
            } else {
                this.cursorDevice.selectPrevious();
            }
        } else {
            selectDevice(inc);
        }
    }
    
    public void selectDevice(final int inc) {
        if (encoderLayerMode == LayerId.DEVICE_REMOTES) {
            if (inc > 0) {
                this.cursorDevice.selectNext();
            } else {
                if (this.cursorDevice.hasPrevious().get()) {
                    this.cursorDevice.selectPrevious();
                } else {
                    setEncoderLayerMode(LayerId.TRACK_REMOTES);
                }
            }
        } else if (encoderLayerMode == LayerId.TRACK_REMOTES) {
            if (inc > 0) {
                setEncoderLayerMode(LayerId.DEVICE_REMOTES);
            } else {
                setEncoderLayerMode(LayerId.PROJECT_REMOTES);
            }
        } else if (encoderLayerMode == LayerId.PROJECT_REMOTES) {
            if (inc > 0) {
                setEncoderLayerMode(LayerId.TRACK_REMOTES);
            }
        }
    }
    
    private String encoderModeToString() {
        return switch (encoderLayerMode) {
            case TRACK_REMOTES, PROJECT_REMOTES, DEVICE_REMOTES -> "Remotes";
            case MIX_CONTROL -> "Mix Grid";
            case TRACK_CONTROL -> "Mix Track";
            default -> "";
        };
    }
    
    public void incrementEncoderMode(final int inc, final boolean roundRobin) {
        final int previousMode = selectedEncoderMode;
        int nextMode = selectedEncoderMode + inc;
        if (roundRobin) {
            nextMode = nextMode % BASIC_ENCODER_MODE_NO;
        } else {
            nextMode = Math.max(0, Math.min(BASIC_ENCODER_MODE_NO - 1, nextMode));
        }
        if (nextMode != previousMode) {
            selectedEncoderMode = nextMode;
            if (selectedEncoderMode == 0) {
                backToDeviceControl();
            } else if (selectedEncoderMode == 1) {
                setEncoderLayerMode(LayerId.TRACK_CONTROL);
            } else if (selectedEncoderMode == 2) {
                setEncoderLayerMode(LayerId.MIX_CONTROL);
            }
            
        }
        
    }
    
    public BasicStringValue getEncoderModeValue() {
        return encoderModeValue;
    }
    
    public void setEncoderLayerMode(final LayerId id) {
        if (id == encoderLayerMode) {
            return;
        }
        if (this.encoderLayerMode.isControlsDevice()) {
            lastControlDeviceMode = this.encoderLayerMode;
        }
        this.encoderLayerMode = id;
        encoderModeValue.set(encoderModeToString());
        final RemotesDisplayControl previous = this.remotesControlLayer.getDisplayControl().orElse(null);
        this.remotesControlLayer.setIsActive(false);
        this.remotesControlLayer = (EncoderLayer) get(id);
        this.remotesControlLayer.setIsActive(true);
        final RemotesDisplayControl current = this.remotesControlLayer.getDisplayControl().orElse(null);
        if (current != null) {
            if (previous != null) {
                previous.setActive(false);
            }
            current.setActive(true);
        }
    }
    
    public void backToDeviceControl() {
        setEncoderLayerMode(lastControlDeviceMode);
    }
}
