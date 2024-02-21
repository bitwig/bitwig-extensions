package com.bitwig.extensions.controllers.mcu.layer;

import com.bitwig.extensions.controllers.mcu.VPotMode;
import com.bitwig.extensions.controllers.mcu.bindings.ResetableBinding;
import com.bitwig.extensions.controllers.mcu.bindings.display.StringRowDisplayBinding;
import com.bitwig.extensions.controllers.mcu.devices.SpecificDevice;
import com.bitwig.extensions.controllers.mcu.display.DisplayRow;
import com.bitwig.extensions.controllers.mcu.value.Orientation;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.BasicStringValue;

public class SpecialDeviceModeLayer extends MixerModeLayer {
    
    private final Layer topRowValueLayer;
    private final Layer bottomRowValueLayer;
    private final BasicStringValue topRowInfoText;
    private final BasicStringValue bottomRowInfoText;
    private String infoState = null;
    private final SpecificDevice device;
    private final VPotMode matchingPotMode;
    private String infoText = null;
    
    public SpecialDeviceModeLayer(final Layers layers, final ControlMode mode, final VPotMode matchingPotMode,
        final MixerSection mixer, final SpecificDevice specialDevice) {
        super(mode, mixer);
        this.device = specialDevice;
        this.matchingPotMode = matchingPotMode;
        this.topRowValueLayer = new Layer(layers, "INFO_LABEL_%s".formatted(mode));
        this.bottomRowValueLayer = new Layer(layers, "INFO_VALUE_%s".formatted(mode));
        topRowInfoText = new BasicStringValue("");
        bottomRowInfoText = new BasicStringValue("");
        this.topRowValueLayer.addBinding(
            new StringRowDisplayBinding(mixer.getDisplayManager(), mode, DisplayRow.LABEL, mixer.getSectionIndex(),
                topRowInfoText));
        this.bottomRowValueLayer.addBinding(
            new StringRowDisplayBinding(mixer.getDisplayManager(), mode, DisplayRow.VALUE, mixer.getSectionIndex(),
                bottomRowInfoText));
        device.addUpdateListeners(this::updateBindings);
        device.addExistChangeListener(this::handleExistenceChanged);
    }
    
    private void updateBindings() {
        if (!active) {
            return;
        }
        resetBindings();
    }
    
    private void handleExistenceChanged(final boolean exists) {
        if (!active) {
            return;
        }
        reassign();
    }
    
    private void resetBindings() {
        resetBindings(displayLabelLayer);
        resetBindings(displayValueLayer);
        resetBindings(faderLayer);
        resetBindings(encoderLayer);
    }
    
    private void resetBindings(final Layer layer) {
        layer.getBindings().stream().filter(ResetableBinding.class::isInstance).map(ResetableBinding.class::cast)
            .forEach(binding -> binding.reset());
    }
    
    @Override
    public void assign() {
        final ControlMode mainMode = !mixer.isFlipped() ? mode : ControlMode.VOLUME;
        final ControlMode lowMode = mixer.isFlipped() ? mode : ControlMode.VOLUME;
        
        encoderLayer = mixer.getLayerSource(mainMode).getEncoderLayer();
        faderLayer = mixer.getLayerSource(lowMode).getFaderLayer();
        
        if (mixer.hasLowerDisplay()) {
            assignDualDisplay();
        } else {
            assignSingleDisplay(mainMode, lowMode);
        }
        assignIfMenuModeActive();
    }
    
    private void assignDualDisplay() {
        final boolean nameValue = mixer.isNameValue();
        final boolean isFlipped = mixer.isFlipped();
        if (!device.isSpecificDevicePresent()) {
            infoState = "No EQ+ in Device Chain press EQ+ to add";
        } else {
            infoState = null;
        }
        if (isFlipped) {
            mixer.setUpperLowerDestination(ControlMode.VOLUME, mode);
            displayLabelLayer = mixer.getTrackDisplayLayer();
            displayValueLayer = mixer.getLayerSource(ControlMode.VOLUME).getDisplayValueLayer();
            
            displayLowerLabelLayer = mixer.getLayerSource(mode).getDisplayLabelLayer();
            displayLowerValueLayer = mixer.getLayerSource(mode).getDisplayValueLayer();
            if (infoState != null) {
                topRowInfoText.set(infoState);
                bottomRowInfoText.set("");
                displayLowerLabelLayer = topRowValueLayer;
                displayLowerValueLayer = bottomRowValueLayer;
            } else if (nameValue) {
                displayLabelLayer = mixer.getLayerSource(ControlMode.VOLUME).getDisplayLabelLayer();
                displayLowerValueLayer = bottomRowValueLayer;
            }
        } else {
            mixer.setUpperLowerDestination(mode, ControlMode.VOLUME);
            displayLabelLayer = mixer.getLayerSource(mode).getDisplayLabelLayer();
            displayValueLayer = mixer.getLayerSource(mode).getDisplayValueLayer();
            displayLowerValueLayer = mixer.getLayerSource(ControlMode.VOLUME).getDisplayValueLayer();
            displayLowerLabelLayer = mixer.getTrackDisplayLayer();
            if (infoState != null) {
                topRowInfoText.set(infoState);
                bottomRowInfoText.set("");
                displayLabelLayer = topRowValueLayer;
                displayValueLayer = bottomRowValueLayer;
            } else if (nameValue) {
                bottomRowInfoText.set(device.getPageInfo());
                displayValueLayer = bottomRowValueLayer;
                displayLowerLabelLayer = mixer.getLayerSource(ControlMode.VOLUME).getDisplayLabelLayer();
            }
        }
    }
    
    private void assignSingleDisplay(final ControlMode mainMode, final ControlMode lowMode) {
        final boolean touched = mixer.isTouched();
        final boolean nameValue = mixer.isNameValue();
        final ControlMode displayMode = touched ? lowMode : mainMode;
        
        if (displayMode == this.mode) {
            if (!device.isSpecificDevicePresent()) {
                infoState = "No EQ+ in Device Chain press EQ+ to add";
            } else {
                infoState = null;
            }
            
            displayLabelLayer = mixer.getLayerSource(displayMode).getDisplayLabelLayer();
            if (infoState != null) {
                topRowInfoText.set(infoState);
                bottomRowInfoText.set("");
                displayLabelLayer = topRowValueLayer;
                displayValueLayer = bottomRowValueLayer;
            } else {
                if (nameValue) {
                    displayValueLayer = bottomRowValueLayer;
                } else {
                    displayValueLayer = mixer.getLayerSource(displayMode).getDisplayValueLayer();
                }
            }
        } else {
            displayLabelLayer = nameValue
                ? mixer.getLayerSource(ControlMode.VOLUME).getDisplayLabelLayer()
                : mixer.getTrackDisplayLayer();
            displayValueLayer = mixer.getLayerSource(displayMode).getDisplayValueLayer();
        }
        if (infoText != null && device.isSpecificDevicePresent()) {
            topRowInfoText.set(infoText);
            displayLabelLayer = topRowValueLayer;
        }
    }
    
    @Override
    public void handleInfoState(final boolean start, final Orientation orientation) {
        if (active) {
            if (start) {
                infoText = "EQ+  Page: %s".formatted(device.getPageInfo());
            } else {
                infoText = null;
            }
            reassign();
        }
    }
    
    @Override
    public void handleModePress(final VPotMode mode, final boolean pressed, final boolean selection) {
    }
    
}