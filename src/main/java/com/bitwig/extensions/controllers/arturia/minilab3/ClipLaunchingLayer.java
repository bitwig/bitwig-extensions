package com.bitwig.extensions.controllers.arturia.minilab3;

import java.util.Arrays;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.Layer;

public class ClipLaunchingLayer extends Layer {
    
    private final RgbLightState[] slotColors = new RgbLightState[8];
    private final BooleanValue clipLauncherOverdub;
    private final long[] downTime = new long[8];
    private final TrackBank trackBank;
    private final MiniLab3Extension driver;
    private int blinkState;
    private long clipsStopTiming = 800;
    private final BooleanValue transportPlaying;
    
    public ClipLaunchingLayer(final MiniLab3Extension driver) {
        super(driver.getLayers(), "CLIP LAUNCHER");
        this.driver = driver;
        clipLauncherOverdub = driver.getTransport().isClipLauncherOverdubEnabled();
        transportPlaying = driver.getTransport().isPlaying();
        clipLauncherOverdub.markInterested();
        transportPlaying.markInterested();
        
        Arrays.fill(slotColors, RgbLightState.OFF);
        Arrays.fill(downTime, -1);
        
        trackBank = driver.getViewTrackBank();
        
        
        final MultiStateHardwareLight bankLight =
            driver.getSurface().createMultiStateHardwareLight("CLIP_LAUNCH_LIGHTS");
        bankLight.state().onUpdateHardware(driver::updateBankState);
        final MinilabRgbButton[] buttons = driver.getPadBankAButtons();
        
        final RgbBankLightState.Handler bankLightHandler = new RgbBankLightState.Handler(PadBank.BANK_A, buttons);
        this.bindLightState(bankLightHandler::getBankLightState, bankLight);
        
        setupHorizontalLaunching(driver);
        
        driver.getPadBank().addValueObserver((this::changePadBank));
    }
    
    public void setClipStopTiming(final String timingValue) {
        switch (timingValue) {
            case "Fast":
                clipsStopTiming = 500;
                break;
            case "Medium":
                clipsStopTiming = 800;
                break;
            case "Standard":
                clipsStopTiming = 1000;
                break;
        }
    }
    
    private void changePadBank(final PadBank newValue) {
        if (!isActive()) {
            return;
        }
        trackBank.setShouldShowClipLauncherFeedback(newValue == PadBank.BANK_A);
    }
    
    private void setupHorizontalLaunching(final MiniLab3Extension driver) {
        final MinilabRgbButton[] buttons = driver.getPadBankAButtons();
        final TrackBank trackBank = driver.getViewTrackBank();
        trackBank.setShouldShowClipLauncherFeedback(driver.getPadBank().get() == PadBank.BANK_A);
        for (int i = 0; i < trackBank.getSizeOfBank(); i++) {
            final Track track = trackBank.getItemAt(i);
            track.arm().markInterested();
            for (int j = 0; j < trackBank.sceneBank().getSizeOfBank(); j++) {
                final int buttonIndex = j * 4 + i;
                final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(j);
                prepareSlot(slot, buttonIndex);
                final MinilabRgbButton button = buttons[buttonIndex];
                button.bindPressed(this, pressed -> handleSlotSelected(buttonIndex, track, slot, pressed));
                button.bindLightState(this, () -> getLightState(buttonIndex, track, slot));
            }
            track.isQueuedForStop().markInterested();
        }
    }
    
    public void launchScene() {
        trackBank.sceneBank().getScene(0).launch();
    }
    
    private void prepareSlot(final ClipLauncherSlot cs, final int index) {
        cs.color().addValueObserver((r, g, b) -> slotColors[index] = RgbLightState.getColor(r, g, b));
        cs.exists().markInterested();
        cs.hasContent().markInterested();
        cs.isPlaybackQueued().markInterested();
        cs.isPlaying().markInterested();
        cs.isRecording().markInterested();
        cs.isRecordingQueued().markInterested();
        cs.isSelected().markInterested();
        cs.isStopQueued().markInterested();
    }
    
    public void notifyBlink(final int blinkState) {
        this.blinkState = blinkState;
        final long time = System.currentTimeMillis();
        for (int i = 0; i < 8; i++) {
            if (downTime[i] != -1 && (time - downTime[i]) > clipsStopTiming) {
                final Track track = driver.getViewTrackBank().getItemAt(i);
                track.stop();
            }
        }
    }
    
    private void handleSlotSelected(final int slotIndex, final Track track, final ClipLauncherSlot slot,
        final boolean pressed) {
        if (pressed) {
            downTime[slotIndex] = System.currentTimeMillis();
        } else {
            final long diff = System.currentTimeMillis() - downTime[slotIndex];
            if (diff > clipsStopTiming) {
                track.stop();
            } else {
                slot.launch();
            }
            downTime[slotIndex] = -1;
        }
    }
    
    RgbLightState getLightState(final int index, final Track track, final ClipLauncherSlot slot) {
        final RgbLightState color = slotColors[index];
        if (!slot.exists().get()) {
            return RgbLightState.OFF;
        }
        if (slot.hasContent().get()) {
            if (slot.isPlaybackQueued().get()) {
                return blinkFast(color.getDarker(), color.getBrighter());
            }
            if (track.isQueuedForStop().get()) {
                return blinkFast(color.getDarker(), color);
            }
            if (slot.isRecordingQueued().get()) {
                return blinkSlow(color.getDarker(), RgbLightState.RED);
            }
            if (slot.isRecording().get()) {
                return blinkSlow(color, RgbLightState.RED);
            }
            if (slot.isPlaying().get() && track.isQueuedForStop().get()) {
                return blinkSlow(RgbLightState.GREEN, color.getDarker());
            }
            if (slot.isPlaying().get()) {
                if (clipLauncherOverdub.get() && track.arm().get()) {
                    return blinkSlow(RgbLightState.RED, color.getDarker());
                } else {
                    if (transportPlaying.get()) {
                        return blinkSlow(RgbLightState.GREEN, color.getBrighter());
                    }
                    return color.getBrighter();
                    //return RgbLightState.GREEN;
                }
            }
            return color.getDarker();
        }
        if (slot.isRecordingQueued().get()) {
            return blinkFast(color.getDarker(), RgbLightState.RED);
        } else if (track.arm().get()) {
            return RgbLightState.RED.getDarker();
        } else if (slot.isPlaybackQueued().get()) {
            return blinkFast(RgbLightState.GREEN, RgbLightState.WHITE);
        }
        return RgbLightState.OFF;
    }
    
    private RgbLightState blinkSlow(final RgbLightState on, final RgbLightState off) {
        if (blinkState % 8 < 4) {
            return on;
        }
        return off;
    }
    
    private RgbLightState blinkFast(final RgbLightState on, final RgbLightState off) {
        if (blinkState % 2 == 0) {
            return on;
        }
        return off;
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
        trackBank.setShouldShowClipLauncherFeedback(driver.getPadBank().get() == PadBank.BANK_A);
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        trackBank.setShouldShowClipLauncherFeedback(false);
    }
    
}
