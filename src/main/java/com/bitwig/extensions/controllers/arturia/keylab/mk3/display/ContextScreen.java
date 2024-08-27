package com.bitwig.extensions.controllers.arturia.keylab.mk3.display;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.CcAssignment;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.KeylabHardwareElements;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.MidiProcessor;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.SceneFocus;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.ViewControl;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.color.RgbColor;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.color.RgbLightState;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.color.SceneState;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.controls.RgbButton;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.controls.TouchEncoder;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.controls.TouchSlider;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.LayoutType;

@Component
public class ContextScreen {
    
    private final MidiProcessor midiProcessor;
    private final ViewControl viewControl;
    private final Layer mainLayer;
    private ControlMode controlMode = ControlMode.MIXER;
    private EncoderControl encoderMode = EncoderControl.TRACKS;
    private boolean hasNextScene = false;
    private boolean hasPreviousScene = false;
    private boolean muteState = false;
    private boolean soloState = false;
    private boolean armState = false;
    private boolean sceneButtonPressed = false;
    private String deviceName;
    private int mixerOffset;
    private boolean deviceExists = false;
    
    private final TrackSelectionState trackSelectionState = new TrackSelectionState();
    private final Layer mixerLayer;
    private final Layer deviceLayer;
    private LayoutType panelLayout;
    private final SceneFocus sceneFocus;
    
    private enum ControlMode {
        MIXER,
        DEVICE
    }
    
    private enum EncoderControl {
        TRACKS,
        DEVICE,
        MIXER,
        SPECIAL
    }
    
    private class TrackSelectionState {
        private String trackName = "";
        private String sceneName = "";
        private RgbColor trackColor = RgbColor.BLACK;
    }
    
    public ContextScreen(final Layers layers, final MidiProcessor midiProcessor, final ViewControl viewControl,
        final KeylabHardwareElements hwElements, final Application application) {
        this.midiProcessor = midiProcessor;
        this.viewControl = viewControl;
        this.mainLayer = new Layer(layers, "CONTEXT_SCREEN");
        this.mixerLayer = new Layer(layers, "MIXER_LAYER");
        this.deviceLayer = new Layer(layers, "DEVICE_LAYER");
        
        sceneFocus = viewControl.getSceneFocus();
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        final PinnableCursorDevice cursorDevice = viewControl.getCursorDevice();
        final TouchEncoder mainEncoder = hwElements.getMainEncoder();
        mainEncoder.bind(mainLayer, this::handleEncoderTurn);
        mainEncoder.bindPressed(mainLayer, () -> {
            viewControl.showPlugin(true);
            cursorTrack.sendMidi(0XB0, 117, 127);
        });
        final RgbButton backButton = hwElements.getButton(CcAssignment.BACK);
        backButton.bindPressed(mainLayer, () -> viewControl.showPlugin(false));
        backButton.bindReleased(mainLayer, () -> backButton.refreshLight());
        backButton.bindLight(mainLayer, () -> {
            if (cursorDevice.isWindowOpen().get()) {
                return RgbLightState.WHITE_DIMMED;
            }
            return RgbLightState.OFF;
        });
        cursorDevice.isWindowOpen().addValueObserver(open -> {
            backButton.setLightState(open ? RgbLightState.WHITE_DIMMED : RgbLightState.OFF);
        });
        
        setUpContextButtons(viewControl, hwElements);
        
        final MultiStateHardwareLight sceneStateDisplay = hwElements.getSceneStateDisplay();
        sceneStateDisplay.state().onUpdateHardware(this::updateSceneState);
        mainLayer.bindLightState(this::getSceneState, sceneStateDisplay);
        
        viewControl.getMixerTrackBank().scrollPosition().addValueObserver(pos -> {
            this.mixerOffset = pos;
            showTrackAssignment(false);
        });
        application.panelLayout().addValueObserver(value -> {
            panelLayout = LayoutType.toType(value);
            updatePreviousButton();
            updateNextButton();
        });
        
        cursorDevice.name().addValueObserver(name -> updateDeviceName(name));
        cursorDevice.exists().addValueObserver(exists -> {
            this.deviceExists = exists;
            showDeviceName(false);
        });
        
        cursorTrack.mute().addValueObserver(muted -> {
            this.muteState = muted;
            updateMuteState();
        });
        cursorTrack.solo().addValueObserver(solo -> {
            this.soloState = solo;
            updateSoloState();
        });
        cursorTrack.arm().addValueObserver(armed -> {
            this.armState = armed;
            updateArmState();
        });
        cursorTrack.name().addValueObserver(name -> {
            trackSelectionState.trackName = name;
            updateMainScreen();
        });
        cursorTrack.color().addValueObserver((r, g, b) -> {
            trackSelectionState.trackColor = RgbColor.getColor(r, g, b);
            updateMainScreen();
        });
        final Scene scene = viewControl.getSceneBank().getScene(0);
        scene.name().addValueObserver(name -> {
            trackSelectionState.sceneName = name;
            updateMainScreen();
        });
        bindControls(hwElements);
    }
    
    private SceneState getSceneState() {
        return sceneFocus.getSceneState(midiProcessor.getBlinkCounter(), sceneButtonPressed);
    }
    
    private void updateSceneState(final InternalHardwareLightState internalHardwareLightState) {
        if (internalHardwareLightState instanceof SceneState sceneState) {
            final ButtonDisplayType type =
                sceneState.isHeld() ? ButtonDisplayType.TOGGLE_ICON : ButtonDisplayType.SIMPLE_ICON;
            final SmallIcon icon = sceneState.isNoClips() ? SmallIcon.STOP : SmallIcon.ARROW_RIGHT;
            midiProcessor.screenContextButton(4, type, icon, sceneState.getColor(), "");
        }
    }
    
    private void updateDeviceName(final String name) {
        this.deviceName = name;
        showDeviceName(false);
    }
    
    private void handleEncoderTurn(final int dir) {
        if (encoderMode == EncoderControl.TRACKS) {
            viewControl.navigateTracks(dir);
        } else if (encoderMode == EncoderControl.DEVICE) {
            viewControl.navigateDevices(dir);
        } else if (encoderMode == EncoderControl.MIXER) {
            viewControl.navigateMixer(dir);
        }
    }
    
    private void setUpContextButtons(final ViewControl viewControl, final KeylabHardwareElements hwElements) {
        final RgbButton contextButton1 = hwElements.getContextButton(0);
        contextButton1.bindDelayHold(mainLayer, () -> setControlMode(ControlMode.DEVICE),
            () -> encoderMode = EncoderControl.TRACKS, this::toEncoderDeviceControl, 750);
        final RgbButton contextButton2 = hwElements.getContextButton(1);
        contextButton2.bindDelayHold(mainLayer, () -> setControlMode(ControlMode.MIXER),
            () -> encoderMode = EncoderControl.TRACKS, this::toEncoderTrackControl, 750);
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        
        final SceneBank sceneBank = viewControl.getSceneBank();
        
        final RgbButton contextButton3 = hwElements.getContextButton(2);
        final RgbButton contextButton4 = hwElements.getContextButton(3);
        contextButton3.bindRepeatHold(mainLayer, () -> sceneBank.scrollBackwards(), 400, 100);
        contextButton4.bindRepeatHold(mainLayer, () -> sceneBank.scrollForwards(), 400, 100);
        
        sceneBank.canScrollForwards().addValueObserver(hasNext -> {
            this.hasNextScene = hasNext;
            updateNextButton();
        });
        sceneBank.canScrollBackwards().addValueObserver(hasPrevious -> {
            this.hasPreviousScene = hasPrevious;
            updatePreviousButton();
        });
        
        final RgbButton contextButton5 = hwElements.getContextButton(4);
        contextButton5.bindPressed(mainLayer, () -> cursorTrack.arm().toggle());
        final RgbButton contextButton6 = hwElements.getContextButton(5);
        contextButton6.bindPressed(mainLayer, () -> cursorTrack.solo().toggle());
        final RgbButton contextButton7 = hwElements.getContextButton(6);
        contextButton7.bindPressed(mainLayer, () -> cursorTrack.mute().toggle());
        final RgbButton contextButton8 = hwElements.getContextButton(7);
        contextButton8.bindIsPressed(mainLayer, pressed -> {
            if (pressed) {
                viewControl.launchScene();
            }
            sceneButtonPressed = pressed;
        });
    }
    
    private void toEncoderTrackControl() {
        encoderMode = EncoderControl.MIXER;
        showTrackAssignment(true);
    }
    
    private void toEncoderDeviceControl() {
        encoderMode = EncoderControl.DEVICE;
        showDeviceName(true);
    }
    
    private void setControlMode(final ControlMode mixer) {
        controlMode = mixer;
        updateControlMode();
        applyControlMode();
    }
    
    private void bindControls(final KeylabHardwareElements hwElements) {
        final TrackBank trackBank = viewControl.getMixerTrackBank();
        for (int i = 0; i < 8; i++) {
            final TouchSlider slider = hwElements.getTouchSlider(i);
            final Track track = trackBank.getItemAt(i);
            slider.bindParameter(mixerLayer, track.volume(), track.name());
            final TouchEncoder encoder = hwElements.getEncoder(i);
            encoder.bindParameter(mixerLayer, track.pan(), track.name());
        }
        final CursorRemoteControlsPage remotes1 = viewControl.getPrimaryRemotes();
        final CursorRemoteControlsPage remotes2 = viewControl.getSliderRemotes();
        for (int i = 0; i < 8; i++) {
            final TouchSlider slider = hwElements.getTouchSlider(i);
            final TouchEncoder encoder = hwElements.getEncoder(i);
            encoder.bindParameter(deviceLayer, remotes1.getParameter(i), remotes1.getParameter(i).name());
            slider.bindParameter(deviceLayer, remotes2.getParameter(i), remotes2.getParameter(i).name());
        }
    }
    
    @Activate
    public void init() {
        this.mainLayer.activate();
        applyControlMode();
        updateControlMode();
    }
    
    public void applyControlMode() {
        if (controlMode == ControlMode.DEVICE) {
            mixerLayer.setIsActive(false);
            deviceLayer.setIsActive(true);
        } else {
            deviceLayer.setIsActive(false);
            mixerLayer.setIsActive(true);
        }
    }
    
    private void showTrackAssignment(final boolean initial) {
        if (encoderMode != EncoderControl.MIXER) {
        }
        midiProcessor.screenLine3(ScreenTarget.POP_SCREEN_3_LINES, "Mixer Control", RgbColor.WHITE,
            "Track %d - %d".formatted(mixerOffset + 1, mixerOffset + 8), RgbColor.WIDGET, "Main encoder to offset",
            RgbColor.WHITE, null);
    }
    
    private void showDeviceName(final boolean initial) {
        if (encoderMode != EncoderControl.DEVICE) {
            return;
        }
        if (deviceExists) {
            if (initial) {
                midiProcessor.screenLine3(ScreenTarget.POP_SCREEN_3_LINES, "Device Control", RgbColor.WHITE, deviceName,
                    RgbColor.WIDGET, "Main encoder to control devices", RgbColor.WHITE, null);
            } else {
                midiProcessor.screenLine2(ScreenTarget.POP_SCREEN_2_LINE_LEFT_ICON, "Device Control", RgbColor.WHITE,
                    deviceName, RgbColor.WIDGET, CenterIcons.MIXER);
            }
        } else {
            if (initial) {
                midiProcessor.screenLine2(ScreenTarget.POP_SCREEN_2_LINES, "Device Control", RgbColor.WHITE,
                    "No Device", RgbColor.WIDGET, null);
            } else {
                midiProcessor.screenLine1(
                    ScreenTarget.POP_SCREEN_2_LINES, "No Device", RgbColor.WIDGET, CenterIcons.MIXER);
            }
        }
    }
    
    private void updateMainScreen() {
        midiProcessor.screenLine2(ScreenTarget.SCREEN_2_LINES_INVERTED, trackSelectionState.trackName,
            trackSelectionState.trackColor, trackSelectionState.sceneName, RgbColor.WHITE, null);
    }
    
    private void updateArmState() {
        midiProcessor.screenContextButton(1, //
            getToggleIcon(armState), SmallIcon.BUTTON_ARMED_AUDIO_DEFAULT, armState ? RgbColor.RED : RgbColor.WHITE,
            "");
    }
    
    private void updateSoloState() {
        midiProcessor.screenContextButton(2,  //
            getToggleIcon(soloState), SmallIcon.SOLO, soloState ? RgbColor.YELLOW : RgbColor.WHITE, "S");
    }
    
    private void updateMuteState() {
        midiProcessor.screenContextButton(3, getToggleIcon(muteState), SmallIcon.MUTE,
            muteState ? RgbColor.ORANGE : RgbColor.WHITE, "M");
    }
    
    private ButtonDisplayType getToggleIcon(final boolean active) {
        return active ? ButtonDisplayType.TOGGLE_ICON : ButtonDisplayType.SIMPLE_ICON;
    }
    
    private void updateControlMode() {
        midiProcessor.screenContextButton(5, getButtonTab(controlMode == ControlMode.DEVICE),//
            SmallIcon.BLUE_HAND, RgbColor.WHITE, "DEVICE");
        midiProcessor.screenContextButton(
            6, getButtonTab(controlMode == ControlMode.MIXER), null, RgbColor.WHITE, "MIXER");
    }
    
    private ButtonDisplayType getButtonTab(final boolean active) {
        return active ? ButtonDisplayType.TAB_UNFOLDED : ButtonDisplayType.TAB_FOLDED;
    }
    
    private void updatePreviousButton() {
        final SmallIcon icon =
            panelLayout == LayoutType.ARRANGER ? SmallIcon.BROWSING_ARROW_LEFT : SmallIcon.BROWSING_ARROW_DEFAULT;
        
        midiProcessor.screenContextButton(7, ButtonDisplayType.SIMPLE_ICON, icon,
            hasPreviousScene ? RgbColor.WHITE : RgbColor.GRAY, "");
    }
    
    private void updateNextButton() {
        final SmallIcon icon =
            (panelLayout == LayoutType.ARRANGER ? SmallIcon.BROWSING_ARROW_RIGHT : SmallIcon.BROWSING_ARROW_DOWN);
        midiProcessor.screenContextButton(8, ButtonDisplayType.SIMPLE_ICON, icon,
            hasNextScene ? RgbColor.WHITE : RgbColor.GRAY, "");
    }
    
}
