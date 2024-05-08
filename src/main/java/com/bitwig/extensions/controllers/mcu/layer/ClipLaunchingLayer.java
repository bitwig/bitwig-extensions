package com.bitwig.extensions.controllers.mcu.layer;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.mcu.GlobalStates;
import com.bitwig.extensions.controllers.mcu.TimedProcessor;
import com.bitwig.extensions.controllers.mcu.ViewControl;
import com.bitwig.extensions.controllers.mcu.config.ControllerConfig;
import com.bitwig.extensions.controllers.mcu.config.McuFunction;
import com.bitwig.extensions.controllers.mcu.control.McuButton;
import com.bitwig.extensions.controllers.mcu.control.MixerSectionHardware;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Context;
import com.bitwig.extensions.framework.values.LayoutType;

public class ClipLaunchingLayer extends Layer {
    private final Layer launcherLayer;
    //private final Layer arrangerLayer;
    private final TrackBank trackBank;
    private final LayoutType layoutType = LayoutType.LAUNCHER;
    private final TimedProcessor timedProcessor;
    private final int trackOffset;
    private final GlobalStates gobalStates;
    
    public ClipLaunchingLayer(final Context diContext, final MixerSectionHardware hwElements,
        final MixerSection mixerSection) {
        super(diContext.getService(Layers.class), "CLIP_LAUNCH_%d".formatted(mixerSection.getSectionIndex() + 1));
        final ControllerConfig config = diContext.getService(ControllerConfig.class);
        timedProcessor = diContext.getService(TimedProcessor.class);
        launcherLayer = diContext.createLayer("VERTICAL_%d".formatted(mixerSection.getSectionIndex() + 1));
        //arrangerLayer = diContext.createLayer("HORIZONTAL_%d".formatted(mixerSection.getSectionIndex() + 1));
        trackOffset = mixerSection.getSectionIndex() * 8;
        trackBank = diContext.getService(ViewControl.class).getMainTrackBank();
        this.gobalStates = diContext.getService(GlobalStates.class);
        final boolean use2Lanes = config.getAssignment(McuFunction.CLIP_LAUNCHER_MODE_2) != null;
        final int numberOfScenes = use2Lanes ? 2 : 4;
        
        for (int index = 0; index < 8; index++) {
            final int trackIndex = index + trackOffset;
            final Track track = trackBank.getItemAt(trackIndex);
            final ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
            for (int slotIndex = 0; slotIndex < numberOfScenes; slotIndex++) {
                final ClipLauncherSlot slot = slotBank.getItemAt(slotIndex);
                prepareSlot(slot);
                final McuButton button = use2Lanes
                    ? hwElements.getButtonFromGridBy2Lane(slotIndex, index)
                    : hwElements.getButtonFromGridBy4Lane(slotIndex, index);
                button.bindLight(launcherLayer, () -> getLightState(slot));
                button.bindIsPressed(launcherLayer, pressed -> handleSlotPressed(slot, pressed));
            }
        }
    }
    
    private static void prepareSlot(final ClipLauncherSlot slot) {
        slot.exists().markInterested();
        slot.hasContent().markInterested();
        slot.isPlaying().markInterested();
        slot.isPlaybackQueued().markInterested();
        slot.isRecording().markInterested();
        slot.isRecordingQueued().markInterested();
        slot.isStopQueued().markInterested();
    }
    
    private boolean getLightState(final ClipLauncherSlot slot) {
        if (!slot.exists().get()) {
            return false;
        }
        if (slot.hasContent().get()) {
            if (slot.isPlaybackQueued().get() || slot.isRecordingQueued().get() || slot.isPlaybackQueued().get()
                || slot.isStopQueued().get()) {
                return timedProcessor.blinkMid();
            } else if (slot.isRecording().get()) {
                return timedProcessor.blinkPeriodic();
            } else if (slot.isPlaying().get()) {
                return timedProcessor.blinkSlow();
            }
            return true;
        }
        if (slot.isPlaybackQueued().get() || slot.isRecordingQueued().get()) {
            return timedProcessor.blinkFast();
        }
        
        return false;
    }
    
    private void handleSlotPressed(final ClipLauncherSlot slot, final boolean pressed) {
        if (gobalStates.getClearHeld().get()) {
            if (pressed) {
                slot.deleteObject();
            }
        } else if (gobalStates.getDuplicateHeld().get()) {
            if (pressed) {
                slot.duplicateClip();
            }
        } else {
            if (pressed) {
                slot.launch();
            } else {
                slot.launchRelease();
            }
        }
    }
    
    @Override
    protected void onActivate() {
        applyLayer();
    }
    
    private void applyLayer() {
        if (!isActive()) {
            return;
        }
        //        if (layoutType == LayoutType.ARRANGER) {
        //            launcherLayer.deactivate();
        //            arrangerLayer.activate();
        //        } else {
        //            arrangerLayer.deactivate();
        //            launcherLayer.activate();
        //        }
        launcherLayer.activate();
        trackBank.setShouldShowClipLauncherFeedback(true);
    }
    
    @Override
    protected void onDeactivate() {
        trackBank.setShouldShowClipLauncherFeedback(false);
        launcherLayer.deactivate();
        //arrangerLayer.deactivate();
    }
    
}
