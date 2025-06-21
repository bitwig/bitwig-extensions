package com.bitwig.extensions.controllers.reloop;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.time.TimeRepeatEvent;
import com.bitwig.extensions.framework.values.Midi;

public class LedButton {
    private final HardwareButton button;
    private final OnOffHardwareLight light;
    private final MidiProcessor midiProcessor;
    private final int channel;
    private final int ccNr;
    private TimeRepeatEvent currentTimer;
    public static final int STD_REPEAT_DELAY = 400;
    public static final int STD_REPEAT_FREQUENCY = 50;
    
    public LedButton(final HardwareSurface surface, final MidiProcessor midiProcessor, final Assignment assignment) {
        final MidiIn midiIn = midiProcessor.getMidiIn();
        this.midiProcessor = midiProcessor;
        this.channel = assignment.getChannel();
        this.ccNr = assignment.getValue();
        button = surface.createHardwareButton("BUTTON_%s".formatted(assignment));
        button.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccNr, 127));
        button.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccNr, 0));
        light = surface.createOnOffHardwareLight("BL_%s".formatted(assignment));
        button.setBackgroundLight(light);
        light.isOn().onUpdateHardware(isOn -> handleLightChange(isOn));
    }
    
    private void handleLightChange(final boolean isOn) {
        midiProcessor.sendMidi(Midi.CC | channel, ccNr, isOn ? 0x7F : 0x00);
    }
    
    public void bindLight(final Layer layer, final BooleanValue booleanSupplier) {
        layer.bind(booleanSupplier, light);
    }
    
    public void bindLight(final Layer layer, final BooleanSupplier booleanSupplier) {
        layer.bind(booleanSupplier, light);
    }
    
    public void bindRepeatHold(final Layer layer, final Runnable action) {
        layer.bind(button, button.pressedAction(),
            () -> initiateRepeat(action, STD_REPEAT_DELAY, STD_REPEAT_FREQUENCY));
        layer.bind(button, button.releasedAction(), this::cancelEvent);
    }
    
    public void initiateRepeat(final Runnable action, final int repeatDelay, final int repeatFrequency) {
        action.run();
        currentTimer = new TimeRepeatEvent(action, repeatDelay, repeatFrequency);
        midiProcessor.queueEvent(currentTimer);
    }
    
    private void cancelEvent() {
        if (currentTimer != null) {
            currentTimer.cancel();
            currentTimer = null;
        }
    }
    
    public void bindToggle(final Layer layer, final SettableBooleanValue value) {
        layer.bind(button, button.pressedAction(), () -> value.toggle());
    }
    
    public void bindPressRelease(final Layer layer, final Runnable action) {
        layer.bind(button, button.releasedAction(), () -> action.run());
    }
    
    public void bindPressed(final Layer layer, final Runnable action) {
        layer.bind(button, button.pressedAction(), action);
    }
    
    public void bindPressedReinforce(final Layer layer, final Runnable action, final BooleanSupplier booleanSupplier) {
        layer.bind(button, button.pressedAction(), action);
        layer.bind(button, button.releasedAction(), () -> {
            handleLightChange(booleanSupplier.getAsBoolean());
        });
    }
    
    public void bindPressed(final Layer layer, final HardwareActionBindable action) {
        layer.bind(button, button.pressedAction(), action);
    }
    
    public void bindTogglePressedState(final Layer layer, final Runnable isActiveAction, final BooleanValue value) {
        value.markInterested();
        layer.bind(button, button.pressedAction(), () -> {
            isActiveAction.run();
            if (!value.get()) {
                handleLightChange(false);
            }
        });
        layer.bind(button, button.releasedAction(), () -> {
            isActiveAction.run();
            if (!value.get()) {
                handleLightChange(false);
            }
        });
    }
    
    public void bindIsPressed(final Layer layer, final Consumer<Boolean> consumer) {
        layer.bind(button, button.pressedAction(), () -> consumer.accept(true));
        layer.bind(button, button.releasedAction(), () -> consumer.accept(false));
    }
    
    public void bindLightOff(final Layer layer, final Runnable action) {
        layer.bind(button, button.pressedAction(), () -> {
            action.run();
            handleLightChange(false);
        });
        layer.bind(button, button.releasedAction(), () -> handleLightChange(false));
    }
    
    public void bindLightOnPressed(final Layer layer) {
        layer.bind(button.isPressed(), light);
    }
}
