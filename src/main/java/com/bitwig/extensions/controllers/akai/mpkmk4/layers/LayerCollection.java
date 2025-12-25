package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extensions.controllers.akai.mpkmk4.GlobalStates;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkHwElements;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMk4ControllerExtension;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkViewControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;

@Component
public class LayerCollection {
    public enum LayerId {
        MAIN,
        CLIP_LAUNCHER,
        SHIFT,
        DEVICE_REMOTES,
        PROJECT_REMOTES,
        TRACK_REMOTES,
        NAVIGATION
    }
    
    private final Map<LayerId, Layer> layerMap = new HashMap();
    private final LayerId[] initLayers = { LayerId.NAVIGATION, LayerId.MAIN, LayerId.CLIP_LAUNCHER, LayerId.DEVICE_REMOTES};
    
    public LayerCollection(final Layers layers, MpkHwElements hwElements,
        MpkMidiProcessor midiProcessor, MpkViewControl viewControl, GlobalStates globalStates) {
        
        final RemotesControlLayer deviceControlLayer =
            createDeviceControlLayer(layers, hwElements, midiProcessor, viewControl);
        
        final RemotesLayerHandler remotesControl = new RemotesLayerHandler(deviceControlLayer);
        
        for (final LayerId layerId : LayerId.values()) {
            switch (layerId) {
                case DEVICE_REMOTES -> layerMap.put(layerId, deviceControlLayer);
                case NAVIGATION -> layerMap.put(layerId, createNavigationLayer(layers, remotesControl,
                    hwElements, midiProcessor, viewControl, globalStates));
                default -> layerMap.put(layerId, new Layer(layers, layerId.toString()));
            }
        }
        MpkMk4ControllerExtension.println("  --- Layer Collection built --- ");
    }
    
    private Layer createNavigationLayer(final Layers layers, RemotesLayerHandler remotesControl,
        final MpkHwElements hwElements,
        final MpkMidiProcessor midiProcessor, final MpkViewControl viewControl, GlobalStates globalStates) {
        return new NavigationLayer(layers, hwElements,globalStates, remotesControl, midiProcessor, viewControl);
    }
    
    private RemotesControlLayer createDeviceControlLayer(final Layers layers, final MpkHwElements hwElements,
        final MpkMidiProcessor midiProcessor,
        final MpkViewControl viewControl) {
        final PinnableCursorDevice cursorDevice = viewControl.getCursorDevice();
        return
            new RemotesControlLayer("DEVICE", layers, cursorDevice.name(),
                cursorDevice.createCursorRemoteControlsPage(8), hwElements, midiProcessor);
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
        MpkMk4ControllerExtension.println(" -- INIT --");
        for (final LayerId id : initLayers) {
            layerMap.get(id).setIsActive(true);
        }
    }
    
}
