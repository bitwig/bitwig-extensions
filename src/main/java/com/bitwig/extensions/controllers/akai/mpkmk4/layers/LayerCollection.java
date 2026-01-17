package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extensions.controllers.akai.mpkmk4.GlobalStates;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkFocusClip;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkHwElements;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkViewControl;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkMultiStateButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.ValueObject;

@Component
public class LayerCollection {
    
    private final RemotesControlHandler remotesHandler;
    
    private final Map<LayerId, Layer> layerMap = new HashMap();
    private final LayerId[] initLayers =
        {LayerId.NAVIGATION, LayerId.MAIN, LayerId.CLIP_LAUNCHER, LayerId.DEVICE_REMOTES};
    private final PinnableCursorDevice cursorDevice;
    private final List<MpkMultiStateButton> gridButtons;
    private int currentPadMode = 1;
    private final DrumPadLayer drumPadLayer;
    private final ValueObject<LayerId> padMode = new ValueObject<>(LayerId.CLIP_LAUNCHER);
    
    
    public LayerCollection(final Layers layers, final MpkHwElements hwElements, final MpkMidiProcessor midiProcessor,
        final MpkViewControl viewControl, final GlobalStates globalStates, final MpkFocusClip focusClip) {
        
        cursorDevice = viewControl.getCursorDevice();
        gridButtons = hwElements.getGridButtons();
        
        remotesHandler = new RemotesControlHandler(layers, hwElements, midiProcessor, viewControl);
        final NavigationLayer navigationLayer =
            new NavigationLayer(layers, hwElements, globalStates, this, viewControl, focusClip);
        
        midiProcessor.registerMainDisplay(hwElements.getMainLineDisplay());
        midiProcessor.addModeChangeListener(this::handlePadModeChange);
        midiProcessor.addUpdateListeners(this::handleUpdateNeeded);
        drumPadLayer = new DrumPadLayer(layers, hwElements, this, viewControl, midiProcessor);
        drumPadLayer.init();
        final PadMenuLayer padMenuLayer = new PadMenuLayer(layers, hwElements, this, viewControl, globalStates);
        
        for (final LayerId layerId : LayerId.values()) {
            switch (layerId) {
                case DEVICE_REMOTES -> layerMap.put(layerId, remotesHandler.getDeviceControlLayer());
                case TRACK_REMOTES -> layerMap.put(layerId, remotesHandler.getTrackControlLayer());
                case PROJECT_REMOTES -> layerMap.put(layerId, remotesHandler.getProjectControlLayer());
                case TRACK_CONTROL -> layerMap.put(layerId, remotesHandler.getCursorTrackMixerLayer());
                case MIX_CONTROL -> layerMap.put(layerId, remotesHandler.getMixerLayer());
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
        final LayerId padModeLayerId = padModeToLayerId(mode);
        padMode.set(padModeLayerId);
        final Layer newLayer = get(padModeLayerId);
        newLayer.setIsActive(true);
        this.currentPadMode = mode;
    }
    
    public ValueObject<LayerId> getPadMode() {
        return padMode;
    }
    
    public RemotesControlHandler getRemotesHandler() {
        return remotesHandler;
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
    
    public DrumPadLayer getDrumPadLayer() {
        return drumPadLayer;
    }
    
    
}
