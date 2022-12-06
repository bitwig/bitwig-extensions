package com.bitwig.extensions.controllers.arturia.minilab3;

import java.util.Arrays;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.Layer;

public class ClipLaunchingLayer extends Layer {

    private final RgbLightState[] sceneSlotColors = new RgbLightState[8];
    private final long[] downTime = new long[8];
    private final Layer sceneLaunchingLayer; // clips in selected scene
    private final TrackBank trackBank;
    private final MiniLab3Extension driver;
    private int blinkState;
    private long clipsStopTiming = 800;
    private final byte[] colorBuffer = new byte[24];
    private final byte[] currentBuffer = new byte[24];

    public ClipLaunchingLayer(final MiniLab3Extension driver) {
        super(driver.getLayers(), "CLIP LAUNCHER");
        this.driver = driver;

        sceneLaunchingLayer = new Layer(driver.getLayers(), "PER_SCENE_LAUNCHER");

        Arrays.fill(sceneSlotColors, RgbLightState.OFF);
        Arrays.fill(downTime, -1);

        final RgbButton[] buttons = driver.getPadBankAButtons();

        trackBank = driver.getViewTrackBank();


        final MultiStateHardwareLight bankLight = driver.getSurface()
                .createMultiStateHardwareLight("CLIP_LAUNCH_LIGHTS");
        bankLight.state().onUpdateHardware(driver::updateBankState);
        final RgbBankLightState.Handler bankLightHandler = new RgbBankLightState.Handler(PadBank.BANK_A, buttons);
        sceneLaunchingLayer.bindLightState(bankLightHandler::getBankLightState, bankLight);

        setupHorizontalLaunching(driver, buttons);

        driver.getPadBank().addValueObserver(((oldValue, newValue) -> changePadBank(newValue)));
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
        activateIndication(newValue == PadBank.BANK_A);
    }

    private void activateIndication(final boolean indication) {
        for (int i = 0; i < MiniLab3Extension.NUM_PADS_TRACK; i++) {
            final Track track = trackBank.getItemAt(i);
            final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(0);
            slot.setIndication(indication);
        }
    }

    private void setupHorizontalLaunching(final MiniLab3Extension driver, final RgbButton[] buttons) {
        for (int i = 0; i < MiniLab3Extension.NUM_PADS_TRACK; i++) {
            final int index = i;
            final Track track = driver.getViewTrackBank().getItemAt(i);
            final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(0);
            prepareSlot(slot);
            track.isQueuedForStop().markInterested();
            final boolean indication = driver.getPadBank().get() == PadBank.BANK_A;
            slot.setIndication(indication);
            slot.color().addValueObserver((r, g, b) -> sceneSlotColors[index] = RgbLightState.getColor(r, g, b));
            final RgbButton button = buttons[i];
            button.bindPressed(sceneLaunchingLayer, pressed -> handleSlotSelected(index, track, slot, pressed),
                    () -> getLightState(index, track, slot));
        }
    }

    public void navigateScenes(final int direction) {
        trackBank.sceneBank().scrollBy(direction);
    }

    public void launchScene() {
        trackBank.sceneBank().getScene(0).launch();
    }

    private void prepareSlot(final ClipLauncherSlot cs) {
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
        final RgbLightState color = sceneSlotColors[index];
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
            if (slot.isPlaying().get()) {
                return color.getBrighter();
            }
            return color.getDarker();
        }
        if (slot.isRecordingQueued().get()) {
            return blinkFast(color.getDarker(), RgbLightState.RED);
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
        sceneLaunchingLayer.activate();
        activateIndication(driver.getPadBank().get() == PadBank.BANK_A);
    }

    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        sceneLaunchingLayer.deactivate();
        activateIndication(false);
    }

}
