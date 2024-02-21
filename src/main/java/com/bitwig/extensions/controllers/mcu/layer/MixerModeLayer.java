package com.bitwig.extensions.controllers.mcu.layer;

import com.bitwig.extensions.controllers.mcu.VPotMode;
import com.bitwig.extensions.controllers.mcu.value.Orientation;
import com.bitwig.extensions.framework.Layer;

public class MixerModeLayer {
    protected final ControlMode mode;
    protected final MixerSection mixer;
    
    protected Layer faderLayer;
    protected Layer encoderLayer;
    protected Layer displayLabelLayer;
    protected Layer displayValueLayer;
    
    protected Layer displayLowerLabelLayer;
    protected Layer displayLowerValueLayer;
    
    protected boolean active = false;
    
    public MixerModeLayer(final ControlMode mode, final MixerSection mixer) {
        this.mode = mode;
        this.mixer = mixer;
    }
    
    public void setIsActive(final boolean isActive) {
        if (this.active == isActive) {
            return;
        }
        this.active = isActive;
        activateLayers(isActive);
    }
    
    private void activateLayers(final boolean isActive) {
        if (encoderLayer == null) {
            this.assign();
        }
        encoderLayer.setIsActive(isActive);
        displayLabelLayer.setIsActive(isActive);
        displayValueLayer.setIsActive(isActive);
        faderLayer.setIsActive(isActive);
        if (mixer.hasLowerDisplay()) {
            displayLowerValueLayer.setIsActive(isActive);
            displayLowerLabelLayer.setIsActive(isActive);
        }
    }
    
    public void assign() {
        final boolean flipped = mixer.isFlipped();
        final boolean touched = mixer.isTouched();
        final boolean nameValue = mixer.isNameValue();
        
        final ControlMode mainMode = !flipped ? mode : ControlMode.VOLUME;
        final ControlMode lowMode = flipped ? mode : ControlMode.VOLUME;
        
        encoderLayer = mixer.getLayerSource(mainMode).getEncoderLayer();
        faderLayer = mixer.getLayerSource(lowMode).getFaderLayer();
        mixer.setUpperLowerDestination(mainMode, lowMode);
        if (mixer.hasLowerDisplay()) {
            displayValueLayer = mixer.getLayerSource(mainMode).getDisplayValueLayer();
            displayLowerValueLayer = mixer.getLayerSource(lowMode).getDisplayValueLayer();
            
            if (flipped) {
                displayLabelLayer = nameValue
                    ? mixer.getLayerSource(ControlMode.VOLUME).getDisplayLabelLayer()
                    : mixer.getTrackDisplayLayer();
                displayLowerLabelLayer = mixer.getLayerSource(mainMode).getDisplayLabelLayer();
            } else {
                displayLabelLayer = mixer.getLayerSource(mainMode).getDisplayLabelLayer();
                displayLowerLabelLayer = nameValue
                    ? mixer.getLayerSource(ControlMode.VOLUME).getDisplayLabelLayer()
                    : mixer.getTrackDisplayLayer();
            }
        } else {
            final ControlMode displayMode = touched ? lowMode : mainMode;
            displayLabelLayer =
                nameValue ? mixer.getLayerSource(displayMode).getDisplayLabelLayer() : mixer.getTrackDisplayLayer();
            displayValueLayer = mixer.getLayerSource(displayMode).getDisplayValueLayer();
        }
        assignIfMenuModeActive();
    }
    
    protected void assignIfMenuModeActive() {
        if (mixer.getActiveLayerGroup() != null) {
            encoderLayer = mixer.getActiveLayerGroup().getEncoderLayer();
            displayLabelLayer = mixer.getActiveLayerGroup().getLabelLayer();
            displayValueLayer = mixer.getActiveLayerGroup().getValueLayer();
        }
    }
    
    public void reassign() {
        activateLayers(false);
        this.assign();
        activateLayers(true);
    }
    
    public void handleInfoState(final boolean start, final Orientation orientation) {
        // default does nothing
    }
    
    public void handleModePress(final VPotMode mode, final boolean pressed, final boolean selection) {
        if (!active) {
            return;
        }
        
        if (!selection && mode == VPotMode.SEND) {
            if (pressed) {
                mixer.activateSendPrePostMenu();
            } else {
                mixer.releaseLayer();
            }
        }
    }
}
