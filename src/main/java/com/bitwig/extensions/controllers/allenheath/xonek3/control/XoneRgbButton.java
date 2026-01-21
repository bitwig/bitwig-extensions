package com.bitwig.extensions.controllers.allenheath.xonek3.control;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.controllers.allenheath.xonek3.XoneMidiDevice;
import com.bitwig.extensions.controllers.allenheath.xonek3.color.XoneIndexColor;
import com.bitwig.extensions.controllers.allenheath.xonek3.color.XoneRgbColor;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.MidiStatus;

public class XoneRgbButton {
    
    private final XoneMidiDevice midiProcessor;
    private final int index;
    private final int status;
    private final MidiStatus midiStatus;
    private final HardwareButton hwButton;
    private final int midiNr;
    private final MultiStateHardwareLight light;
    private final int ledIndex;
    private int lastColorIndex = -1;
    
    public XoneRgbButton(final int index, final int ledIndex, final String name, final MidiStatus midiStatus,
        final int nr, final int channel, final HardwareSurface surface, final XoneMidiDevice midiProcessor) {
        this.midiProcessor = midiProcessor;
        this.index = index;
        this.ledIndex = ledIndex;
        this.midiNr = nr;
        this.midiStatus = midiStatus;
        this.status = midiStatus.getStatus(channel);
        hwButton = surface.createHardwareButton("%s %d".formatted(name, index + 1));
        hwButton.isPressed().markInterested();
        final MidiIn midiIn = midiProcessor.getMidiIn();
        if (midiStatus == MidiStatus.NOTE_ON) {
            hwButton.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(channel, nr));
            hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(channel, nr));
        } else {
            hwButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, nr, 0x7F));
            hwButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, nr, 0x0));
        }
        light = surface.createMultiStateHardwareLight("%s LIGHT %d".formatted(name, index + 1));
        light.state().setValue(XoneIndexColor.BLACK);
        light.setColorToStateFunction(XoneRgbColor::forColor);
        hwButton.setBackgroundLight(light);
        if (ledIndex != -1) {
            light.state().onUpdateHardware(this::handleState);
        }
    }
    
    private void handleState(final InternalHardwareLightState state) {
        if (state instanceof final XoneIndexColor color) {
            updateColor(color);
        } else if (state instanceof final XoneRgbColor color) {
            updateColor(color);
        } else {
            midiProcessor.sendMidi(this.status, midiNr, 0);
        }
    }
    
    private void updateColor(final XoneRgbColor color) {
        midiProcessor.updateLed(ledIndex, color.getRed(), color.getGreen(), color.getBlue(), color.getBrightness());
    }
    
    private void updateColor(final XoneIndexColor color) {
        if (color.getColorIndex() != lastColorIndex) {
            midiProcessor.sendMidi(this.status, midiNr, 0);
            midiProcessor.configureLed(ledIndex, 0, color.getColorIndex(), this.midiStatus, this.midiNr);
            midiProcessor.sendMidi(this.status, midiNr, color.stateValue());
            lastColorIndex = color.getColorIndex();
        } else {
            midiProcessor.sendMidi(this.status, midiNr, color.stateValue());
        }
    }
    
    public void bindIsPressed(final Layer layer, final Consumer<Boolean> handler) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> handler.accept(true));
        layer.bind(hwButton, hwButton.releasedAction(), () -> handler.accept(false));
    }
    
    public void bindIsPressed(final Layer layer, final SettableBooleanValue value) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> value.set(true));
        layer.bind(hwButton, hwButton.releasedAction(), () -> value.set(false));
    }
    
    public void bindLight(final Layer layer, final Supplier<InternalHardwareLightState> supplier) {
        layer.bindLightState(supplier, light);
    }
    
    public void bindDisabled(final Layer layer) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> {});
        layer.bind(hwButton, hwButton.releasedAction(), () -> {});
        layer.bindLightState(() -> XoneRgbColor.OFF, light);
    }
    
    public void bindLightPressed(final Layer layer, final InternalHardwareLightState holdState,
        final InternalHardwareLightState releaseState) {
        hwButton.isPressed().markInterested();
        layer.bindLightState(() -> hwButton.isPressed().get() ? holdState : releaseState, light);
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
    
    public BooleanValue isPressed() {
        return hwButton.isPressed();
    }
    
}
