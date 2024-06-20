package com.bitwig.extensions.controllers.mcu.control;

import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mcu.MidiProcessor;
import com.bitwig.extensions.controllers.mcu.bindings.FaderBinding;
import com.bitwig.extensions.controllers.mcu.bindings.paramslots.FaderSlotBinding;
import com.bitwig.extensions.controllers.mcu.bindings.paramslots.ResetableAbsoluteValueSlotBinding;
import com.bitwig.extensions.controllers.mcu.config.McuAssignments;
import com.bitwig.extensions.controllers.mcu.devices.ParamPageSlot;
import com.bitwig.extensions.framework.AbsoluteHardwareControlBinding;
import com.bitwig.extensions.framework.Layer;

public class MotorSlider {

    private final HardwareSlider fader;
    private final FaderResponse response;
    private final HardwareButton touchButton;

    public MotorSlider(final HardwareSurface surface, final MidiProcessor midiProcessor, final int channel) {
        fader = surface.createHardwareSlider("FADER_%d_%d".formatted(midiProcessor.getPortIndex(), channel));
        midiProcessor.attachPitchBendSliderValue(fader, channel);

        response = new FaderResponse(midiProcessor, channel);
        touchButton = surface.createHardwareButton(
                "FADER_TOUCH_%d_%d".formatted(midiProcessor.getPortIndex(), channel));
        int touchNote = McuAssignments.TOUCH_VOLUME.getNoteNo() + channel;
        midiProcessor.attachNoteOnOffMatcher(touchButton, 0, touchNote);
        fader.setHardwareButton(touchButton);
    }

    public void addTouchAction(BooleanValueChangedCallback touchAction) {
        touchButton.isPressed().addValueObserver(touchAction);
    }

    public void bindParameter(final Layer layer, final ParamPageSlot parameter) {
        layer.addBinding(new FaderSlotBinding(parameter, response));
        layer.addBinding(new ResetableAbsoluteValueSlotBinding(fader, parameter));
        layer.bind(touchButton, touchButton.pressedAction(), () -> parameter.touch(true));
        layer.bind(touchButton, touchButton.releasedAction(), () -> parameter.touch(false));
    }

    public void bindParameter(final Layer layer, final Parameter parameter) {
        layer.addBinding(new FaderBinding(parameter, response));
        layer.addBinding(new AbsoluteHardwareControlBinding(fader, parameter));
        layer.bind(touchButton, touchButton.pressedAction(), () -> parameter.touch(true));
        layer.bind(touchButton, touchButton.releasedAction(), () -> parameter.touch(false));
    }

    public ResetableAbsoluteValueSlotBinding createSlotBinding(ParamPageSlot paramPageSlot) {
        return new ResetableAbsoluteValueSlotBinding(fader, paramPageSlot);
    }

    public HardwareSlider getFader() {
        return fader;
    }

    public void sendValue(final int value) {
        response.sendValue(0);
    }

}
