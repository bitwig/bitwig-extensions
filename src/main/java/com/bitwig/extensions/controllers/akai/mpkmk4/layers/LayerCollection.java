package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extensions.controllers.akai.mpkmk4.GlobalStates;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkBaseMode;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkFocusClip;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkHwElements;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMk4ControllerExtension;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkVariant;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkViewControl;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkMultiStateButton;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.LineDisplay;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkColorLookup;
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
    private final List<MpkMultiStateButton> gridButtons;
    private int currentPadMode = 1;
    private final DrumPadLayer drumPadLayer;
    private final ValueObject<LayerId> padMode = new ValueObject<>(LayerId.CLIP_LAUNCHER);
    private final IntFunction<LayerId> padModSelector;
    private final MpkHwElements hwElements;
    
    public LayerCollection(final Layers layers, final MpkHwElements hwElements, final MpkMidiProcessor midiProcessor,
        final MpkViewControl viewControl, final GlobalStates globalStates, final MpkFocusClip focusClip) {
        
        gridButtons = hwElements.getGridButtons();
        padModSelector = globalStates.getVariant().getLayoutStyle() == MpkVariant.LayoutStyle.STANDARD
            ? this::padModeToLayerId
            : this::padModeToLayerIdExpanded;
        
        this.hwElements = hwElements;
        remotesHandler = new RemotesControlHandler(layers, hwElements, midiProcessor, viewControl);
        final NavigationLayer navigationLayer =
            new NavigationLayer(layers, hwElements, globalStates, this, viewControl, focusClip);
        final PluginNavigationLayer pluginNavigationLayer =
            new PluginNavigationLayer(layers, hwElements, globalStates, this, viewControl, focusClip);
        
        midiProcessor.registerMainDisplay(hwElements.getMainLineDisplay());
        midiProcessor.addModeChangeListener(this::handlePadModeChange);
        globalStates.getMpkBaseMode().addValueObserver(this::handleDawPluginModeChange);
        midiProcessor.addUpdateListeners(this::handleUpdateNeeded);
        drumPadLayer = new DrumPadLayer(layers, hwElements, viewControl, midiProcessor, globalStates.getVariant());
        drumPadLayer.init();
        final PadMenuLayer padMenuLayer = new PadMenuLayer(layers, hwElements, this, viewControl, globalStates);
        setTrackControl(viewControl, hwElements.getMainLineDisplay());
        
        for (final LayerId layerId : LayerId.values()) {
            switch (layerId) {
                case DEVICE_REMOTES -> layerMap.put(layerId, remotesHandler.getDeviceControlLayer());
                case TRACK_REMOTES -> layerMap.put(layerId, remotesHandler.getTrackControlLayer());
                case PROJECT_REMOTES -> layerMap.put(layerId, remotesHandler.getProjectControlLayer());
                case TRACK_CONTROL -> layerMap.put(layerId, remotesHandler.getCursorTrackMixerLayer());
                case MIX_CONTROL -> layerMap.put(layerId, remotesHandler.getMixerLayer());
                case NAVIGATION -> layerMap.put(layerId, navigationLayer);
                case PLUGIN_NAVIGATION -> layerMap.put(layerId, pluginNavigationLayer);
                case DRUM_PAD_CONTROL -> layerMap.put(layerId, drumPadLayer);
                case PAD_MENU_LAYER -> layerMap.put(layerId, padMenuLayer);
                default -> layerMap.put(layerId, new Layer(layers, layerId.toString()));
            }
        }
    }
    
    private void setTrackControl(MpkViewControl viewControl, LineDisplay display) {
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        cursorTrack.name().addValueObserver(name -> {
            display.setText(0, 0, name);
            remotesHandler.updateDisplayState();
        });
        
        cursorTrack.color().addValueObserver((r, g, b) -> {
            final int index = MpkColorLookup.rgbToIndex(r, g, b);
            display.setColorIndex(0, 0, index);
        });
    }
    
    private void handlePadModeChange(final int mode) {
        if (mode == currentPadMode) {
            return;
        }
        
        final Layer currentLayer = get(padModSelector.apply(currentPadMode));
        currentLayer.setIsActive(false);
        final LayerId padModeLayerId = padModSelector.apply(mode);
        padMode.set(padModeLayerId);
        final Layer newLayer = get(padModeLayerId);
        newLayer.setIsActive(true);
        this.currentPadMode = mode;
        hwElements.displayTemp("Pad Mode", modeString(padModeLayerId));
    }
    
    private String modeString(LayerId id) {
        return switch (id) {
            case CLIP_LAUNCHER -> "Clip Launcher";
            case TRACK_PAD_CONTROL -> "Track Control";
            case MUTE_SOLO_CONTROL -> "Solo/Mute";
            case MIXER_PAD_CONTROL -> "Stop/Arm";
            case DRUM_PAD_CONTROL -> "Notes";
            default -> "";
        };
    }
    
    private void handleDawPluginModeChange(final MpkBaseMode mode) {
        final Layer currentPadLayer = get(padModSelector.apply(currentPadMode));
        if (mode == MpkBaseMode.DAW) {
            MpkMk4ControllerExtension.println(" MODE TO DAW");
            currentPadLayer.setIsActive(true);
            get(LayerId.MAIN).setIsActive(true);
            get(LayerId.NAVIGATION).setIsActive(true);
            //get(LayerId.PLUGIN_NAVIGATION).setIsActive(false);
            remotesHandler.setActive(true);
        } else if (mode == MpkBaseMode.PLUGIN) {
            MpkMk4ControllerExtension.println(" MODE TO PLUGIN");
            currentPadLayer.setIsActive(false);
            get(LayerId.MAIN).setIsActive(true);
            get(LayerId.NAVIGATION).setIsActive(false);
            //get(LayerId.PLUGIN_NAVIGATION).setIsActive(true);
            remotesHandler.setActive(false);
        }
    }
    
    public ValueObject<LayerId> getPadMode() {
        return padMode;
    }
    
    public RemotesControlHandler getRemotesHandler() {
        return remotesHandler;
    }
    
    private LayerId padModeToLayerId(final int mode) {
        return switch (mode) {
            case 0 -> LayerId.DRUM_PAD_CONTROL;
            case 2 -> LayerId.TRACK_PAD_CONTROL;   // LayerId.MIXER_PAD_CONTROL; Mini Plus II
            default -> LayerId.CLIP_LAUNCHER;
        };
    }
    
    private LayerId padModeToLayerIdExpanded(final int mode) {
        return switch (mode) {
            case 0 -> LayerId.DRUM_PAD_CONTROL;
            case 2 -> LayerId.MIXER_PAD_CONTROL;   // LayerId.MIXER_PAD_CONTROL; Mini Plus II
            case 3 -> LayerId.MUTE_SOLO_CONTROL;
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
