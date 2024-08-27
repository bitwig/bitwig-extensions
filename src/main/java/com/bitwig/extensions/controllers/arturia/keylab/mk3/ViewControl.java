package com.bitwig.extensions.controllers.arturia.keylab.mk3;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.PostConstruct;
import com.bitwig.extensions.framework.values.BooleanValueObject;

@Component
public class ViewControl {
    private static final String ANALOG_LAB_V_DEVICE_ID = "4172747541564953416C617650726F63";
    private final TrackBank viewTrackBank;
    private final PinnableCursorDevice primaryDevice;
    private final CursorTrack cursorTrack;
    private final TrackBank mixerTrackBank;
    private final Scene sceneTrackItem;
    private final PinnableCursorDevice cursorDevice;
    private final Clip cursorClip;
    private final CursorRemoteControlsPage primaryRemotes;
    private final CursorRemoteControlsPage sliderRemotes;
    private String[] devicePageNames = {};
    private int primaryPageCount;
    private int primaryIndex;
    private final SceneBank sceneBank;
    private final SceneFocus sceneFocus;
    private BooleanValueObject controlsAnalogLab;
    public static final int NUM_PADS_TRACK = 8;
    
    public ViewControl(final ControllerHost host) {
        mixerTrackBank = host.createTrackBank(8, 2, 2);
        cursorTrack = host.createCursorTrack(2, 2);
        
        viewTrackBank = host.createTrackBank(4, 2, 3);
        viewTrackBank.followCursorTrack(cursorTrack);
        mixerTrackBank.followCursorTrack(cursorTrack);
        viewTrackBank.sceneBank().scrollPosition().markInterested();
        
        sceneBank = host.createSceneBank(1);
        sceneFocus = new SceneFocus(host, sceneBank.getScene(0), 64);
        sceneBank.setIndication(true);
        sceneBank.scrollPosition().addValueObserver(pos -> {
            sceneFocus.setPosition(pos);
            viewTrackBank.sceneBank().scrollPosition().set(pos);
        });
        
        sceneTrackItem = viewTrackBank.sceneBank().getScene(0);
        cursorClip = host.createLauncherCursorClip(32, 127);
        
        primaryDevice = cursorTrack.createCursorDevice("DrumDetection", "Pad Device", NUM_PADS_TRACK,
            CursorDeviceFollowMode.FIRST_INSTRUMENT);
        cursorDevice = cursorTrack.createCursorDevice("device-control", "Device Control", 0,
            CursorDeviceFollowMode.FOLLOW_SELECTION);
        cursorDevice.isWindowOpen().markInterested();
        
        primaryRemotes = cursorDevice.createCursorRemoteControlsPage(8);
        sliderRemotes = cursorDevice.createCursorRemoteControlsPage("page-2-remotes", 8, null);
        sliderRemotes.selectedPageIndex().markInterested();
        primaryRemotes.selectedPageIndex().markInterested();
        primaryRemotes.selectedPageIndex().addValueObserver(primaryIndex -> {
            this.primaryIndex = primaryIndex;
            final int nextIndex = primaryIndex + 1 < primaryPageCount ? primaryIndex + 1 : 0;
            sliderRemotes.selectedPageIndex().set(nextIndex);
        });
        primaryRemotes.pageCount().addValueObserver(pages -> {
            this.primaryPageCount = pages;
            final int nextIndex = primaryIndex + 1 < primaryPageCount ? primaryIndex + 1 : 0;
            sliderRemotes.selectedPageIndex().set(nextIndex);
        });
        primaryRemotes.pageNames().addValueObserver(pageNames -> {
            devicePageNames = pageNames;
            if (devicePageNames != null && devicePageNames.length > 0
                && primaryRemotes.selectedPageIndex().get() == -1) {
                //mainParameterBankIndexChanged(parameterBank2, parameterBank1.selectedPageIndex().get());
            }
        });
        
        setUpFollowArturiaDevice(host);
    }
    
    @PostConstruct
    void init() {
        viewTrackBank.canScrollForwards().markInterested();
        viewTrackBank.canScrollBackwards().markInterested();
        cursorTrack.hasNext().markInterested();
        cursorTrack.hasPrevious().markInterested();
    }
    
    private void setUpFollowArturiaDevice(final ControllerHost host) {
        final DeviceMatcher arturiaMatcher = host.createVST3DeviceMatcher(ANALOG_LAB_V_DEVICE_ID);
        final DeviceBank matcherBank = cursorTrack.createDeviceBank(1);
        matcherBank.setDeviceMatcher(arturiaMatcher);
        final Device matcherDevice = matcherBank.getItemAt(0);
        matcherDevice.exists().markInterested();
        
        controlsAnalogLab = new BooleanValueObject();
        
        final BooleanValue onArturiaDevice = cursorDevice.createEqualsValue(matcherDevice);
        cursorTrack.arm().addValueObserver(
            armed -> controlsAnalogLab.set(armed && cursorDevice.exists().get() && onArturiaDevice.get()));
        onArturiaDevice.addValueObserver(
            onArturia -> controlsAnalogLab.set(cursorTrack.arm().get() && cursorDevice.exists().get() && onArturia));
        cursorDevice.exists().addValueObserver(cursorDeviceExists -> controlsAnalogLab.set(
            cursorTrack.arm().get() && cursorDeviceExists && onArturiaDevice.get()));
    }
    
    public void navigateTracks(final int inc) {
        if (inc > 0) {
            cursorTrack.selectNext();
        } else {
            cursorTrack.selectPrevious();
        }
    }
    
    public CursorRemoteControlsPage getPrimaryRemotes() {
        return primaryRemotes;
    }
    
    public CursorRemoteControlsPage getSliderRemotes() {
        return sliderRemotes;
    }
    
    public Scene getSceneTrackItem() {
        return sceneTrackItem;
    }
    
    public TrackBank getViewTrackBank() {
        return viewTrackBank;
    }
    
    public SceneFocus getSceneFocus() {
        return sceneFocus;
    }
    
    public CursorTrack getCursorTrack() {
        return cursorTrack;
    }
    
    public PinnableCursorDevice getCursorDevice() {
        return cursorDevice;
    }
    
    public PinnableCursorDevice getPrimaryDevice() {
        return primaryDevice;
    }
    
    public TrackBank getMixerTrackBank() {
        return mixerTrackBank;
    }
    
    public BooleanValueObject getControlsAnalogLab() {
        return controlsAnalogLab;
    }
    
    public void invokeQuantize() {
        cursorClip.quantize(1.0);
        final ClipLauncherSlot slot = cursorClip.clipLauncherSlot();
        slot.showInEditor();
    }
    
    public SceneBank getSceneBank() {
        return sceneBank;
    }
    
    public void navigateScenes(final int direction) {
        sceneBank.scrollBy(direction);
        //viewTrackBank.sceneBank().scrollBy(direction);
    }
    
    public void launchScene() {
        sceneBank.getScene(0).launch();
    }
    
    public void navigateDevices(final int dir) {
        if (dir > 0) {
            cursorDevice.selectNext();
        } else {
            cursorDevice.selectPrevious();
        }
    }
    
    public void navigateMixer(final int dir) {
        mixerTrackBank.scrollBy(dir);
    }
    
    public void showPlugin(final boolean visible) {
        cursorDevice.isWindowOpen().set(visible);
    }
}
