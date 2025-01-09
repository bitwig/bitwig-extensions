package com.bitwig.extensions.controllers.reloop;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.framework.Layer;

public class RgbButton {
    private final HardwareButton button;
    private final MidiProcessor midiProcessor;
    private final int noteValue;
    private final MultiStateHardwareLight light;
    private final Mode mode;
    private ReloopRgb lastColor = ReloopRgb.OFF;
    
    public enum Mode {
        NOTE,
        CC
    }
    
    public RgbButton(final HardwareSurface surface, final MidiProcessor midiProcessor, final Mode mode,
        final int index) {
        final MidiIn midiIn = midiProcessor.getMidiIn();
        this.midiProcessor = midiProcessor;
        this.mode = mode;
        this.noteValue = index + 0x20;
        button = surface.createHardwareButton("PAD_%s_%d".formatted(mode, index));
        if (mode == Mode.NOTE) {
            button.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(9, noteValue));
            button.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(9, noteValue));
        } else {
            button.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, index, 0x7F));
            button.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(0, index, 0x00));
        }
        light = surface.createMultiStateHardwareLight("PAD_LIGHT_%s_%d".formatted(mode, index));
        button.setBackgroundLight(light);
        light.state().onUpdateHardware(this::updateStateNote);
        light.state().setValue(lastColor);
    }
    
    private void updateStateNote(final InternalHardwareLightState intState) {
        if (intState instanceof ReloopRgb state) {
            lastColor = state;
            midiProcessor.sendMidi(0x99, noteValue, state.getColorValue());
        } else {
            midiProcessor.sendMidi(0x99, noteValue, 0);
        }
    }
    
    public void restoreLastColor() {
        if (lastColor != null) {
            midiProcessor.sendMidi(0x99, noteValue, lastColor.getColorValue());
        } else {
            midiProcessor.sendMidi(0x99, noteValue, 0);
        }
    }
    
    public void bindPressed(final Layer layer, final Runnable action) {
        layer.bind(button, button.pressedAction(), () -> {
            action.run();
            //restoreLastColor();
        });
        layer.bind(button, button.releasedAction(), () -> restoreLastColor());
    }
    
    
    public void bindIsPressed(final Layer layer, final Consumer<Boolean> consumer) {
        layer.bind(button, button.pressedAction(), () -> {
            consumer.accept(true);
            //restoreLastColor();
        });
        layer.bind(button, button.releasedAction(), () -> {
            consumer.accept(false);
            //restoreLastColor();
        });
    }
    
    public void bindLight(final Layer layer, final Supplier<InternalHardwareLightState> supplier) {
        layer.bindLightState(supplier, light);
    }
    
}
