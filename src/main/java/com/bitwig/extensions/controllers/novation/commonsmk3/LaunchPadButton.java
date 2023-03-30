package com.bitwig.extensions.controllers.novation.commonsmk3;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.time.TimeRepeatEvent;
import com.bitwig.extensions.framework.time.TimedEvent;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class LaunchPadButton {
    public static final int STD_REPEAT_DELAY = 400;
    public static final int STD_REPEAT_FREQUENCY = 50;

    protected HardwareButton hwButton;
    protected MultiStateHardwareLight light;
    protected final MidiProcessor midiProcessor;
    protected final int channel;
    private TimedEvent currentTimer;
    private long recordedDownTime;

    protected LaunchPadButton(final String id, final HardwareSurface surface, final MidiProcessor midiProcessor,
                              final int channel) {
        super();
        this.midiProcessor = midiProcessor;
        this.channel = channel;
        hwButton = surface.createHardwareButton(id);
        light = surface.createMultiStateHardwareLight(id + "-light");
        light.state().setValue(RgbState.of(0));
        hwButton.isPressed().markInterested();
    }

    protected void initButtonNote(final MidiIn midiIn, final int notevalue) {
        hwButton.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(channel, notevalue));
        hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(channel, notevalue));
    }

    protected void initButtonCc(final MidiIn midiIn, final CCSource ccAssignment) {
        hwButton.pressedAction().setActionMatcher(ccAssignment.createMatcher(midiIn, 127));
        hwButton.releasedAction().setActionMatcher(ccAssignment.createMatcher(midiIn, 0));
    }

    protected void initButtonCc(final MidiIn midiIn, final int ccValue) {
        hwButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccValue, 127));
        hwButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccValue, 0));
    }

    public HardwareButton getHwButton() {
        return hwButton;
    }

    public MultiStateHardwareLight getLight() {
        return light;
    }

    public abstract void refresh();

    public void bind(final Layer layer, final Runnable action, final BooleanSupplier state, final NovationColor color) {
        final RgbState onState = color.getHiColor();
        final RgbState offState = color.getLowColor();
        if (state instanceof BooleanValue) {
            ((BooleanValue) state).markInterested();
        }
        layer.bind(hwButton, hwButton.pressedAction(), action);
        layer.bindLightState(() -> pressedStateCombo(hwButton.isPressed(), state, onState, offState), light);
    }

    private RgbState pressedStateCombo(final BooleanValue actionValue, final BooleanSupplier state,
                                       final RgbState onState, final RgbState offState) {
        if (!state.getAsBoolean()) {
            return RgbState.of(0);
        }
        if (actionValue.get()) {
            return onState;
        }
        return offState;
    }

    public void disable(final Layer layer) {
        bindPressed(layer, () -> {
        });
        bindRelease(layer, () -> {
        });
        bindLight(layer, () -> RgbState.OFF);
    }

    public void bind(final Layer layer, final Runnable action, final NovationColor onColor) {
        final RgbState onState = onColor.getHiColor();
        final RgbState offState = onColor.getLowColor();
        layer.bind(hwButton, hwButton.pressedAction(), action);
        layer.bindLightState(() -> hwButton.isPressed().get() ? onState : offState, light);
    }

    /**
     * @param layer         the layer
     * @param downAction    the press down action
     * @param releaseAction the action to be invoked at release passing true if time was exceeded, false if not
     * @param releaseTime   the time the button needs to be down for the release action to be invoked upon release
     */
    public void bindPressReleaseAfter(final Layer layer, final Runnable downAction,
                                      final Consumer<Boolean> releaseAction, final long releaseTime) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> {
            recordedDownTime = System.currentTimeMillis();
            downAction.run();
        });
        layer.bind(hwButton, hwButton.releasedAction(), () -> {
            if (System.currentTimeMillis() - recordedDownTime > releaseTime) {
                releaseAction.accept(true);
            } else {
                releaseAction.accept(false);
            }
        });
    }

    public void bindPressed(final Layer layer, final Runnable action) {
        layer.bind(hwButton, hwButton.pressedAction(), action);
    }

    public void bindRelease(final Layer layer, final Runnable action) {
        layer.bind(hwButton, hwButton.releasedAction(), action);
    }


    public void bind(final Layer layer, final Runnable action, final Supplier<InternalHardwareLightState> supplier) {
        layer.bind(hwButton, hwButton.pressedAction(), action);
        layer.bindLightState(supplier, light);
    }

    public void bindPressed(final Layer layer, final Consumer<Boolean> target,
                            final Function<Boolean, RgbState> colorFunction) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
        layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
        layer.bindLightState(() -> colorFunction.apply(hwButton.isPressed().get()), light);
    }

    public void bindPressed(final Layer layer, final Consumer<Boolean> target, final NovationColor onColor) {
        final RgbState onState = onColor.getHiColor();
        final RgbState offState = onColor.getLowColor();
        layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
        layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
        layer.bindLightState(() -> hwButton.isPressed().get() ? onState : offState, light);
    }

    public void bindPressed(final Layer layer, final Consumer<Boolean> target) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
        layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
    }

    /**
     * Binds the given action to a button. Upon pressing the button the action is immediately executed. However while
     * holding the button, the action repeats after an initial delay.
     *
     * @param layer           the layer this is bound to
     * @param action          action to be invoked and after a delay repeat
     * @param repeatDelay     time in ms until the action gets repeated
     * @param repeatFrequency time interval in ms between repeats, values should be >= 50ms
     */
    public void bindRepeatHold(final Layer layer, final Runnable action, final int repeatDelay,
                               final int repeatFrequency) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> initiateRepeat(action, repeatDelay, repeatFrequency));
        layer.bind(hwButton, hwButton.releasedAction(), this::cancelEvent);
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

    /**
     * Creates a binding that invokes the press action after a delay. If the released before, the press action is not
     * executed.
     *
     * @param layer         the layer this is bound to
     * @param pressAction   the action to be invoked upon delay, if release before this is not invoked
     * @param releaseAction the release action
     * @param delayTime     the time the button need to be held for the press action to be invoked
     */
    public void bindDelayedAction(final Layer layer, final Runnable pressAction, final Runnable releaseAction,
                                  final int delayTime) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> initiateTimed(pressAction, delayTime));
        layer.bind(hwButton, hwButton.releasedAction(), () -> {
            cancelEvent();
            releaseAction.run();
        });
    }

    private void initiateTimed(final Runnable pressAction, final int delayTime) {
        currentTimer = new TimedEvent(pressAction, delayTime);
        midiProcessor.queueEvent(currentTimer);
    }

    public void bindLight(final Layer layer, final Supplier<InternalHardwareLightState> supplier) {
        layer.bindLightState(supplier, light);
    }

    public void bindLight(final Layer layer, final Function<Boolean, InternalHardwareLightState> pressedCombine) {
        layer.bindLightState(() -> pressedCombine.apply(hwButton.isPressed().get()), light);
    }

    public void bindLightPressed(final Layer layer, final RgbState state, final RgbState holdState) {
        layer.bindLightState(() -> hwButton.isPressed().get() ? holdState : state, light);
    }

    /**
     * A light state that is active or not. If not active, the light is off. If active which light depends if the
     * button is being pressed or ot.
     *
     * @param layer         the layer this is bound to
     * @param isActiveState the active state
     * @param active        standard active color
     * @param pressed       active color when button iw being pressed
     */
    public void bindHighlightButton(final Layer layer, final BooleanValue isActiveState, final RgbState active,
                                    final RgbState pressed) {
        layer.bindLightState(() -> {
            if (isActiveState.getAsBoolean()) {
                return hwButton.isPressed().get() ? pressed : active;
            }
            return RgbState.OFF;
        }, light);
    }

    public void bindHighlightButton(final Layer layer, final BooleanSupplier isActiveState, final RgbState active,
                                    final RgbState pressed) {
        layer.bindLightState(() -> {
            if (isActiveState.getAsBoolean()) {
                return hwButton.isPressed().get() ? pressed : active;
            }
            return RgbState.OFF;
        }, light);
    }

    public void bindPressed(final Layer layer, final Consumer<Boolean> target, final RgbState onState,
                            final RgbState offState) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
        layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
        layer.bindLightState(() -> hwButton.isPressed().get() ? onState : offState, light);
    }

    public void bindPressed(final Layer layer, final SettableBooleanValue value, final NovationColor color) {
        final RgbState onState = color.getHiColor();
        final RgbState offState = color.getLowColor();
        layer.bind(hwButton, hwButton.pressedAction(), () -> value.set(true));
        layer.bind(hwButton, hwButton.releasedAction(), () -> value.set(false));
        layer.bindLightState(() -> value.get() ? onState : offState, light);
    }

    private void cancelEvent() {
        if (currentTimer != null) {
            currentTimer.cancel();
            currentTimer = null;
        }
    }

}
