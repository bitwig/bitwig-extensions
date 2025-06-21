package com.bitwig.extensions.controllers.mcu.control;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.controllers.mcu.MidiProcessor;
import com.bitwig.extensions.controllers.mcu.TimedProcessor;
import com.bitwig.extensions.controllers.mcu.config.ButtonAssignment;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.time.TimeRepeatEvent;
import com.bitwig.extensions.framework.time.TimedDelayEvent;
import com.bitwig.extensions.framework.time.TimedEvent;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.Midi;

public class McuButton {
    private final HardwareButton hwButton;
    private final OnOffHardwareLight light;
    private final int noteNr;
    private final int channel;
    private TimedEvent currentTimer;
    private final TimedProcessor timedProcessor;
    private long recordedDownTime;
    public static final int FAST_ACTION_TIME = 5;
    public static final int STD_REPEAT_DELAY = 400;
    public static final int STD_REPEAT_FREQUENCY = 50;
    
    public McuButton(final int noteNr, final String name, final HardwareSurface surface,
        final MidiProcessor midiProcessor, final TimedProcessor timedProcessor) {
        this.timedProcessor = timedProcessor;
        this.channel = 0;
        hwButton = surface.createHardwareButton("B_%s".formatted(name));
        midiProcessor.attachNoteOnOffMatcher(hwButton, 0, noteNr);
        this.noteNr = noteNr;
        light = surface.createOnOffHardwareLight("BL_%s".formatted(name));
        hwButton.setBackgroundLight(light);
        light.onUpdateHardware(
            () -> midiProcessor.sendLedLightStatus(noteNr, 0, light.isOn().currentValue() ? 127 : 0));
    }
    
    public McuButton(final ButtonAssignment assignment, final int subIndex, final HardwareSurface surface,
        final MidiProcessor midiProcessor, final TimedProcessor timedProcessor) {
        this.timedProcessor = timedProcessor;
        this.channel = assignment.getChannel();
        //McuExtension.println(" Create button %s %d %d", assignment, assignment.getNoteNo(), assignment.getChannel());
        hwButton = surface.createHardwareButton("B%d_%s_".formatted(subIndex, assignment));
        midiProcessor.attachNoteOnOffMatcher(hwButton, assignment.getChannel(), assignment.getNoteNo());
        this.noteNr = assignment.getNoteNo();
        light = surface.createOnOffHardwareLight("BL%d_%s_".formatted(subIndex, assignment));
        hwButton.setBackgroundLight(light);
        light.onUpdateHardware(
            () -> midiProcessor.sendLedLightStatus(this.noteNr, this.channel, light.isOn().currentValue() ? 127 : 0));
    }
    
    public int getNoteNr() {
        return noteNr;
    }
    
    public void bindToggle(final Layer layer, final SettableBooleanValue value) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> value.toggle());
        layer.bind(value, light.isOn());
    }
    
    public void bindIsPressed(final Layer layer, final BooleanValueChangedCallback callback) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> callback.valueChanged(true));
        layer.bind(hwButton, hwButton.releasedAction(), () -> callback.valueChanged(false));
    }
    
    public void bindLight(final Layer layer, final BooleanValue value) {
        value.markInterested();
        layer.bind(value, light.isOn());
    }
    
    public void bindLight(final Layer layer, final BooleanSupplier value) {
        layer.bind(value, light.isOn());
    }
    
    public void bindMomentary(final Layer layer, final BooleanValueObject value) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> value.set(true));
        layer.bind(hwButton, hwButton.releasedAction(), () -> value.set(false));
        layer.bind(value, light.isOn());
    }
    
    public void bindMode(final Layer layer, final Consumer<Boolean> consumer, final BooleanSupplier ledState) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> consumer.accept(true));
        layer.bind(hwButton, hwButton.releasedAction(), () -> consumer.accept(false));
        layer.bind(ledState, light.isOn());
    }
    
    public void bindRelease(final Layer layer, final Runnable action) {
        layer.bind(hwButton, hwButton.releasedAction(), () -> action.run());
    }
    
    public void bindPressedLight(final Layer layer, final Runnable action) {
        bindPressed(layer, action);
        bindHeldLight(layer);
    }
    
    public void bindPressed(final Layer layer, final Runnable action) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> action.run());
    }
    
    public void bindHeldLight(final Layer layer) {
        layer.bind(hwButton.isPressed(), light.isOn());
    }
    
    public void bindPressed(final Layer layer, final HardwareActionBindable action) {
        layer.bind(hwButton, hwButton.pressedAction(), action);
    }
    
    public void bindRepeatHold(final Layer layer, final Runnable action) {
        layer.bind(hwButton, hwButton.pressedAction(),
            () -> initiateRepeat(action, STD_REPEAT_DELAY, STD_REPEAT_FREQUENCY));
        layer.bind(hwButton, hwButton.releasedAction(), this::cancelEvent);
    }
    
    public void initiateRepeat(final Runnable action, final int repeatDelay, final int repeatFrequency) {
        action.run();
        currentTimer = new TimeRepeatEvent(action, repeatDelay, repeatFrequency);
        timedProcessor.queueEvent(currentTimer);
    }
    
    private void cancelEvent() {
        if (currentTimer != null) {
            currentTimer.cancel();
            currentTimer = null;
        }
    }
    
    public void bindRepeatHold(final Layer layer, final IntConsumer action) {
        layer.bind(hwButton, hwButton.pressedAction(),
            () -> initiateRepeat(action, STD_REPEAT_DELAY, STD_REPEAT_FREQUENCY));
        layer.bind(hwButton, hwButton.releasedAction(), this::cancelEvent);
    }
    
    public void initiateRepeat(final IntConsumer action, final int repeatDelay, final int repeatFrequency) {
        action.accept(0);
        currentTimer = new TimeRepeatEvent(action, repeatDelay, repeatFrequency);
        timedProcessor.queueEvent(currentTimer);
    }
    
    public void bindRepeatHold(final Layer layer, final IntConsumer action, final Runnable fastCommandAction) {
        layer.bind(hwButton, hwButton.pressedAction(),
            () -> initiateRepeatSeq(action, STD_REPEAT_DELAY, STD_REPEAT_FREQUENCY));
        layer.bind(hwButton, hwButton.releasedAction(), () -> handleSeqRelease(fastCommandAction));
    }
    
    public void initiateRepeatSeq(final IntConsumer action, final int repeatDelay, final int repeatFrequency) {
        recordedDownTime = System.currentTimeMillis();
        timedProcessor.delayTask(() -> {
            if (recordedDownTime != -1 && (System.currentTimeMillis() - recordedDownTime) > FAST_ACTION_TIME) {
                recordedDownTime = -1;
                action.accept(0);
            }
        }, FAST_ACTION_TIME);
        currentTimer = new TimeRepeatEvent(action, repeatDelay, repeatFrequency);
        timedProcessor.queueEvent(currentTimer);
    }
    
    public void handleSeqRelease(final Runnable fastCommandAction) {
        if (recordedDownTime != -1 && (System.currentTimeMillis() - recordedDownTime) < FAST_ACTION_TIME) {
            fastCommandAction.run();
            recordedDownTime = -1;
        }
        this.cancelEvent();
    }
    
    public void bindDelayedAction(final Layer layer, final Runnable baseAction, final Runnable delayedAction,
        final Runnable releaseAction, final int time) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> {
            baseAction.run();
            currentTimer = new TimedDelayEvent(delayedAction, time);
            timedProcessor.queueEvent(currentTimer);
        });
        layer.bind(hwButton, hwButton.releasedAction(), () -> {
            if (currentTimer != null && !currentTimer.isCompleted()) {
                currentTimer.cancel();
            } else {
                releaseAction.run();
            }
        });
    }
    
    public void bindDelayedAction(final Layer layer, final Consumer<Boolean> baseAction, final Runnable delayedAction,
        final Runnable releaseAction, final int time) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> {
            baseAction.accept(true);
            currentTimer = new TimedDelayEvent(delayedAction, time);
            timedProcessor.queueEvent(currentTimer);
        });
        layer.bind(hwButton, hwButton.releasedAction(), () -> {
            baseAction.accept(false);
            if (currentTimer != null && !currentTimer.isCompleted()) {
                currentTimer.cancel();
            } else {
                releaseAction.run();
            }
        });
    }
    
    public void bindClickAltMenu(final Layer layer, final Runnable clickAction, final Consumer<Boolean> holdFunction) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> handleClickDown(holdFunction));
        layer.bind(hwButton, hwButton.releasedAction(), () -> handleClickUp(clickAction, holdFunction));
    }
    
    private void handleClickDown(final Consumer<Boolean> holdFunction) {
        cancelEvent();
        currentTimer = new TimedDelayEvent(() -> holdFunction.accept(true), 400);
        timedProcessor.queueEvent(currentTimer);
        recordedDownTime = System.currentTimeMillis();
    }
    
    private void handleClickUp(final Runnable clickAction, final Consumer<Boolean> holdFunction) {
        if (currentTimer instanceof TimedDelayEvent && !currentTimer.isCompleted()) {
            cancelEvent();
            clickAction.run();
        } else {
            holdFunction.accept(false);
        }
    }
    
    public void clear(final MidiProcessor midiProcessor) {
        midiProcessor.sendMidi(Midi.NOTE_ON | channel, noteNr, 0);
    }
}
