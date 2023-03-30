package com.bitwig.extensions.controllers.akai.apcmk2.control;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.akai.apcmk2.MidiProcessor;
import com.bitwig.extensions.controllers.akai.apcmk2.led.RgbLightState;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.time.TimeRepeatEvent;
import com.bitwig.extensions.framework.time.TimedEvent;

import java.util.function.Function;
import java.util.function.Supplier;

public class ApcButton {
    public static final int STD_REPEAT_DELAY = 400;
    public static final int STD_REPEAT_FREQUENCY = 50;

    protected MultiStateHardwareLight light;
    protected HardwareButton hwButton;
    protected MidiProcessor midiProcessor;
    private TimedEvent currentTimer;
    private long recordedDownTime;
    protected final int midiId;

    protected ApcButton(final int midiId, HardwareSurface surface, final MidiProcessor midiProcessor) {
        this.midiProcessor = midiProcessor;
        final MidiIn midiIn = midiProcessor.getMidiIn();
        this.midiId = midiId;
        hwButton = surface.createHardwareButton("PAD_" + midiId);
        hwButton.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(0, midiId));
        hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, midiId));
        light = surface.createMultiStateHardwareLight("PAD_LIGHT_" + midiId);
        light.state().setValue(RgbLightState.OFF);
        hwButton.isPressed().markInterested();
    }

    public void bindPressed(final Layer layer, final Runnable action) {
        layer.bind(hwButton, hwButton.pressedAction(), action);
    }

    public void bindPressed(final Layer layer, final HardwareActionBindable action) {
        layer.bind(hwButton, hwButton.pressedAction(), action);
    }

    public void bindRelease(final Layer layer, final Runnable action) {
        layer.bind(hwButton, hwButton.releasedAction(), action);
    }

    public void bindLight(final Layer layer, final Supplier<InternalHardwareLightState> supplier) {
        layer.bindLightState(supplier, light);
    }

    public void bindLight(final Layer layer, final Function<Boolean, InternalHardwareLightState> pressedCombine) {
        layer.bindLightState(() -> pressedCombine.apply(hwButton.isPressed().get()), light);
    }

    public void bindLightPressed(final Layer layer, final InternalHardwareLightState state,
                                 final InternalHardwareLightState holdState) {
        layer.bindLightState(() -> hwButton.isPressed().get() ? holdState : state, light);
    }

    /**
     * Binds the given action to a button. Upon pressing the button the action is immediately executed. However while
     * holding the button, the action repeats after an initial delay. The standard delay time of 400ms and repeat
     * frequency of 50ms are used.
     *
     * @param layer  the layer this is bound to
     * @param action action to be invoked and after a delay repeat
     */
    public void bindRepeatHold(final Layer layer, final Runnable action) {
        layer.bind(hwButton, hwButton.pressedAction(),
           () -> initiateRepeat(action, STD_REPEAT_DELAY, STD_REPEAT_FREQUENCY));
        layer.bind(hwButton, hwButton.releasedAction(), this::cancelEvent);
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


}
