package com.bitwig.extensions.controllers.novation.slmk3.control;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.controllers.novation.slmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;
import com.bitwig.extensions.controllers.novation.slmk3.value.ObservableColor;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.time.TimeRepeatEvent;
import com.bitwig.extensions.framework.time.TimedDelayEvent;
import com.bitwig.extensions.framework.time.TimedEvent;

public class RgbButton {
    
    protected MultiStateHardwareLight light;
    protected HardwareButton hwButton;
    protected MidiProcessor midiProcessor;
    private TimedEvent currentTimer;
    private long recordedDownTime;
    private boolean momentaryJustTurnedOn;
    protected final int midiId;
    private final int ledIndex;
    
    public enum Type {
        NOTE,
        CC
    }
    
    public RgbButton(final Type type, final int midiId, final int ledIndex, final String name,
        final HardwareSurface surface, final MidiProcessor midiProcessor) {
        this.midiProcessor = midiProcessor;
        this.ledIndex = ledIndex;
        this.midiId = midiId;
        hwButton = surface.createHardwareButton(name + "_" + midiId);
        if (type == Type.NOTE) {
            midiProcessor.setNoteMatcher(hwButton, midiId);
        } else {
            midiProcessor.setCcMatcher(hwButton, midiId);
        }
        light = surface.createMultiStateHardwareLight(name + "_LIGHT_" + midiId);
        light.state().setValue(SlRgbState.OFF);
        hwButton.setBackgroundLight(light);
        hwButton.isPressed().markInterested();
        light.state().onUpdateHardware(this::updateState);
    }
    
    public int getMidiId() {
        return midiId;
    }
    
    private void updateState(final InternalHardwareLightState internalHardwareLightState) {
        if (internalHardwareLightState instanceof SlRgbState rgbState) {
            midiProcessor.updateRgbState(this.ledIndex, rgbState);
        }
    }
    
    public void bindTimedPressRelease(final Layer layer, final Runnable pressAction, final IntConsumer timedRelease) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> this.handleTimedPress(pressAction));
        layer.bind(hwButton, hwButton.releasedAction(), () -> this.handleTimedRelease(timedRelease));
    }
    
    private void handleTimedPress(final Runnable action) {
        recordedDownTime = System.currentTimeMillis();
        action.run();
    }
    
    private void handleTimedRelease(final IntConsumer timedRelease) {
        timedRelease.accept((int) (System.currentTimeMillis() - recordedDownTime));
    }
    
    public void bindHoldToggle(final Layer layer, final SettableBooleanValue value, final int momentaryThreshold) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> this.handleTogglePressed(value));
        layer.bind(hwButton, hwButton.releasedAction(), () -> this.handleToggleReleased(value, momentaryThreshold));
    }
    
    private void handleTogglePressed(final SettableBooleanValue value) {
        recordedDownTime = System.currentTimeMillis();
        if (!value.get()) {
            value.set(true);
            momentaryJustTurnedOn = true;
        } else {
            momentaryJustTurnedOn = false;
        }
    }
    
    private void handleToggleReleased(final SettableBooleanValue value, final int momentaryThreshold) {
        final long diff = System.currentTimeMillis() - recordedDownTime;
        if (diff > momentaryThreshold) {
            value.set(false);
        } else if (!momentaryJustTurnedOn) {
            value.set(false);
            momentaryJustTurnedOn = false;
        }
    }
    
    
    public void bindIsPressed(final Layer layer, final Consumer<Boolean> target) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
        layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
    }
    
    public void bindIsPressed(final Layer layer, final SettableBooleanValue value) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> value.set(true));
        layer.bind(hwButton, hwButton.releasedAction(), () -> value.set(false));
    }
    
    public void bind(final Layer layer, final SettableBooleanValue value) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> value.set(true));
        layer.bind(hwButton, hwButton.releasedAction(), () -> value.set(false));
    }
    
    public void bind(final Layer layer, final HardwareActionBindable action) {
        layer.bind(hwButton, hwButton.pressedAction(), action::invoke);
    }
    
    public void bindDisabled(final Layer layer) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> {});
        layer.bind(hwButton, hwButton.releasedAction(), () -> {});
        layer.bindLightState(() -> SlRgbState.OFF, light);
    }
    
    public void bindPressed(final Layer layer, final Runnable action) {
        layer.bind(hwButton, hwButton.pressedAction(), action);
    }
    
    public void bindHoldDelay(final Layer layer, final Runnable initAction, final Runnable delayedAction,
        final int delayTime) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> {
            initAction.run();
            currentTimer = new TimedDelayEvent(delayedAction, delayTime);
            midiProcessor.queueTimedEvent(currentTimer);
        });
        layer.bind(hwButton, hwButton.releasedAction(), () -> cancelEvent());
    }
    
    public void bindReleased(final Layer layer, final Runnable action) {
        layer.bind(hwButton, hwButton.releasedAction(), action);
    }
    
    public void bindLightOnPressed(final Layer layer, final SlRgbState onColor, final SlRgbState offColor) {
        layer.bindLightState(() -> hwButton.isPressed().get() ? onColor : offColor, light);
    }
    
    public void bindLightOnPressed(final Layer layer, final Function<Boolean, SlRgbState> colorProvider) {
        layer.bindLightState(() -> colorProvider.apply(hwButton.isPressed().get()), light);
    }
    
    public void bindLightOnPressed(final Layer layer, final ObservableColor colorSource) {
        layer.bindLightState(
            () -> hwButton.isPressed().get() ? colorSource.get() : colorSource.getDimmedColor(), light);
    }
    
    public void bindLightOnPressed(final Layer layer, final SlRgbState color) {
        final SlRgbState onColor = color;
        final SlRgbState offColor = color.reduced(25);
        layer.bindLightState(() -> hwButton.isPressed().get() ? onColor : offColor, light);
    }
    
    public void bindLightOnPressed(final Layer layer, final SlRgbState color, final BooleanValue value) {
        final SlRgbState onColor = color;
        final SlRgbState offColor = color.reduced(25);
        final SlRgbState activeColor = color.reduced(80);
        value.markInterested();
        layer.bindLightState(() -> {
            if (hwButton.isPressed().get()) {
                return onColor;
            }
            return value.get() ? activeColor : offColor;
        }, light);
    }
    
    public void bindLightOnPressed(final Layer layer, final SlRgbState color, final BooleanSupplier value) {
        final SlRgbState onColor = color;
        final SlRgbState offColor = color.reduced(25);
        final SlRgbState activeColor = color.reduced(80);
        layer.bindLightState(() -> {
            if (hwButton.isPressed().get()) {
                return onColor;
            }
            return value.getAsBoolean() ? activeColor : offColor;
        }, light);
    }
    
    public void bindLight(final Layer layer, final Supplier<InternalHardwareLightState> supplier) {
        layer.bindLightState(supplier, light);
    }
    
    public void bindRepeatHold(final Layer layer, final Runnable pressedAction, final int repeatDelay,
        final int repeatFrequency) {
        layer.bind(
            hwButton, hwButton.pressedAction(), () -> initiateRepeat(pressedAction, repeatDelay, repeatFrequency));
        layer.bind(hwButton, hwButton.releasedAction(), this::cancelEvent);
    }
    
    private void initiateRepeat(final Runnable action, final int repeatDelay, final int repeatFrequency) {
        action.run();
        recordedDownTime = System.currentTimeMillis();
        currentTimer = new TimeRepeatEvent(action, repeatDelay, repeatFrequency);
        midiProcessor.queueTimedEvent(currentTimer);
    }
    
    private void cancelEvent() {
        if (currentTimer != null) {
            currentTimer.cancel();
            currentTimer = null;
        }
    }
    
    public void refresh() {
        light.state().setValue(null);
    }
}
