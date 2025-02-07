package com.bitwig.extensions.controllers.novation.slmk3.layer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.bitwig.extensions.controllers.novation.slmk3.GlobalStates;
import com.bitwig.extensions.controllers.novation.slmk3.display.ButtonMode;
import com.bitwig.extensions.controllers.novation.slmk3.display.KnobMode;
import com.bitwig.extensions.controllers.novation.slmk3.seqcommons.SequencerLayers;
import com.bitwig.extensions.controllers.novation.slmk3.value.ObservableValue;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;

@Component
public class LayerRepo {
    
    private final Layer padClipLayer;
    private final Layer padModifierLayer;
    private final Layer selectClipLayer;
    private final Layer padDeviceSelectionLayer;
    private final Layer padDrumLayer;
    private final SequencerLayers padSequenceLayer;
    
    private final Map<KnobMode, Layer> knobLayers = new HashMap<>();
    private final Map<ButtonMode, Layer> buttonLayers = new HashMap<>();
    private final ObservableValue<ButtonMode> buttonMode;
    
    public LayerRepo(final Layers layers, final GlobalStates globalStates) {
        this.buttonMode = globalStates.getButtonMode();
        this.buttonMode.addValueObserver(mode -> applySelectButtonLayer(mode));
        this.padClipLayer = new Layer(layers, "PAD_CLIP_LAYER");
        this.padModifierLayer = new Layer(layers, "PAD_MODIFIER_LAYER");
        this.selectClipLayer = new Layer(layers, "SELECT_CLIP_LAYER");
        this.padSequenceLayer = new SequencerLayers(layers);
        this.padDrumLayer = new Layer(layers, "PAD_DRUM_LAYER");
        this.padDeviceSelectionLayer = new Layer(layers, "DEVICE_CLIP_LAYER");
        Arrays.stream(KnobMode.values())
            .forEach(mode -> knobLayers.put(mode, new Layer(layers, "%s_KNOB_LAYER".formatted(mode))));
        Arrays.stream(ButtonMode.values())
            .forEach(mode -> buttonLayers.put(mode, new Layer(layers, "%s_BUTTON_LAYER".formatted(mode))));
    }
    
    public Layer getPadClipLayer() {
        return padClipLayer;
    }
    
    public Layer getPadDeviceSelectionLayer() {
        return padDeviceSelectionLayer;
    }
    
    public Layer getPadModifierLayer() {
        return padModifierLayer;
    }
    
    public SequencerLayers getPadSequenceLayer() {
        return padSequenceLayer;
    }
    
    public Layer getKnobLayer(final KnobMode mode) {
        return knobLayers.get(mode);
    }
    
    public Layer getButtonLayer(final ButtonMode mode) {
        return buttonLayers.get(mode);
    }
    
    public Layer getSelectClipLayer() {
        return selectClipLayer;
    }
    
    public void updateKnobModeLayer(final KnobMode mode) {
        getKnobLayer(KnobMode.DEVICE).setIsActive(mode == KnobMode.DEVICE && buttonMode.get() != ButtonMode.OPTION);
        getKnobLayer(KnobMode.OPTION).setIsActive(mode == KnobMode.DEVICE && buttonMode.get() == ButtonMode.OPTION);
        getKnobLayer(KnobMode.OPTION_SHIFT).setIsActive(mode == KnobMode.OPTION_SHIFT);
        
        getKnobLayer(KnobMode.TRACK).setIsActive(mode == KnobMode.TRACK);
        getKnobLayer(KnobMode.PROJECT).setIsActive(mode == KnobMode.PROJECT);
        getKnobLayer(KnobMode.PAN).setIsActive(mode == KnobMode.PAN);
        getKnobLayer(KnobMode.SEND).setIsActive(mode == KnobMode.SEND);
        getKnobLayer(KnobMode.SEQUENCER).setIsActive(mode == KnobMode.SEQUENCER);
        getKnobLayer(KnobMode.DRUM_VOLUME).setIsActive(mode == KnobMode.DRUM_VOLUME);
        getKnobLayer(KnobMode.DRUM_PAN).setIsActive(mode == KnobMode.DRUM_PAN);
        getKnobLayer(KnobMode.DRUM_SENDS).setIsActive(mode == KnobMode.DRUM_SENDS);
    }
    
    public void applySelectButtonLayer(final ButtonMode mode) {
        getButtonLayer(ButtonMode.TRACK).setIsActive(mode == ButtonMode.TRACK);
        getButtonLayer(ButtonMode.OPTION).setIsActive(mode == ButtonMode.OPTION);
        getButtonLayer(ButtonMode.SHIFT).setIsActive(mode == ButtonMode.SHIFT);
        getButtonLayer(ButtonMode.SEQUENCER).setIsActive(mode == ButtonMode.SEQUENCER);
        getButtonLayer(ButtonMode.SEQUENCER2).setIsActive(mode == ButtonMode.SEQUENCER2);
    }
    
    public void applySelectButtonLayer() {
        applySelectButtonLayer(this.buttonMode.get());
    }
    
    public void applyPadLayer(final GridMode gridMode, final KnobMode knobMode) {
        padDeviceSelectionLayer.setIsActive(gridMode == GridMode.OPTION && knobMode == KnobMode.DEVICE);
        padSequenceLayer.setIsActive(gridMode == GridMode.SEQUENCER);
        selectClipLayer.setIsActive(gridMode == GridMode.SELECT);
    }
    
    public Layer getPadDrumLayer() {
        return padDrumLayer;
    }
}
