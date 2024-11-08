package com.bitwig.extensions.controllers.mcu.layer;

import com.bitwig.extensions.controllers.mcu.StringUtil;
import com.bitwig.extensions.controllers.mcu.VPotMode;
import com.bitwig.extensions.controllers.mcu.bindings.display.StringRowDisplayBinding;
import com.bitwig.extensions.controllers.mcu.display.DisplayRow;
import com.bitwig.extensions.controllers.mcu.value.Orientation;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.BasicStringValue;

public abstract class DeviceModeLayer extends MixerModeLayer {
    protected final Layer topRowValueLayer;
    protected final Layer bottomRowValueLayer;
    
    protected final BasicStringValue topRowInfoText;
    protected final BasicStringValue bottomRowInfoText;
    protected VPotMode potMode = VPotMode.PLUGIN;
    protected String deviceName = "";
    protected String pageName = "";
    protected String deviceInfo = "";
    protected State matchState = State.TYPE_MATCH;
    protected boolean justChangedTo = false;
    protected String infoText = null;
    
    protected enum State {
        TYPE_MATCH,
        TYPE_MISMATCH,
        TYPE_NOT_IN_CHAIN,
        NO_PARAM_PAGES
    }
    
    public DeviceModeLayer(final Layers layers, final ControlMode mode, final MixerSection mixer) {
        super(mode, mixer);
        this.topRowValueLayer = new Layer(layers, "INFO_LABEL_%s_%d".formatted(mode, mixer.getSectionIndex()));
        this.bottomRowValueLayer = new Layer(layers, "INFO_VALUE_%s_%d".formatted(mode, mixer.getSectionIndex()));
        topRowInfoText = new BasicStringValue("");
        bottomRowInfoText = new BasicStringValue("");
        this.topRowValueLayer.addBinding(
            new StringRowDisplayBinding(mixer.getDisplayManager(), mode, DisplayRow.LABEL, mixer.getSectionIndex(),
                topRowInfoText));
        this.bottomRowValueLayer.addBinding(
            new StringRowDisplayBinding(mixer.getDisplayManager(), mode, DisplayRow.VALUE, mixer.getSectionIndex(),
                bottomRowInfoText));
    }
    
    public void updateDeviceName(final String deviceName) {
        this.deviceName = deviceName;
        this.deviceInfo = "Device %s  Page:  %s".formatted(deviceName, 14, pageName);
        if (matchState == State.TYPE_MATCH) {
            bottomRowInfoText.set(deviceInfo);
        }
    }
    
    public void updateParameterPage(final String parameterPage) {
        this.pageName = StringUtil.padEnd(parameterPage, 14);
        this.deviceInfo = "Device %s  Page:  %s".formatted(deviceName, pageName);
        if (matchState == State.TYPE_MATCH) {
            bottomRowInfoText.set(deviceInfo);
        }
    }
    
    public void setPotMode(final VPotMode potMode) {
        justChangedTo = this.potMode != potMode;
        this.potMode = potMode;
        if (active) {
            evalInfoState(potMode);
        }
    }
    
    protected abstract void evalInfoState(final VPotMode potMode);
    
    @Override
    public void handleModePress(final VPotMode mode, final boolean pressed, final boolean selection) {
        if (!active) {
            return;
        }
        if (pressed) {
            evalInfoState(mode);
            if (!justChangedTo && matchState == State.TYPE_MATCH) {
                mixer.activateMenu(mixer.getDeviceModeLayer());
            }
            reassign();
            justChangedTo = false;
        } else {
            mixer.releaseLayer();
        }
    }
    
    @Override
    public void handleInfoState(final boolean start, final Orientation orientation) {
        if (active) {
            if (start) {
                infoText = "D: %s Page: %s".formatted(deviceName, pageName);
            } else {
                infoText = null;
            }
            reassign();
        }
    }
    
    @Override
    public void assign() {
        final ControlMode mainMode = !mixer.isFlipped() ? mode : ControlMode.VOLUME;
        final ControlMode lowMode = mixer.isFlipped() ? mode : ControlMode.VOLUME;
        evalDeviceMatch();
        determineInfoState(mixer.isFlipped());
        encoderLayer = mixer.getLayerSource(mainMode).getEncoderLayer();
        faderLayer = mixer.getLayerSource(lowMode).getFaderLayer();
        
        if (mixer.hasLowerDisplay()) {
            assignDualDisplay();
        } else {
            assignSingleDisplay(mainMode, lowMode);
        }
        assignIfMenuModeActive();
    }
    
    protected abstract void evalDeviceMatch();
    
    protected abstract void determineInfoState(final boolean flipped);
    
    protected void assignDualDisplay() {
        final boolean nameValue = mixer.isNameValue();
        final boolean isFlipped = mixer.isFlipped();
        if (isFlipped) {
            mixer.setUpperLowerDestination(ControlMode.VOLUME, mode);
            
            displayLabelLayer = mixer.getTrackDisplayLayer();
            displayValueLayer = mixer.getLayerSource(ControlMode.VOLUME).getDisplayValueLayer();
            displayLowerLabelLayer = mixer.getLayerSource(mode).getDisplayLabelLayer();
            displayLowerValueLayer = mixer.getLayerSource(mode).getDisplayValueLayer();
            if (matchState != State.TYPE_MATCH) {
                displayLowerLabelLayer = topRowValueLayer;
                displayLowerValueLayer = bottomRowValueLayer;
            } else if (nameValue) {
                displayLabelLayer = mixer.getLayerSource(ControlMode.VOLUME).getDisplayLabelLayer();
                displayLowerValueLayer = bottomRowValueLayer;
            }
            if (infoText != null && matchState == State.TYPE_MATCH) {
                topRowInfoText.set(infoText);
                displayLowerLabelLayer = topRowValueLayer;
            }
        } else {
            mixer.setUpperLowerDestination(mode, ControlMode.VOLUME);
            if (matchState != State.TYPE_MATCH) {
                displayLabelLayer = topRowValueLayer;
                displayValueLayer = bottomRowValueLayer;
            } else {
                displayLabelLayer = mixer.getLayerSource(mode).getDisplayLabelLayer();
                displayValueLayer = mixer.getLayerSource(mode).getDisplayValueLayer();
                if (nameValue) {
                    displayValueLayer = bottomRowValueLayer;
                }
            }
            displayLowerValueLayer = mixer.getLayerSource(ControlMode.VOLUME).getDisplayValueLayer();
            displayLowerLabelLayer = nameValue
                ? mixer.getLayerSource(ControlMode.VOLUME).getDisplayLabelLayer()
                : mixer.getTrackDisplayLayer();
            if (infoText != null && matchState == State.TYPE_MATCH) {
                topRowInfoText.set(infoText);
                displayLabelLayer = topRowValueLayer;
            }
        }
    }
    
    protected void assignSingleDisplay(final ControlMode mainMode, final ControlMode lowMode) {
        final boolean touched = mixer.isTouched();
        final boolean nameValue = mixer.isNameValue();
        final ControlMode displayMode = touched ? lowMode : mainMode;
        
        if (displayMode == ControlMode.STD_PLUGIN) {
            displayLabelLayer = mixer.getLayerSource(displayMode).getDisplayLabelLayer();
            if (matchState != State.TYPE_MATCH) {
                displayLabelLayer = topRowValueLayer;
                displayValueLayer = bottomRowValueLayer;
                encoderLayer = mixer.getEmptyEncoderLayer();
            } else {
                if (nameValue) {
                    displayValueLayer = bottomRowValueLayer;
                } else {
                    displayValueLayer = mixer.getLayerSource(displayMode).getDisplayValueLayer();
                }
            }
        } else {
            displayLabelLayer = nameValue
                ? mixer.getLayerSource(ControlMode.STD_PLUGIN).getDisplayLabelLayer()
                : mixer.getTrackDisplayLayer();
            displayValueLayer = mixer.getLayerSource(nameValue ? ControlMode.STD_PLUGIN : displayMode).getDisplayValueLayer();
        }
        if (infoText != null && matchState == State.TYPE_MATCH) {
            topRowInfoText.set(infoText);
            displayLabelLayer = topRowValueLayer;
        }
    }
    
}
