package com.bitwig.extensions.controllers.allenheath.xonek3.layer;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extensions.controllers.allenheath.xonek3.DeviceHwElements;
import com.bitwig.extensions.controllers.allenheath.xonek3.ViewControl;
import com.bitwig.extensions.controllers.allenheath.xonek3.XoneHwElements;
import com.bitwig.extensions.controllers.allenheath.xonek3.XoneK3GlobalStates;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;

@Component
public class LayerCollection {
    
    private final Map<LayerId, Layer> layerMap = new HashMap();
    
    private final LayerId[] initLayers = {LayerId.MAIN, LayerId.MIXER, LayerId.CLIP_LAUNCHER};
    
    public LayerCollection(final Layers layers, final ViewControl viewControl, final XoneHwElements hwElements,
        final XoneK3GlobalStates globalStates) {
        for (final LayerId layerId : LayerId.values()) {
            switch (layerId) {
                case REMOTES -> layerMap.put(layerId, new RemotesLayer(layers, viewControl, hwElements, globalStates));
                case DJ_EQ -> layerMap.put(layerId, new EqControlLayer(layers, viewControl, hwElements, globalStates));
                case MIXER -> layerMap.put(layerId, new MixerLayer(layers, viewControl, hwElements, globalStates));
                case IND_REMOTES ->
                    layerMap.put(layerId, new SingleRemotesControlLayer(layers, viewControl, hwElements, globalStates));
                default -> layerMap.put(layerId, new Layer(layers, layerId.toString()));
            }
        }
        
        // TODO Consider something when 2 devices in play
        final DeviceHwElements deviceHwElements = hwElements.getDeviceElements(0);
        RemotesLayer.bindStandardRemoteControl(
            layerMap.get(LayerId.TRACK_REMOTES), deviceHwElements,
            viewControl.getTrackRemotes(), null);
        RemotesLayer.bindStandardRemoteControl(
            layerMap.get(LayerId.PROJECT_REMOTES), deviceHwElements,
            viewControl.getProjectRemotes(), null);
        hwElements.disableKnobButtonSectionRightSide(layerMap.get(LayerId.TRACK_REMOTES));
        hwElements.disableKnobButtonSectionRightSide(layerMap.get(LayerId.PROJECT_REMOTES));
        
    }
    
    @Activate
    public void init() {
        for (final LayerId id : initLayers) {
            layerMap.get(id).setIsActive(true);
        }
    }
    
    public Layer getLayer(final LayerId id) {
        return layerMap.get(id);
    }
    
    
    public void setActive(final LayerId layerId, final boolean active) {
        layerMap.get(layerId).setIsActive(active);
    }
}
