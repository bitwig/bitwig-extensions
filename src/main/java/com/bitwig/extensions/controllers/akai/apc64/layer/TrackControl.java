package com.bitwig.extensions.controllers.akai.apc64.layer;

import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.akai.apc.common.control.RgbButton;
import com.bitwig.extensions.controllers.akai.apc.common.led.LedBehavior;
import com.bitwig.extensions.controllers.akai.apc.common.led.RgbLightState;
import com.bitwig.extensions.controllers.akai.apc.common.led.VarSingleLedState;
import com.bitwig.extensions.controllers.akai.apc64.Apc64CcAssignments;
import com.bitwig.extensions.controllers.akai.apc64.HardwareElements;
import com.bitwig.extensions.controllers.akai.apc64.ModifierStates;
import com.bitwig.extensions.controllers.akai.apc64.ViewControl;
import com.bitwig.extensions.controllers.akai.apc64.control.SingleLedButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
public class TrackControl extends Layer {

    private static final RgbLightState STOP_PLAY_COLOR = RgbLightState.of(21);
    private static final RgbLightState STOP_COLOR = RgbLightState.of(21, LedBehavior.LIGHT_10);
    private static final RgbLightState STOP_QUEDED_COLOR = RgbLightState.of(21, LedBehavior.BLINK_8);

    private final ModifierStates modifierStates;
    private Mode mode = Mode.MUTE;
    private final Map<Mode, Layer> layerMap = new HashMap<>();
    private int soloHeld = 0;

    @Inject
    private PadLayer padLayer;

    private enum Mode {
        ARM,
        MUTE,
        SOLO,
        STOP
    }

    public TrackControl(Layers layers, HardwareElements hwElements, ModifierStates modifierStates,
                        ViewControl viewControl) {
        super(layers, "TRACK_CONTROL");
        this.modifierStates = modifierStates;
        Arrays.stream(Mode.values()).forEach(mode -> layerMap.put(mode, new Layer(layers, "TRACK_CONTROL_" + mode)));
        bindModeButton(hwElements, Apc64CcAssignments.MODE_REC, Mode.ARM);
        SingleLedButton muteButton = bindModeButton(hwElements, Apc64CcAssignments.MODE_MUTE, Mode.MUTE);
        muteButton.bindIsPressed(this, pressed -> padLayer.activateMute(pressed));
        SingleLedButton soloButton = bindModeButton(hwElements, Apc64CcAssignments.MODE_SOLO, Mode.SOLO);
        soloButton.bindIsPressed(this, pressed -> padLayer.activateSolo(pressed));
        bindModeButton(hwElements, Apc64CcAssignments.MODE_STOP, Mode.STOP);
        bindArm(hwElements, viewControl);
        bindMutes(hwElements, viewControl);
        bindSolo(hwElements, viewControl);
        bindStop(hwElements, viewControl);
    }

    @Activate
    public void contextActivate() {
        setIsActive(true);
        layerMap.get(mode).setIsActive(true);
    }

    public SingleLedButton bindModeButton(HardwareElements hwElements, Apc64CcAssignments assignment, Mode mode) {
        SingleLedButton button = hwElements.getButton(assignment);
        button.bindPressed(this, () -> changeMode(mode));
        button.bindLight(this, () -> this.mode == mode ? VarSingleLedState.FULL : VarSingleLedState.LIGHT_10);
        return button;
    }


    public void bindMutes(HardwareElements hwElements, ViewControl viewControl) {
        Layer layer = layerMap.get(Mode.MUTE);
        for (int i = 0; i < 8; i++) {
            RgbButton button = hwElements.getTrackControlButtons(i);
            Track track = viewControl.getTrackBank().getItemAt(i);
            track.mute().markInterested();
            button.bindLight(layer, () -> muteState(track));
            button.bindPressed(layer, () -> track.mute().toggle());
        }
    }

    public void bindSolo(HardwareElements hwElements, ViewControl viewControl) {
        Layer layer = layerMap.get(Mode.SOLO);
        for (int i = 0; i < 8; i++) {
            RgbButton button = hwElements.getTrackControlButtons(i);
            Track track = viewControl.getTrackBank().getItemAt(i);
            track.mute().markInterested();
            button.bindLight(layer, () -> soloState(track));
            button.bindIsPressed(layer, pressed -> handleSolo(track, pressed));
        }
    }

    private void handleSolo(Track track, boolean pressed) {
        if (pressed) {
            soloHeld++;
            track.solo().toggle(!modifierStates.getShiftActive().get() && soloHeld == 1);
        } else {
            if (soloHeld > 0) {
                soloHeld--;
            }
        }
    }

    public void bindArm(HardwareElements hwElements, ViewControl viewControl) {
        Layer layer = layerMap.get(Mode.ARM);
        for (int i = 0; i < 8; i++) {
            RgbButton button = hwElements.getTrackControlButtons(i);
            Track track = viewControl.getTrackBank().getItemAt(i);
            track.arm().markInterested();
            button.bindLight(layer, () -> armState(track));
            button.bindPressed(layer, () -> track.arm().toggle());
        }
    }

    public void bindStop(HardwareElements hwElements, ViewControl viewControl) {
        Layer layer = layerMap.get(Mode.STOP);
        for (int i = 0; i < 8; i++) {
            RgbButton button = hwElements.getTrackControlButtons(i);
            Track track = viewControl.getTrackBank().getItemAt(i);
            track.mute().markInterested();
            track.isStopped().markInterested();
            track.isQueuedForStop().markInterested();
            button.bindLight(layer, () -> stopState(track));
            button.bindPressed(layer, () -> track.stop());
        }
    }

    public RgbLightState muteState(Track track) {
        if (track.exists().get()) {
            return track.mute().get() ? RgbLightState.ORANGE_FULL : RgbLightState.ORANGE_DIM;
        }
        return RgbLightState.OFF;
    }

    public RgbLightState armState(Track track) {
        if (track.exists().get()) {
            return track.arm().get() ? RgbLightState.RED_FULL : RgbLightState.RED_DIM;
        }
        return RgbLightState.OFF;
    }

    public RgbLightState soloState(Track track) {
        if (track.exists().get()) {
            return track.solo().get() ? RgbLightState.YELLOW_FULL : RgbLightState.YELLOW_DIM;
        }
        return RgbLightState.OFF;
    }

    public RgbLightState stopState(Track track) {
        if (track.exists().get()) {
            if (track.isQueuedForStop().get()) {
                return STOP_QUEDED_COLOR;
            }
            if (track.isStopped().get()) {
                return STOP_COLOR;
            }
            return STOP_PLAY_COLOR;
        }
        return RgbLightState.OFF;
    }


    public void changeMode(Mode mode) {
        if (this.mode == mode) {
            return;
        }
        layerMap.get(this.mode).setIsActive(false);
        this.mode = mode;
        layerMap.get(this.mode).setIsActive(true);
    }
}
