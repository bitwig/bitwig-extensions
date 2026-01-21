package com.bitwig.extensions.controllers.allenheath.xonek3.layer;

import java.util.List;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.allenheath.xonek3.DeviceHwElements;
import com.bitwig.extensions.controllers.allenheath.xonek3.ViewControl;
import com.bitwig.extensions.controllers.allenheath.xonek3.XoneHwElements;
import com.bitwig.extensions.controllers.allenheath.xonek3.XoneK3GlobalStates;
import com.bitwig.extensions.controllers.allenheath.xonek3.color.XoneRgbColor;
import com.bitwig.extensions.controllers.allenheath.xonek3.control.XoneRgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.FocusMode;
import com.bitwig.extensions.framework.values.PanelLayout;

@Component
public class ClipSceneControl {
    private final XoneRgbColor[][] slotColors;
    private final XoneRgbColor[] sceneColors = new XoneRgbColor[4];
    
    private final Transport transport;
    private final ViewControl viewControl;
    private final BooleanValueObject shiftHeld;
    private final XoneK3GlobalStates globalStates;
    private FocusMode recordFocusMode;
    private final Track rootTrack;
    
    protected SettableBooleanValue clipLauncherOverdub;
    
    private final Layer clipLaunchLayer;
    private final Layer sceneLaunchLayer;
    
    private int sceneOffset;
    
    public ClipSceneControl(final ControllerHost host, final LayerCollection layers, final XoneHwElements hwElements,
        final ViewControl viewControl, final Transport transport, final XoneK3GlobalStates globalStates,
        final Application application) {
        slotColors = new XoneRgbColor[4 * globalStates.getDeviceCount()][4];
        clipLaunchLayer = layers.getLayer(LayerId.CLIP_LAUNCHER);
        sceneLaunchLayer = layers.getLayer(LayerId.SCENE_LAUNCHER);
        final Layer layerChooserLayer = layers.getLayer(LayerId.LAYER_CHOOSER);
        this.transport = transport;
        rootTrack = host.getProject().getRootTrackGroup();
        this.shiftHeld = globalStates.getShiftHeld();
        this.viewControl = viewControl;
        this.globalStates = globalStates;
        application.panelLayout().addValueObserver(this::handlePanelLayoutChanged);
        this.clipLauncherOverdub = transport.isClipLauncherOverdubEnabled();
        this.clipLauncherOverdub.markInterested();
        this.transport.isPlaying().markInterested();
        recordFocusMode = FocusMode.LAUNCHER;
        
        final SettableEnumValue recordButtonAssignment = host.getDocumentState().getEnumSetting(
            "Record Button assignment", //
            "Transport", new String[] {FocusMode.LAUNCHER.getDescriptor(), FocusMode.ARRANGER.getDescriptor()},
            recordFocusMode.getDescriptor());
        recordButtonAssignment.addValueObserver(value -> recordFocusMode = FocusMode.toMode(value));
        
        
        final TrackBank trackBank = viewControl.getTrackBank();
        for (int i = 0; i < trackBank.getSizeOfBank(); i++) {
            final int trackIndex = i;
            final Track track = trackBank.getItemAt(i);
            for (int j = 0; j < trackBank.sceneBank().getSizeOfBank(); j++) {
                final int sceneIndex = j;
                slotColors[trackIndex][sceneIndex] = XoneRgbColor.OFF;
                final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(j);
                prepareSlot(slot, trackIndex, sceneIndex);
                final XoneRgbButton button = hwElements.getGridButton(trackIndex, sceneIndex);
                button.bindLight(clipLaunchLayer, () -> determineColor(track, slot, trackIndex, sceneIndex));
                button.bindPressed(clipLaunchLayer, () -> launchSlot(slot, trackIndex, sceneIndex));
                button.bindRelease(clipLaunchLayer, () -> releaseSlot(slot, trackIndex, sceneIndex));
            }
        }
        bindSceneLaunching(hwElements, trackBank);
        bindTransportOnGrid(layerChooserLayer, hwElements.getDeviceElements(0).getKnobButtons(), transport);
    }
    
    private void bindSceneLaunching(final XoneHwElements hwElements, final TrackBank trackBank) {
        final SceneBank sceneBank = trackBank.sceneBank();
        sceneBank.scrollPosition().addValueObserver(value -> sceneOffset = value);
        final DeviceHwElements deviceElements = hwElements.getDeviceElements(0);
        for (int i = 0; i < 4; i++) {
            final int sceneIndex = i;
            final Scene scene = sceneBank.getScene(i);
            scene.color().addValueObserver(
                (r, g, b) -> sceneColors[sceneIndex] = XoneRgbColor.of(r, g, b, 0x20, XoneRgbColor.GREEN_DIM));
            scene.clipCount().markInterested();
            scene.exists().markInterested();
            final XoneRgbButton sceneButton = deviceElements.getGridButtons().get(i * 4);
            sceneButton.bindLight(sceneLaunchLayer, () -> getSceneState(sceneIndex, scene));
            sceneButton.bindPressed(sceneLaunchLayer, () -> launchScene(scene));
            sceneButton.bindRelease(sceneLaunchLayer, () -> releaseScene(scene));
            for (int j = 1; j < globalStates.getDeviceCount() * 4; j++) {
                final XoneRgbButton button = hwElements.getGridButton(j, i);
                if (j != 3 || i != 3) {
                    button.bindDisabled(sceneLaunchLayer);
                }
            }
        }
        
        
        final Track rootTrack = viewControl.getRootTrack();
        final XoneRgbButton stopAllButton = deviceElements.getGridButtons().get(15);
        stopAllButton.bindPressed(sceneLaunchLayer, () -> rootTrack.stop());
        stopAllButton.bindLightPressed(sceneLaunchLayer, XoneRgbColor.WHITE, XoneRgbColor.WHITE_DIM);
    }
    
    
    private void bindTransportOnGrid(final Layer layer, final List<XoneRgbButton> buttons, final Transport transport) {
        final XoneRgbButton playButton = buttons.get(11);
        transport.isPlaying().markInterested();
        playButton.bindPressed(layer, () -> transport.play());
        playButton.bindLight(layer, () -> transport.isPlaying().get() ? XoneRgbColor.GREEN : XoneRgbColor.GREEN_DIM);
        
        final XoneRgbButton stopButton = buttons.get(10);
        stopButton.bindPressed(layer, () -> transport.stop());
        stopButton.bindLight(layer, () -> transport.isPlaying().get() ? XoneRgbColor.WHITE : XoneRgbColor.WHITE_DIM);
        final XoneRgbButton recButton = buttons.get(9);
        transport.isArrangerRecordEnabled().markInterested();
        transport.isArrangerOverdubEnabled().markInterested();
        recButton.bindPressed(layer, () -> transport.record());
        recButton.bindLight(
            layer, () -> transport.isArrangerRecordEnabled().get() ? XoneRgbColor.RED : XoneRgbColor.RED_DIM);
        
        final XoneRgbButton overdubArrangeButton = buttons.get(8);
        transport.isArrangerAutomationWriteEnabled().markInterested();
        overdubArrangeButton.bindPressed(layer, () -> toggleOverdub(transport));
        overdubArrangeButton.bindLight(layer, () -> overdubColorState(transport));
    }
    
    private XoneRgbColor overdubColorState(final Transport transport) {
        if (recordFocusMode == FocusMode.ARRANGER) {
            return transport.isArrangerOverdubEnabled().get() ? XoneRgbColor.RED : XoneRgbColor.RED_DIM;
        } else {
            return transport.isClipLauncherOverdubEnabled().get() ? XoneRgbColor.RED : XoneRgbColor.RED_DIM;
        }
    }
    
    private void toggleOverdub(final Transport transport) {
        if (recordFocusMode == FocusMode.ARRANGER) {
            transport.isArrangerOverdubEnabled().toggle();
        } else {
            transport.isClipLauncherOverdubEnabled().toggle();
        }
    }
    
    private void launchScene(final Scene scene) {
        if (shiftHeld.get()) {
            scene.launchAlt();
        } else {
            scene.launch();
        }
    }
    
    private void releaseScene(final Scene scene) {
        if (shiftHeld.get()) {
            scene.launchReleaseAlt();
        } else {
            scene.launchRelease();
        }
    }
    
    private XoneRgbColor getSceneState(final int sceneIndex, final Scene scene) {
        if (!scene.exists().get()) {
            return XoneRgbColor.OFF;
        }
        if (viewControl.hasQueuedClips(sceneOffset + sceneIndex)) {
            return globalStates.blinkFast(sceneColors[sceneIndex]);
        }
        if (viewControl.hasPlayingClips(sceneOffset + sceneIndex)) {
            return globalStates.pulse(sceneColors[sceneIndex]);
        }
        
        return sceneColors[sceneIndex];
    }
    
    @Activate
    public void activate() {
        clipLaunchLayer.setIsActive(true);
        sceneLaunchLayer.setIsActive(false);
    }
    
    private void prepareSlot(final ClipLauncherSlot slot, final int trackIndex, final int sceneIndex) {
        slot.hasContent().markInterested();
        slot.isPlaying().markInterested();
        slot.isStopQueued().markInterested();
        slot.isRecordingQueued().markInterested();
        slot.isRecording().markInterested();
        slot.isPlaybackQueued().markInterested();
        slot.color().addValueObserver((r, g, b) -> slotColors[trackIndex][sceneIndex] = XoneRgbColor.of(r, g, b));
    }
    
    private void launchSlot(final ClipLauncherSlot slot, final int trackIndex, final int sceneIndex) {
        if (shiftHeld.get()) {
            slot.launchAlt();
        } else {
            slot.launch();
        }
    }
    
    private void releaseSlot(final ClipLauncherSlot slot, final int trackIndex, final int sceneIndex) {
        if (shiftHeld.get()) {
            slot.launchReleaseAlt();
        } else {
            slot.launchRelease();
        }
    }
    
    private XoneRgbColor determineColor(final Track track, final ClipLauncherSlot slot, final int trackIndex,
        final int sceneIndex) {
        final XoneRgbColor color = slotColors[trackIndex][sceneIndex];
        if (slot.hasContent().get()) {
            if (slot.isRecordingQueued().get()) {
                return globalStates.blinkMid(XoneRgbColor.RED);
            } else if (slot.isRecording().get()) {
                return globalStates.pulse(XoneRgbColor.RED);
            } else if (slot.isPlaybackQueued().get()) {
                return globalStates.blinkFast(color);
            } else if (slot.isStopQueued().get()) {
                return globalStates.blinkFast(XoneRgbColor.GREEN);
            } else if (slot.isPlaying().get() && track.isQueuedForStop().get()) {
                return globalStates.pulse(XoneRgbColor.GREEN);
            } else if (slot.isPlaying().get()) {
                if (clipLauncherOverdub.get() && track.arm().get()) {
                    return globalStates.pulse(XoneRgbColor.RED);
                } else {
                    if (transport.isPlaying().get()) {
                        return globalStates.pulse(XoneRgbColor.GREEN);
                    }
                    return XoneRgbColor.GREEN;
                }
            }
            return color;
        }
        if (slot.isRecordingQueued().get()) {
            return globalStates.blinkFast(XoneRgbColor.RED);
        }
        return XoneRgbColor.OFF;
    }
    
    private void handlePanelLayoutChanged(final String value) {
        final PanelLayout panelLayout;
        if (value.equals("MIX")) {
            panelLayout = PanelLayout.LAUNCHER;
        } else {
            panelLayout = PanelLayout.ARRANGER;
        }
    }
    
}