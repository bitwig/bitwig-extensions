package com.bitwig.extensions.controllers.mcu.layer;

import com.bitwig.extensions.controllers.mcu.CursorDeviceControl;
import com.bitwig.extensions.controllers.mcu.VPotMode;
import com.bitwig.extensions.framework.Layers;

public class UnifiedDeviceModeLayer extends DeviceModeLayer {
    private int pageCount;
    private final CursorDeviceControl cursorDeviceControl;
    private boolean deviceExists;
    
    public UnifiedDeviceModeLayer(final Layers layers, final ControlMode mode, final MixerSection mixer,
        final CursorDeviceControl cursorDeviceControl) {
        super(layers, mode, mixer);
        this.cursorDeviceControl = cursorDeviceControl;
        cursorDeviceControl.getRemotes().pageCount().addValueObserver(pageCount -> {
            this.pageCount = pageCount;
            update();
        });
        cursorDeviceControl.getCursorDevice().exists().addValueObserver(exists -> {
            this.deviceExists = exists;
            update();
        });
    }
    
    private void update() {
        evalDeviceMatch();
        if (active) {
            reassign();
        }
    }
    
    @Override
    protected void evalDeviceMatch() {
        if (!deviceExists) {
            matchState = State.TYPE_NOT_IN_CHAIN;
        } else if (pageCount == 0) {
            matchState = State.NO_PARAM_PAGES;
        } else {
            matchState = State.TYPE_MATCH;
        }
    }
    
    @Override
    protected void evalInfoState(final VPotMode potMode) {
        
    }
    
    @Override
    protected void determineInfoState(final boolean flipped) {
        String topLineText = deviceInfo;
        String bottomLineText = "";
        if (matchState == State.NO_PARAM_PAGES) {
            topLineText = "Configure in Bitwig";
            bottomLineText = "%s has no Parameter Pages".formatted(deviceName);
        } else if (matchState == State.TYPE_NOT_IN_CHAIN) {
            topLineText = "No Device in Chain";
            bottomLineText = "";
        }
        bottomRowInfoText.set(flipped ? bottomLineText : topLineText);
        topRowInfoText.set(flipped ? topLineText : bottomLineText);
    }
    
}
