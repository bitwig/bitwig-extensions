package com.bitwig.extensions.controllers.akai.mpkmk4;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;

@Component
public class LayerCollection {
    public enum LayerId {
        MAIN,
        CLIP_LAUNCHER,
        SHIFT
    }
    
    private final Map<LayerId, Layer> layerMap = new HashMap();
    private final LayerId[] initLayers = {};
    
    public LayerCollection(final Layers layers) {
        for (final LayerId layerId : LayerId.values()) {
            switch (layerId) {
                default -> layerMap.put(layerId, new Layer(layers, layerId.toString()));
            }
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
    
}
