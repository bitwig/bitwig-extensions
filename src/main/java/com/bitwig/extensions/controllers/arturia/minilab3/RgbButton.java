package com.bitwig.extensions.controllers.arturia.minilab3;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public class RgbButton {
    private final byte[] rgbCommand = {(byte) 0xF0, 0x00, 0x20, 0x6B, 0x7F, 0x42, 0x02, //
            0x02, // 7 - Patch Id
            0x16, // 8 - Command
            0x02, // 9 - Pad ID
            0x00, // 10 - Red
            0x00, // 11 - Green
            0x00, // 12 - blue
            (byte) 0xF7};

    private final HardwareButton hwButton;
    private final MultiStateHardwareLight light;
    private final int padId;
    private final PadBank bankId;
    private final SysExHandler sysExHandler;
    private final int noteValue;

    public enum Type {
        NOTE,
        CC
    }

    public RgbButton(final int padId, final PadBank bankId, final Type type, final int value, final int channel,
                     final boolean shiftButtonType, final MiniLab3Extension driver) {
        this.padId = padId;
        this.bankId = bankId;
        noteValue = value;
       ControllerHost host = driver.getHost();
       BooleanValueObject shiftDown = driver.getShiftDown();
        final MidiIn midiIn = driver.getMidiIn();
        sysExHandler = driver.getSysExHandler();
        rgbCommand[7] = (byte) 0x2; // DAW Preset
        rgbCommand[9] = (byte) padId;
        hwButton = driver.getSurface().createHardwareButton("RGB_PAD_" + value);
        if (type == Type.NOTE) {
            hwButton.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(channel, value));
            hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(channel, value));
        } else {
            hwButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, value, 127));
            hwButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, value, 0));
        }
        hwButton.isPressed().markInterested();
        light = driver.getSurface().createMultiStateHardwareLight("RGB_PAD_LIGHT_" + value);
        hwButton.setBackgroundLight(light);
        if (bankId.getIndex() == -1) { // Individual updates handled
            light.state().onUpdateHardware(this::updateState);
        }
    }

    public Integer getNoteValue() {
        return noteValue;
    }


    public MultiStateHardwareLight getLight() {
        return light;
    }

    private void updateState(final InternalHardwareLightState state) {
        if (state instanceof RgbLightState) {
            ((RgbLightState) state).apply(rgbCommand);
            sysExHandler.sendColor((byte) padId, bankId, rgbCommand[10], rgbCommand[11], rgbCommand[12]);
        } else {
            setRgbOff();
        }
    }

    private void setRgbOff() {
        rgbCommand[10] = 0;
        rgbCommand[11] = 0;
        rgbCommand[12] = 0;
    }

    public void bindPressed(final Layer layer, final Consumer<Boolean> target,
                            final Supplier<RgbLightState> lightSource) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
        layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
        layer.bindLightState(lightSource::get, light);
    }

    public void bind(final Layer layer, final Runnable action, final Supplier<RgbLightState> lightSource) {
        layer.bind(hwButton, hwButton.pressedAction(), action);
        layer.bind(hwButton, hwButton.releasedAction(), () -> {
        });
        layer.bindLightState(lightSource::get, light);
    }

    public void bind(final Layer layer, final Runnable action, final RgbLightState pressOn,
                     final RgbLightState pressOff) {
        layer.bind(hwButton, hwButton.pressedAction(), action);
        layer.bind(hwButton, hwButton.releasedAction(), () -> {
        });
        layer.bindLightState(() -> hwButton.isPressed().get() ? pressOn : pressOff, light);
    }

    public void bindToggle(final Layer layer, final SettableBooleanValue value, final RgbLightState onColor,
                           final RgbLightState offColor) {
        value.markInterested();
        layer.bind(hwButton, hwButton.pressedAction(), value::toggle);
        layer.bindLightState(() -> value.get() ? onColor : offColor, light);
    }

}
