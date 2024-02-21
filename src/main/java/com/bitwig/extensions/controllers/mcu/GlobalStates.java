package com.bitwig.extensions.controllers.mcu;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.controllers.mcu.display.VuMode;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.ValueObject;

@Component
public class GlobalStates {
    private final BooleanValueObject shift = new BooleanValueObject();
    private final BooleanValueObject option = new BooleanValueObject();
    private final BooleanValueObject control = new BooleanValueObject();
    private final BooleanValueObject zoomMode = new BooleanValueObject();
    
    private final BooleanValueObject flipped = new BooleanValueObject();
    private final BooleanValueObject nameValue = new BooleanValueObject();
    private final BooleanValueObject globalView = new BooleanValueObject();
    private final BooleanValueObject clipLaunchingActive = new BooleanValueObject();
    private final BasicStringValue twoSegmentText = new BasicStringValue("  ");
    private final ValueObject<VPotMode> potMode;
    private final ValueObject<VuMode> vuMode = new ValueObject<>(VuMode.LED);
    private VPotMode lastSendsMode = VPotMode.SEND;
    private final ControllerHost host;
    private int heldSoloButtons = 0;
    private String trackIndexMain = "01";
    private String trackIndexGlobal = "01";
    
    public GlobalStates(final ControllerHost host) {
        this.host = host;
        potMode = new ValueObject<>(VPotMode.PAN);
        potMode.addValueObserver((oldValue, newValue) -> {
            if (newValue == VPotMode.ALL_SENDS || newValue == VPotMode.SEND) {
                this.lastSendsMode = newValue;
            }
        });
    }
    
    @Activate
    public void activate() {
        clipLaunchingActive.addValueObserver(active -> updateSegmentText());
        getGlobalView().addValueObserver(active -> updateSegmentText());
    }
    
    private void updateSegmentText() {
        if (clipLaunchingActive.get()) {
            twoSegmentText.set("CL");
        } else {
            if (globalView.get()) {
                twoSegmentText.set(trackIndexGlobal);
            } else {
                twoSegmentText.set(trackIndexMain);
            }
        }
    }
    
    public BooleanValueObject getGlobalView() {
        return globalView;
    }
    
    
    public void toggleTrackMode() {
        if (potMode.get() == VPotMode.SEND) {
            potMode.set(VPotMode.ALL_SENDS);
        } else if (potMode.get() == VPotMode.ALL_SENDS) {
            potMode.set(VPotMode.SEND);
        }
    }
    
    public BooleanValueObject getFlipped() {
        return flipped;
    }
    
    public BooleanValueObject getShift() {
        return shift;
    }
    
    public BooleanValueObject getOption() {
        return option;
    }
    
    public BooleanValueObject getControl() {
        return control;
    }
    
    public ValueObject<VPotMode> getPotMode() {
        return potMode;
    }
    
    public ValueObject<VuMode> getVuMode() {
        return vuMode;
    }
    
    public BooleanValueObject getNameValue() {
        return nameValue;
    }
    
    public BooleanValueObject getZoomMode() {
        return zoomMode;
    }
    
    public void soloPressed(final boolean pressed) {
        if (pressed) {
            heldSoloButtons++;
        } else if (heldSoloButtons > 0) {
            heldSoloButtons--;
        }
    }
    
    public boolean isSoloHeld() {
        return heldSoloButtons > 0;
    }
    
    public boolean isShiftSet() {
        return shift.get();
    }
    
    public boolean isOptionSet() {
        return option.get();
    }
    
    public boolean isControlSet() {
        return control.get();
    }
    
    public boolean trackModeActive() {
        return potMode.get().getAssign() != VPotMode.BitwigType.CHANNEL || potMode.get() == VPotMode.ALL_SENDS;
    }
    
    public VPotMode getLastSendsMode() {
        return lastSendsMode;
    }
    
    public SettableBooleanValue getClipLaunchingActive() {
        return clipLaunchingActive;
    }
    
    public void notifySelectedTrackState(final String trackCode, final boolean fromExtended) {
        if (fromExtended) {
            trackIndexGlobal = trackCode;
        } else {
            trackIndexMain = trackCode;
        }
        updateSegmentText();
    }
    
    public BasicStringValue getTwoSegmentText() {
        return twoSegmentText;
    }
}
