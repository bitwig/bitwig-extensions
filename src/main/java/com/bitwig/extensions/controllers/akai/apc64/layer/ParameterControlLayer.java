package com.bitwig.extensions.controllers.akai.apc64.layer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.akai.apc.common.led.ColorLookup;
import com.bitwig.extensions.controllers.akai.apc.common.led.RgbLightState;
import com.bitwig.extensions.controllers.akai.apc.common.led.VarSingleLedState;
import com.bitwig.extensions.controllers.akai.apc64.Apc64CcAssignments;
import com.bitwig.extensions.controllers.akai.apc64.DeviceControl;
import com.bitwig.extensions.controllers.akai.apc64.HardwareElements;
import com.bitwig.extensions.controllers.akai.apc64.ViewControl;
import com.bitwig.extensions.controllers.akai.apc64.control.FaderLightState;
import com.bitwig.extensions.controllers.akai.apc64.control.SingleLedButton;
import com.bitwig.extensions.controllers.akai.apc64.control.TouchSlider;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class ParameterControlLayer extends Layer {


    private enum Mode {
        DEVICE(Apc64CcAssignments.STRIP_DEVICE),
        VOLUME(Apc64CcAssignments.STRIP_VOLUME),
        PAN(Apc64CcAssignments.STRIP_PAN),
        SENDS(Apc64CcAssignments.STRIP_SENDS),
        CHANNEL_STRIP(Apc64CcAssignments.STRIP_CHANNEL),
        OFF(Apc64CcAssignments.STRIP_OFF);
        private final Apc64CcAssignments assignment;

        Mode(Apc64CcAssignments assignment) {
            this.assignment = assignment;
        }

        public Apc64CcAssignments getAssignment() {
            return assignment;
        }
    }

    private Mode currentMode = Mode.VOLUME;
    private DeviceControl.Focus currentDeviceFocus = DeviceControl.Focus.DEVICE;
    private final Map<Mode, Layer> modes = new HashMap<>();
    private final Map<DeviceControl.Focus, Layer> deviceModes = new HashMap<>();

    @Inject
    private NavigationLayer navigationSection;

    private final ViewControl viewControl;
    private int cursorTrackColor = 0;
    private final MainDisplay display;

    public ParameterControlLayer(final Layers layers, HardwareElements hwElements, ViewControl viewControl,
                                 MainDisplay mainDisplay) {
        super(layers, "PARAMETER CONTROL");
        this.viewControl = viewControl;
        this.display = mainDisplay;
        Arrays.stream(Mode.values()).forEach(mode -> modes.put(mode, new Layer(layers, "STRIP_" + mode.toString())));
        Arrays.stream(DeviceControl.Focus.values())
                .forEach(mode -> deviceModes.put(mode, new Layer(layers, "DEVICE_" + mode.toString())));
        deviceModes.put(DeviceControl.Focus.DEVICE, modes.get(Mode.DEVICE));
        bindModeToButton(hwElements, Mode.DEVICE);
        bindModeToButton(hwElements, Mode.VOLUME);
        bindModeToButton(hwElements, Mode.PAN);
        bindModeToButton(hwElements, Mode.SENDS);
        bindModeToButton(hwElements, Mode.CHANNEL_STRIP);
        bindModeToButton(hwElements, Mode.OFF);
        TouchSlider[] touchSliders = hwElements.getTouchSliders();
        bindVolumeLayer(touchSliders, viewControl.getTrackBank());
        bindPanLayer(touchSliders, viewControl.getTrackBank());
        bindSendsLayer(touchSliders, viewControl.getTrackBank());
        bindCursorLayer(touchSliders, viewControl.getCursorTrack());
        bindDeviceLayer(hwElements, viewControl.getDeviceControl());
        bindOffLayer(touchSliders);
    }

    private void bindDeviceLayer(HardwareElements hwElements, DeviceControl deviceControl) {
        TouchSlider[] sliders = hwElements.getTouchSliders();

        bindToPage(sliders, deviceModes.get(DeviceControl.Focus.DEVICE),
                deviceControl.getPage(DeviceControl.Focus.DEVICE), this::faderTrackColorProvider);
        bindToPage(sliders, deviceModes.get(DeviceControl.Focus.TRACK),
                deviceControl.getPage(DeviceControl.Focus.TRACK), this::faderTrackColorProvider);
        bindToPage(sliders, deviceModes.get(DeviceControl.Focus.PROJECT),
                deviceControl.getPage(DeviceControl.Focus.PROJECT), this::faderProjectColorProvider);
        deviceControl.setFocusListener(focus -> changeDeviceFocus(focus));
    }

    private void bindToPage(TouchSlider[] sliders, Layer layer, CursorRemoteControlsPage remotePage,
                            Function<Parameter, RgbLightState> colorProvider) {
        for (int i = 0; i < sliders.length; i++) {
            TouchSlider slider = sliders[i];
            RemoteControl parameter = remotePage.getParameter(i);
            parameter.exists().markInterested();
            parameter.name().markInterested();
            bindSlider(layer, slider, parameter, colorProvider);
        }
    }

    private void bindSlider(Layer layer, TouchSlider slider, RemoteControl parameter,
                            Function<Parameter, RgbLightState> colorProvider) {
        slider.bindParameter(layer, display, parameter.name(), parameter);
        slider.bindIsPressed(layer, pressed -> parameter.touch(pressed));
        slider.bindLightColor(layer, () -> colorProvider.apply(parameter));
        slider.bindLightState(layer, () -> !parameter.exists().get() ? FaderLightState.OFF : FaderLightState.V_WHITE);
    }

    private RgbLightState faderTrackColorProvider(Parameter parameter) {
        if (parameter.exists().get()) {
            return RgbLightState.of(cursorTrackColor);
        }
        return RgbLightState.OFF;
    }

    private RgbLightState faderProjectColorProvider(Parameter parameter) {
        if (parameter.exists().get()) {
            return RgbLightState.WHITE;
        }
        return RgbLightState.OFF;
    }

    private void bindModeToButton(HardwareElements hwElements, Mode mode) {
        SingleLedButton button = hwElements.getButton(mode.getAssignment());
        button.bindLightPressed(this,
                pressed -> pressed ? VarSingleLedState.FULL : currentMode == mode ? VarSingleLedState.LIGHT_75 : VarSingleLedState.LIGHT_10);
        button.bindIsPressed(this, pressed -> this.handleModeChange(pressed, mode));
    }

    private void bindVolumeLayer(TouchSlider[] sliders, TrackBank trackBank) {
        Layer layer = modes.get(Mode.VOLUME);

        for (int i = 0; i < sliders.length; i++) {
            int index = i;
            TouchSlider slider = sliders[i];
            Track track = trackBank.getItemAt(i);
            slider.bindParameter(layer, display, track.name(), track.volume());
            slider.bindIsPressed(layer, pressed -> track.volume().touch(pressed));
            slider.bindLightColor(layer, () -> !track.exists().get() ? RgbLightState.OFF : RgbLightState.of(
                    viewControl.getTrackColor(index)));
            slider.bindLightState(layer, () -> getVolumeState(track, slider));
        }
    }

    private FaderLightState getVolumeState(Track track, TouchSlider slider) {
        if (track.exists().get()) {
            return slider.isAutomated() ? FaderLightState.V_RED : FaderLightState.V_WHITE;
        }
        return FaderLightState.OFF;
    }

    private void bindPanLayer(TouchSlider[] sliders, TrackBank trackBank) {
        Layer layer = modes.get(Mode.PAN);

        for (int i = 0; i < sliders.length; i++) {
            int index = i;
            TouchSlider slider = sliders[i];
            Track track = trackBank.getItemAt(i);
            slider.bindParameter(layer, display, track.name(), track.pan());
            slider.bindIsPressed(layer, pressed -> track.pan().touch(pressed));
            slider.bindLightColor(layer, () -> !track.exists().get() ? RgbLightState.OFF : RgbLightState.of(
                    viewControl.getTrackColor(index)));
            slider.bindLightState(layer,
                    () -> !track.exists().get() ? FaderLightState.OFF : FaderLightState.BIPOLOAR_WHITE);
        }
    }

    private void bindSendsLayer(TouchSlider[] sliders, TrackBank trackBank) {
        Layer layer = modes.get(Mode.SENDS);

        for (int i = 0; i < sliders.length; i++) {
            int index = i;
            TouchSlider slider = sliders[i];
            Track track = trackBank.getItemAt(i);
            Send send = track.sendBank().getItemAt(0);
            send.exists().markInterested();
            slider.bindParameter(layer, display, track.name(), send);
            slider.bindIsPressed(layer, pressed -> send.touch(pressed));
            slider.bindLightColor(layer, () -> getSendColor(track, send, index));
            slider.bindLightState(layer, () -> getSendState(track, send));
        }
    }

    private void bindCursorLayer(TouchSlider[] sliders, CursorTrack cursorTrack) {
        Layer layer = modes.get(Mode.CHANNEL_STRIP);
        cursorTrack.color().addValueObserver((r, g, b) -> cursorTrackColor = ColorLookup.toColor(r, g, b));
        sliders[0].bindParameter(layer, display, cursorTrack.name(), cursorTrack.volume());
        sliders[0].bindIsPressed(layer, pressed -> cursorTrack.volume().touch(pressed));
        sliders[0].bindLightColor(layer,
                () -> !cursorTrack.exists().get() ? RgbLightState.OFF : RgbLightState.of(cursorTrackColor));
        sliders[0].bindLightState(layer,
                () -> !cursorTrack.exists().get() ? FaderLightState.OFF : FaderLightState.V_WHITE);

        sliders[1].bindParameter(layer, display, cursorTrack.name(), cursorTrack.pan());
        sliders[1].bindIsPressed(layer, pressed -> cursorTrack.pan().touch(pressed));
        sliders[1].bindLightColor(layer,
                () -> !cursorTrack.exists().get() ? RgbLightState.OFF : RgbLightState.of(cursorTrackColor));
        sliders[1].bindLightState(layer,
                () -> !cursorTrack.exists().get() ? FaderLightState.OFF : FaderLightState.BIPOLOAR_WHITE);

        for (int i = 0; i < 6; i++) {
            int index = i;
            TouchSlider slider = sliders[i + 2];
            Send send = cursorTrack.sendBank().getItemAt(i);
            send.exists().markInterested();
            slider.bindParameter(layer, display, cursorTrack.name(), send);
            slider.bindIsPressed(layer, pressed -> send.touch(pressed));
            slider.bindLightColor(layer, () -> getSendColor(cursorTrack, send, index));
            slider.bindLightState(layer, () -> getSendState(cursorTrack, send));
        }
    }

    private RgbLightState getSendColor(Track track, Send send, int index) {
        if (track.exists().get() && send.exists().get()) {
            return RgbLightState.of(cursorTrackColor);
        }
        return RgbLightState.OFF;
    }

    private FaderLightState getSendState(Track track, Send send) {
        if (track.exists().get() && send.exists().get()) {
            return FaderLightState.V_WHITE;
        }
        return FaderLightState.OFF;
    }

    private void bindOffLayer(TouchSlider[] sliders) {
        Layer layer = modes.get(Mode.OFF);

        for (int i = 0; i < sliders.length; i++) {
            TouchSlider slider = sliders[i];
            slider.bindIsPressed(layer, pressed -> {
            });
            slider.bindLightColor(layer, () -> RgbLightState.OFF);
            slider.bindLightState(layer, () -> FaderLightState.OFF);
        }
    }

    private void handleModeChange(boolean pressed, Mode mode) {
        if (pressed) {
            if (currentMode != mode) {
                getLayerFromMode(currentMode).setIsActive(false);
                currentMode = mode;
                getLayerFromMode(currentMode).setIsActive(true);
            } else if (currentMode == Mode.SENDS) {
                navigationSection.navigateSends();
            }
        }
        if (currentMode == Mode.DEVICE) {
            navigationSection.setDeviceNavigationActive(pressed);
        } else if (currentMode == Mode.SENDS) {
            navigationSection.setSendsNavigationActive(pressed);
        }
    }

    private void changeDeviceFocus(DeviceControl.Focus newFocus) {
        if (newFocus == currentDeviceFocus) {
            return;
        }
        if (currentMode == Mode.DEVICE) {
            getLayerFromMode(currentMode).setIsActive(false);
            currentDeviceFocus = newFocus;
            getLayerFromMode(currentMode).setIsActive(true);
        } else {
            currentDeviceFocus = newFocus;
        }
    }

    private Layer getLayerFromMode(Mode mode) {
        if (mode == Mode.DEVICE) {
            return deviceModes.get(currentDeviceFocus);
        }
        return modes.get(mode);
    }

    @Activate
    public void activateLayer() {
        this.activate();
        modes.get(currentMode).setIsActive(true);
    }
}
