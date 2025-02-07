package com.bitwig.extensions.controllers.novation.slmk3.layer;

import java.util.Arrays;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.novation.slmk3.CcAssignment;
import com.bitwig.extensions.controllers.novation.slmk3.GlobalStates;
import com.bitwig.extensions.controllers.novation.slmk3.OverviewGrid;
import com.bitwig.extensions.controllers.novation.slmk3.SlMk3Extension;
import com.bitwig.extensions.controllers.novation.slmk3.SlMk3HardwareElements;
import com.bitwig.extensions.controllers.novation.slmk3.ViewControl;
import com.bitwig.extensions.controllers.novation.slmk3.control.RgbButton;
import com.bitwig.extensions.controllers.novation.slmk3.display.ScreenHandler;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlotUtil;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

@Component
public class ClipLauncherLayer extends Layer {
    private final OverviewGrid overviewGrid;
    private final Layer padLayer;
    private final Layer selectLayer;
    private final ControllerHost host;
    private final Layer modifierLayer;
    private final GlobalStates globalStates;
    private boolean overdubActive;
    private final SlRgbState[] sceneSlotColors = new SlRgbState[16];
    private final SlRgbState[] sceneColors = new SlRgbState[2];
    private final TrackBank trackBank;
    
    @Inject
    private Project project;
    
    @Inject
    private ScreenHandler screenHandler;
    
    public ClipLauncherLayer(final Layers layers, final ViewControl viewControl, final SlMk3HardwareElements hwElements,
        final LayerRepo layerRepo, final Transport transport, final ControllerHost host,
        final GlobalStates globalStates) {
        super(layers, "CLIP LAUNCHER");
        this.globalStates = globalStates;
        padLayer = layerRepo.getPadClipLayer();
        selectLayer = layerRepo.getSelectClipLayer();
        modifierLayer = layerRepo.getPadModifierLayer();
        this.host = host;
        Arrays.fill(sceneSlotColors, SlRgbState.OFF);
        transport.isClipLauncherOverdubEnabled().addValueObserver(overdubActive -> this.overdubActive = overdubActive);
        trackBank = viewControl.getTrackBank();
        trackBank.setShouldShowClipLauncherFeedback(true);
        overviewGrid = viewControl.getOverviewGrid();
        for (int i = 0; i < 2; i++) {
            final int sceneIndex = i;
            for (int j = 0; j < 8; j++) {
                final int trackIndex = j;
                final Track track = trackBank.getItemAt(trackIndex);
                final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
                final int buttonIndex = sceneIndex * 8 + trackIndex;
                prepareSlot(slot, buttonIndex);
                final RgbButton button = hwElements.getPadButtons().get(buttonIndex);
                button.bindIsPressed(padLayer, pressed -> this.handleSlotPress(pressed, slot));
                button.bindLight(padLayer, () -> getColor(slot, track, buttonIndex));
                button.bindPressed(selectLayer, () -> this.handleSlotSelect(slot, track));
                button.bindLight(selectLayer, () -> getSelectColor(slot, track, buttonIndex));
            }
        }
        
        globalStates.getShiftState().addValueObserver(modActive -> modifierLayer.setIsActive(modActive));
        
        final RgbButton sceneLaunch1 = hwElements.getButton(CcAssignment.SCENE_LAUNCH_1);
        final RgbButton sceneLaunch2 = hwElements.getButton(CcAssignment.SCENE_LAUNCH_2);
        final Scene scene1 = trackBank.sceneBank().getScene(0);
        final Scene scene2 = trackBank.sceneBank().getScene(1);
        scene1.color().addValueObserver((r, g, b) -> sceneColors[0] = SlRgbState.get(r, g, b));
        scene2.color().addValueObserver((r, g, b) -> sceneColors[1] = SlRgbState.get(r, g, b));
        sceneLaunch1.bindIsPressed(padLayer, pressed -> handleSceneLaunch(scene1, pressed));
        sceneLaunch1.bindLight(padLayer, () -> getSceneColor(scene1, 0));
        
        sceneLaunch2.bindIsPressed(padLayer, pressed -> handleSceneLaunch(scene2, pressed));
        sceneLaunch2.bindLight(padLayer, () -> getSceneColor(scene2, 1));
        
        initNavigation(hwElements, viewControl);
    }
    
    
    private SlRgbState getSceneColor(final Scene scene, final int index) {
        if (overviewGrid.hasQueuedScenes(index)) {
            return SlRgbState.WHITE_BLINK;
        }
        return sceneColors[index];
    }
    
    private void handleSceneLaunch(final Scene scene, final Boolean pressed) {
        if (globalStates.getShiftState().get()) {
            if (pressed) {
                scene.launchAlt();
            } else {
                scene.launchReleaseAlt();
            }
        } else {
            if (globalStates.getClearState().get() && pressed) {
                scene.deleteObject();
                screenHandler.notifyMessage("Delete", "Scene");
            } else if (globalStates.getDuplicateState().get() && pressed) {
                scene.nextSceneInsertionPoint().copySlotsOrScenes(scene);
                screenHandler.notifyMessage("Duplicate", "Scene");
            } else {
                if (pressed) {
                    scene.launch();
                } else {
                    scene.launchRelease();
                }
            }
        }
    }
    
    private void initNavigation(final SlMk3HardwareElements hwElements, final ViewControl viewControl) {
        final RgbButton sceneNavUp = hwElements.getButton(CcAssignment.PADS_UP);
        final RgbButton sceneNavDown = hwElements.getButton(CcAssignment.PADS_DOWN);
        
        final SceneBank sceneBank = trackBank.sceneBank();
        sceneBank.canScrollBackwards().markInterested();
        sceneBank.canScrollForwards().markInterested();
        trackBank.canScrollBackwards().markInterested();
        trackBank.canScrollForwards().markInterested();
        
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        cursorTrack.hasNext().markInterested();
        cursorTrack.hasPrevious().markInterested();
        
        sceneNavUp.bindLight(padLayer, () -> sceneBank.canScrollBackwards().get() ? SlRgbState.WHITE : SlRgbState.OFF);
        sceneNavDown.bindLight(padLayer, () -> sceneBank.canScrollForwards().get() ? SlRgbState.WHITE : SlRgbState.OFF);
        sceneNavUp.bindRepeatHold(
            padLayer, () -> sceneBank.scrollBackwards(), SlMk3Extension.REPEAT_DELAY, SlMk3Extension.REPEAT_FREQUENCY);
        sceneNavDown.bindRepeatHold(
            padLayer, () -> sceneBank.scrollForwards(), SlMk3Extension.REPEAT_DELAY, SlMk3Extension.REPEAT_FREQUENCY);
        
        sceneNavUp.bindLightOnPressed(modifierLayer, SlRgbState.ORANGE);
        sceneNavUp.bindPressed(modifierLayer, () -> {
            project.createSceneFromPlayingLauncherClips();
            screenHandler.notifyMessage("Capture", "Scene");
        });
        sceneNavDown.bindLightOnPressed(modifierLayer, SlRgbState.ORANGE);
        sceneNavDown.bindPressed(modifierLayer, () -> {
            project.createScene();
            screenHandler.notifyMessage("Insert", "Scene");
        });
    }
    
    @Activate
    public void doActivate() {
        this.activate();
        this.padLayer.setIsActive(true);
    }
    
    private SlRgbState getColor(final ClipLauncherSlot slot, final Track track, final int index) {
        return SlotUtil.determineClipColor(slot, track, sceneSlotColors[index], overdubActive);
    }
    
    private SlRgbState getSelectColor(final ClipLauncherSlot slot, final Track track, final int index) {
        if (slot.isSelected().get()) {
            return SlRgbState.WHITE;
        }
        return SlotUtil.determineClipColor(slot, track, sceneSlotColors[index], overdubActive);
    }
    
    private void handleSlotSelect(final ClipLauncherSlot slot, final Track track) {
        if (!track.canHoldNoteData().get()) {
            return;
        }
        track.selectInEditor();
        if (!slot.hasContent().get()) {
            slot.createEmptyClip(4);
        }
        host.scheduleTask(() -> {
            slot.select();
        }, 100);
    }
    
    
    private void handleSlotPress(final boolean pressed, final ClipLauncherSlot slot) {
        if (globalStates.getShiftState().get()) {
            if (pressed) {
                slot.launchAlt();
            } else {
                slot.launchReleaseAlt();
            }
        } else {
            if (globalStates.getClearState().get() && pressed) {
                slot.deleteObject();
                screenHandler.notifyMessage("Delete", "Clip");
            } else if (globalStates.getDuplicateState().get() && pressed) {
                slot.duplicateClip();
                screenHandler.notifyMessage("Duplicate", "Clip");
            } else {
                if (pressed) {
                    slot.launch();
                } else {
                    slot.launchRelease();
                }
            }
        }
    }
    
    private void prepareSlot(final ClipLauncherSlot cs, final int index) {
        cs.color().addValueObserver((r, g, b) -> sceneSlotColors[index] = SlRgbState.get(r, g, b));
        SlotUtil.prepareSlot(cs);
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        modifierLayer.setIsActive(false);
    }
}
