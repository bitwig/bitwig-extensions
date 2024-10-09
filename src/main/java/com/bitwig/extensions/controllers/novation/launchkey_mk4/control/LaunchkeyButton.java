package com.bitwig.extensions.controllers.novation.launchkey_mk4.control;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.MidiProcessor;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.time.TimeRepeatEvent;
import com.bitwig.extensions.framework.time.TimedEvent;

public class LaunchkeyButton {
    protected HardwareButton hwButton;
    protected MidiProcessor midiProcessor;
    protected TimedEvent currentTimer;
    protected long recordedDownTime;
    protected boolean momentaryJustTurnedOn;
    protected final int midiId;
    protected final int channel;
    
    public LaunchkeyButton(final ButtonMidiType type, final int midiId, final String name,
        final HardwareSurface surface, final MidiProcessor midiProcessor) {
        this(type, 0, midiId, name, surface, midiProcessor);
    }
    
    public LaunchkeyButton(final ButtonMidiType type, final int channel, final int midiId, final String name,
        final HardwareSurface surface, final MidiProcessor midiProcessor) {
        this.midiProcessor = midiProcessor;
        this.channel = channel;
        this.midiId = midiId;
        hwButton = surface.createHardwareButton(name + "_" + midiId);
        if (type == ButtonMidiType.CC) {
            midiProcessor.setCcMatcher(hwButton, midiId, channel);
        } else {
            midiProcessor.setNoteMatcher(hwButton, midiId);
        }
        hwButton.isPressed().markInterested();
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
    
    
    public void bindPressed(final Layer layer, final Runnable action) {
        layer.bind(hwButton, hwButton.pressedAction(), action);
    }
    
    public void bindReleased(final Layer layer, final Runnable action) {
        layer.bind(hwButton, hwButton.releasedAction(), action);
    }
    
    public void bindRepeatHold(final Layer layer, final Runnable pressedAction, final int repeatDelay,
        final int repeatFrequency) {
        layer.bind(
            hwButton, hwButton.pressedAction(), () -> initiateRepeat(pressedAction, repeatDelay, repeatFrequency));
        layer.bind(hwButton, hwButton.releasedAction(), this::cancelEvent);
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
    
    public void bindToggle(final Layer layer, final SettableBooleanValue value) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> value.toggle());
    }
}