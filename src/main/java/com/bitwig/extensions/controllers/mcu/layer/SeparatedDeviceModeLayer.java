package com.bitwig.extensions.controllers.mcu.layer;

import com.bitwig.extensions.controllers.mcu.StringUtil;
import com.bitwig.extensions.controllers.mcu.VPotMode;
import com.bitwig.extensions.controllers.mcu.devices.DeviceTypeBank;
import com.bitwig.extensions.framework.Layers;

public class SeparatedDeviceModeLayer extends DeviceModeLayer {
    private final DeviceTypeBank deviceTypeBank;
    private int pageCount;
    private DeviceTypeBank.GeneralDeviceType currentSelectedDeviceType = DeviceTypeBank.GeneralDeviceType.NONE;
    
    public SeparatedDeviceModeLayer(final Layers layers, final ControlMode mode, final MixerSection mixer,
        final DeviceTypeBank deviceTypeBank) {
        super(layers, mode, mixer);
        this.deviceTypeBank = deviceTypeBank;
        deviceTypeBank.addDeviceTypeListener(this::handleDeviceTypeChanged);
        deviceTypeBank.getCursorRemotes().pageCount().addValueObserver(pageCount -> {
            this.pageCount = pageCount;
            if (active) {
                evalDeviceMatch();
                reassign();
            }
        });
        deviceTypeBank.addExistenceListener((potMode, exist) -> {
            if (active && this.potMode == potMode) {
                evalDeviceMatch();
                reassign();
            }
        });
    }
    
    private void handleDeviceTypeChanged(final DeviceTypeBank.GeneralDeviceType type) {
        currentSelectedDeviceType = type;
        if (active) {
            evalDeviceMatch();
            reassign();
        }
    }
    
    @Override
    protected void evalDeviceMatch() {
        if (!deviceTypeBank.hasDeviceType(potMode)) {
            matchState = State.TYPE_NOT_IN_CHAIN;
        } else if (potMode != currentSelectedDeviceType.getMode()) {
            matchState = State.TYPE_MISMATCH;
            //            McuExtension.println("MISMATCH %s %s  = %s", currentSelectedDeviceType,
            //            currentSelectedDeviceType.getMode(),
            //                    potMode);
        } else if (pageCount == 0) {
            matchState = State.NO_PARAM_PAGES;
        } else {
            matchState = State.TYPE_MATCH;
        }
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
    
    public void handleModePress(final VPotMode mode, final boolean pressed) {
        if (!active) {
            return;
        }
        if (pressed) {
            //McuExtension.println(" Force Mode " + mode + " > " + deviceTypeBank.hasDeviceType(mode));
            if (deviceTypeBank.hasDeviceType(mode)) {
                //deviceTypeBank.ensureDeviceSelection(mode);
            }
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
    protected void evalInfoState(final VPotMode potMode) {
        if (!deviceTypeBank.hasDeviceType(potMode)) {
            matchState = State.TYPE_NOT_IN_CHAIN;
        } else if (deviceTypeBank.hasDeviceType(potMode)) {
            matchState = State.TYPE_MATCH;
            deviceTypeBank.ensureDeviceSelection(potMode);
        }
    }
    
    protected void determineInfoState(final boolean flipTopBottom) {
        String topLineText = deviceInfo;
        String bottomLineText = "";
        if (matchState == State.TYPE_MISMATCH) {
            topLineText = "No %s in focus".formatted(potMode.getDescription());
            bottomLineText = "Press %s to focus".formatted(this.potMode);
        } else if (matchState == State.TYPE_NOT_IN_CHAIN) {
            topLineText = "No %s in Chain".formatted(potMode.getDescription());
            bottomLineText = "Browse for Device in Bitwig";
        } else if (matchState == State.NO_PARAM_PAGES) {
            topLineText = "Configure in Bitwig";
            bottomLineText = "%s has no Parameter Pages".formatted(deviceName);
        }
        bottomRowInfoText.set(flipTopBottom ? bottomLineText : topLineText);
        topRowInfoText.set(flipTopBottom ? topLineText : bottomLineText);
    }
    
}
