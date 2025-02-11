package com.bitwig.extensions.controllers.novation.slmk3.seqcommons;

import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class SequencerLayers extends Layer {
    
    private final Layer stepLayer;
    private final Layer topRecurrenceLayer;
    private final Layer bottomRecurrenceLayer;
    private final Layer copyLayer;
    private final Layer stepInputLayer;
    private SeqLayerMode mode = SeqLayerMode.STANDARD;
    
    public enum SeqLayerMode {
        STANDARD,
        TOP_RECURRENCE,
        BOTTOM_RECURRENCE,
        STEP_INPUT,
        LENGTH
    }
    
    public SequencerLayers(final Layers layers) {
        super(layers, "SEQUENCER_LAYERS");
        this.stepLayer = new Layer(layers, "SEQ_PAD_LAYER");
        this.topRecurrenceLayer = new Layer(layers, "REC1_TOP_LAYER");
        this.bottomRecurrenceLayer = new Layer(layers, "REC21_TOP_LAYER");
        this.copyLayer = new Layer(layers, "COPY_LAYER");
        this.stepInputLayer = new Layer(layers, "STEP_INPUT");
        //        this.gridLayer = new Layer(layers, "GRID_LAYER");
    }
    
    public Layer getStepLayer() {
        return stepLayer;
    }
    
    public void setMode(final SeqLayerMode mode) {
        this.mode = mode;
        applyMode();
    }
    
    public SeqLayerMode getMode() {
        return mode;
    }
    
    public void reset() {
        if (mode == SeqLayerMode.STANDARD) {
            return;
        }
        this.mode = SeqLayerMode.STANDARD;
        applyMode();
    }
    
    private void applyMode() {
        this.topRecurrenceLayer.setIsActive(mode == SeqLayerMode.TOP_RECURRENCE);
        this.bottomRecurrenceLayer.setIsActive(mode == SeqLayerMode.BOTTOM_RECURRENCE);
        this.stepInputLayer.setIsActive(mode == SeqLayerMode.STEP_INPUT);
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
        stepLayer.setIsActive(true);
        this.stepInputLayer.setIsActive(mode == SeqLayerMode.STEP_INPUT);
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        stepLayer.setIsActive(false);
        copyLayer.setIsActive(false);
        stepInputLayer.setIsActive(false);
        bottomRecurrenceLayer.setIsActive(false);
        topRecurrenceLayer.setIsActive(false);
    }
    
    public Layer getBottomRecurrenceLayer() {
        return bottomRecurrenceLayer;
    }
    
    public Layer getTopRecurrenceLayer() {
        return topRecurrenceLayer;
    }
    
    public Layer getCopyLayer() {
        return copyLayer;
    }
    
    public Layer getStepInputLayer() {
        return stepInputLayer;
    }
    
}
