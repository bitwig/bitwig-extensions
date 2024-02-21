package com.bitwig.extensions.controllers.mcu.layer;

import com.bitwig.extensions.controllers.mcu.VPotMode;

public class AllSendsModeLayer extends MixerModeLayer {
    
    public AllSendsModeLayer(final ControlMode mode, final MixerSection mixer) {
        super(mode, mixer);
    }
    
    public void handleModePress(final VPotMode mode, final boolean pressed, final boolean selection) {
        if (!active) {
            return;
        }
        if (!selection) {
            if (pressed) {
                mixer.activateSendPrePostMenu();
            } else {
                mixer.releaseLayer();
            }
        }
    }
    
    public void assign() {
        final boolean flipped = mixer.isFlipped();
        final boolean touched = mixer.isTouched();
        
        final ControlMode mainMode = !flipped ? mode : ControlMode.VOLUME;
        final ControlMode lowMode = flipped ? mode : ControlMode.VOLUME;
        
        encoderLayer = mixer.getLayerSource(mainMode).getEncoderLayer();
        faderLayer = mixer.getLayerSource(lowMode).getFaderLayer();
        if (mixer.hasLowerDisplay()) {
            assignDualDisplay();
        } else {
            final ControlMode displayMode = touched ? lowMode : mainMode;
            final boolean nameValue = mixer.isNameValue();
            if (mixer.isFlipped()) {
                displayLabelLayer =
                    nameValue ? mixer.getTrackDisplayLayer() : mixer.getLayerSource(displayMode).getDisplayLabelLayer();
            } else {
                displayLabelLayer =
                    nameValue ? mixer.getTrackDisplayLayer() : mixer.getLayerSource(displayMode).getDisplayLabelLayer();
            }
            displayValueLayer = mixer.getLayerSource(displayMode).getDisplayValueLayer();
        }
        assignIfMenuModeActive();
    }
    
    private void assignDualDisplay() {
        final boolean nameValue = mixer.isNameValue();
        if (mixer.isFlipped()) {
            displayLabelLayer = nameValue
                ? mixer.getLayerSource(ControlMode.VOLUME).getDisplayValueLayer()
                : mixer.getTrackDisplayLayer();
            displayValueLayer = mixer.getLayerSource(ControlMode.VOLUME).getDisplayValueLayer();
            displayLowerValueLayer = mixer.getLayerSource(mode).getDisplayValueLayer();
            displayLowerLabelLayer = mixer.getLayerSource(mode).getDisplayLabelLayer();
            mixer.setUpperLowerDestination(ControlMode.VOLUME, mode);
        } else {
            displayLabelLayer = mixer.getLayerSource(mode).getDisplayValueLayer();
            displayValueLayer = mixer.getLayerSource(mode).getDisplayLabelLayer();
            displayLowerLabelLayer = nameValue
                ? mixer.getLayerSource(ControlMode.VOLUME).getDisplayValueLayer()
                : mixer.getTrackDisplayLayer();
            displayLowerValueLayer = mixer.getLayerSource(ControlMode.VOLUME).getDisplayValueLayer();
            mixer.setUpperLowerDestination(mode, ControlMode.VOLUME);
        }
    }
    
}
