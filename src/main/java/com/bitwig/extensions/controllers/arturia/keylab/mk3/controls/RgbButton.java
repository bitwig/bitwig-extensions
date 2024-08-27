package com.bitwig.extensions.controllers.arturia.keylab.mk3.controls;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.MidiProcessor;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.color.RgbLightState;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.time.TimeRepeatEvent;
import com.bitwig.extensions.framework.time.TimedDelayEvent;
import com.bitwig.extensions.framework.time.TimedEvent;

public abstract class RgbButton {
    protected final HardwareButton hwButton;
    protected final MultiStateHardwareLight light;
    protected final int midiValue;
    protected TimedEvent currentTimer;
    protected final MidiProcessor midiProcessor;
    protected final byte[] rgbCommand = {
        (byte) 0xF0, 0x00, 0x20, 0x6B, 0x7F, 0x42, 0x04, //
        0x02, // 7 - Patch Id
        0x03, // 8 - Content Type
        0x03, // 9 - LED ID
        0x00, // 10 - Red
        0x00, // 11 - Green
        0x00, // 12 - blue
        0x00, // state special processing
        (byte) 0xF7
    };
    
    public RgbButton(final String name, final int midiValue, final int lightId, final HardwareSurface surface,
        final MidiProcessor midiProcessor) {
        this.midiValue = midiValue;
        this.midiProcessor = midiProcessor;
        hwButton = surface.createHardwareButton("%s_BUTTON".formatted(name));
        hwButton.isPressed().markInterested();
        light = surface.createMultiStateHardwareLight("%s_LIGHT".formatted(name));
        hwButton.setBackgroundLight(light);
        rgbCommand[9] = (byte) lightId;
        light.setColorToStateFunction(RgbLightState::forColor);
        light.state().onUpdateHardware(this::updateState);
    }
    
    public void refreshLight() {
        light.state().setValue(null);
    }
    
    public void setLightState(final RgbLightState state) {
        light.state().setValue(state);
    }
    
    private void updateState(final InternalHardwareLightState state) {
        if (state instanceof final RgbLightState ligthState) {
            ligthState.apply(rgbCommand);
            midiProcessor.sendSysex(rgbCommand);
        } else {
            setRgbOff();
            midiProcessor.sendSysex(rgbCommand);
        }
    }
    
    private void setRgbOff() {
        rgbCommand[10] = 0;
        rgbCommand[11] = 0;
        rgbCommand[12] = 0;
    }
    
    public void bindIsPressed(final Layer layer, final Consumer<Boolean> target) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
        layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
    }
    
    public void bindPressed(final Layer layer, final Runnable action) {
        layer.bind(hwButton, hwButton.pressedAction(), action);
    }
    
    public void bindPressed(final Layer layer, final Action action) {
        layer.bind(hwButton, hwButton.pressedAction(), action);
    }
    
    public void bindReleased(final Layer layer, final Runnable action) {
        layer.bind(hwButton, hwButton.releasedAction(), action);
    }
    
    public void bindToggle(final Layer layer, final SettableBooleanValue value, final RgbLightState onColor,
        final RgbLightState offColor) {
        value.markInterested();
        layer.bind(hwButton, hwButton.pressedAction(), value::toggle);
        layer.bindLightState(() -> value.get() ? onColor : offColor, light);
    }
    
    public void bindLight(final Layer layer, final Supplier<RgbLightState> lightSource) {
        layer.bindLightState(lightSource::get, light);
    }
    
    public void bindLight(final Layer layer, final RgbLightState color, final RgbLightState holdColor) {
        hwButton.isPressed().markInterested();
        layer.bindLightState(() -> hwButton.isPressed().get() ? holdColor : color, light);
    }
    
    public void bindRepeatHold(final Layer layer, final Runnable pressedAction, final int repeatDelay,
        final int repeatFrequency) {
        layer.bind(
            hwButton, hwButton.pressedAction(), () -> initiateRepeat(pressedAction, repeatDelay, repeatFrequency));
        layer.bind(hwButton, hwButton.releasedAction(), this::cancelEvent);
    }
    
    public void bindDelayHold(final Layer layer, final Consumer<Boolean> holdAction, final int delayTime) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> {
            currentTimer = new TimedDelayEvent(() -> holdAction.accept(true), delayTime);
            midiProcessor.queueTimedEvent(currentTimer);
        });
        layer.bind(hwButton, hwButton.releasedAction(), () -> {
            holdAction.accept(false);
            cancelEvent();
        });
    }
    
    public void bindDelayHold(final Layer layer, final Runnable downAction, final Runnable releaseAction,
        final Runnable delayAction, final int delayTime) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> {
            downAction.run();
            currentTimer = new TimedDelayEvent(delayAction, delayTime);
            midiProcessor.queueTimedEvent(currentTimer);
        });
        layer.bind(hwButton, hwButton.releasedAction(), () -> {
            releaseAction.run();
            cancelEvent();
        });
        
    }
    
    public void bindRepeatHold(final Layer layer, final Runnable pressedAction, final Runnable repeatAction,
        final Runnable releaseAction, final int repeatDelay, final int repeatFrequency) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> {
            pressedAction.run();
            initiateRepeat(repeatAction, repeatDelay, repeatFrequency);
        });
        layer.bind(hwButton, hwButton.releasedAction(), () -> {
            cancelEvent();
            releaseAction.run();
        });
    }
    
    private void initiateRepeat(final Runnable action, final int repeatDelay, final int repeatFrequency) {
        action.run();
        currentTimer = new TimeRepeatEvent(action, repeatDelay, repeatFrequency);
        midiProcessor.queueTimedEvent(currentTimer);
    }
    
    private void cancelEvent() {
        if (currentTimer != null) {
            currentTimer.cancel();
            currentTimer = null;
        }
    }
    
    public void bind(final Layer layer, final Runnable action, final RgbLightState pressOn,
        final RgbLightState pressOff) {
        layer.bind(hwButton, hwButton.pressedAction(), action);
        layer.bindLightState(() -> hwButton.isPressed().get() ? pressOn : pressOff, light);
    }
    
    public void bind(final Layer layer, final HardwareActionBindable action, final RgbLightState pressOn,
        final RgbLightState pressOff) {
        layer.bind(hwButton, hwButton.pressedAction(), action);
        layer.bindLightState(() -> hwButton.isPressed().get() ? pressOn : pressOff, light);
    }
    
    
}
