package com.bitwig.extensions.controllers.akai.apc64.control;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.akai.apc.common.led.RgbLightState;
import com.bitwig.extensions.controllers.akai.apc64.Apc64MidiProcessor;
import com.bitwig.extensions.controllers.akai.apc64.layer.MainDisplay;
import com.bitwig.extensions.framework.Layer;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class TouchSlider {

    private final HardwareSlider fader;
    private final FaderResponse response;
    private final HardwareButton touchButton;
    private final MultiStateHardwareLight light;
    private final MultiStateHardwareLight lightState;

    private final int index;
    private final Apc64MidiProcessor midiProcessor;

    public TouchSlider(final int index, final HardwareSurface surface, final Apc64MidiProcessor midiProcessor) {
        fader = surface.createHardwareSlider("FADER_" + index);
        this.index = index;
        this.midiProcessor = midiProcessor;
        final MidiIn midiIn = midiProcessor.getMidiIn();
        fader.setAdjustValueMatcher(midiIn.createAbsolutePitchBendValueMatcher(index));

        response = new FaderResponse(midiProcessor, index);

        touchButton = surface.createHardwareButton("FADER_TOUCH_" + index);
        touchButton.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, 0x52 + index));
        touchButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, 0x52 + index));
        touchButton.isPressed().markInterested();
        fader.setHardwareButton(touchButton);
        light = surface.createMultiStateHardwareLight("FADER_COLOR_" + index);
        light.state().onUpdateHardware(this::updateLight);
        lightState = surface.createMultiStateHardwareLight("FADER_STATE_" + index);
        lightState.state().onUpdateHardware(this::updateState);
    }

    private void updateLight(final InternalHardwareLightState internalHardwareLightState) {
        if (internalHardwareLightState instanceof RgbLightState state) {
            midiProcessor.sendMidi(0xB0, 0x70 + index, state.getColorIndex());
        }
    }

    private void updateState(final InternalHardwareLightState internalHardwareLightState) {
        if (internalHardwareLightState instanceof FaderLightState state) {
            midiProcessor.sendMidi(0xB0, 0x68 + index, state.getCode());
        }
    }

    public void bindParameter(final Layer layer, MainDisplay display, StringValue parameterOwner,
                              final Parameter parameter) {
        layer.addBinding(new FaderBinding(parameter, response));
        layer.addBinding(
                new TouchSliderControlBinding(index, this, parameter, parameterOwner, midiProcessor.getShiftMode(),
                        midiProcessor.getClearMode(), display));
    }

    public void bindIsPressed(final Layer layer, Consumer<Boolean> consumer) {
        layer.bind(touchButton, touchButton.pressedAction(), () -> consumer.accept(true));
        layer.bind(touchButton, touchButton.releasedAction(), () -> consumer.accept(false));
    }

    public void bindLightColor(final Layer layer, Supplier<InternalHardwareLightState> supplier) {
        layer.bindLightState(supplier, light);
    }

    public void bindLightState(final Layer layer, Supplier<InternalHardwareLightState> supplier) {
        layer.bindLightState(supplier, lightState);
    }

    public boolean isTouched() {
        return touchButton.isPressed().get();
    }

    public HardwareSlider getFader() {
        return fader;
    }

    public void sendValue(final int value) {
        response.sendValue(0);
    }


    public HardwareButton getTouchButton() {
        return touchButton;
    }

    public boolean isAutomated() {
        return false;
    }
}
