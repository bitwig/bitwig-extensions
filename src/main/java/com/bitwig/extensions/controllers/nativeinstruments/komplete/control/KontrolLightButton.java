package com.bitwig.extensions.controllers.nativeinstruments.komplete.control;

import java.util.function.BooleanSupplier;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.MidiProcessor;
import com.bitwig.extensions.framework.Layer;

public class KontrolLightButton {
    
    private final HardwareButton hwButton;
    private final MidiProcessor midiProcessor;
    private final OnOffHardwareLight hwLight;
    private KontrolLightButton linkedButton;
    private final int ccNr;
    private final int lightOnValue;
    
    public KontrolLightButton(final String name, final int ccNr, final int pressValue, final int lightOnValue,
        final HardwareSurface surface, final MidiProcessor midiProcessor) {
        this.ccNr = ccNr;
        this.lightOnValue = lightOnValue;
        hwButton = surface.createHardwareButton("%s_BUTTON".formatted(name));
        this.midiProcessor = midiProcessor;
        hwButton.pressedAction()
            .setActionMatcher(midiProcessor.getMidiIn().createCCActionMatcher(0xF, ccNr, pressValue));
        hwLight = surface.createOnOffHardwareLight("%s_BUTTON_LIGHT".formatted(name));
        hwLight.isOn().onUpdateHardware(this::updateLight);
    }
    
    public KontrolLightButton(final String name, final int ccNr, final int pressValue, final HardwareSurface surface,
        final MidiProcessor midiProcessor) {
        this(name, ccNr, pressValue, pressValue == 0x7F ? 2 : 1, surface, midiProcessor);
    }
    
    public void linkLights(final KontrolLightButton other) {
        this.linkedButton = other;
        other.linkedButton = this;
    }
    
    public int getLightStatus() {
        return hwLight.isOn().currentValue() ? lightOnValue : 0x0;
    }
    
    private void updateLight(final boolean on) {
        final int bw = linkedButton.getLightStatus();
        midiProcessor.sendLedUpdate(ccNr, (on ? lightOnValue : 0x0) | bw);
    }
    
    public void updateLights() {
        midiProcessor.sendLedUpdate(ccNr, getLightStatus() | linkedButton.getLightStatus());
    }
    
    public HardwareButton getHwButton() {
        return hwButton;
    }
    
    public OnOffHardwareLight getHwLight() {
        return hwLight;
    }
    
    public void bindPressed(final Layer layer, final Runnable action) {
        layer.bindPressed(hwButton, action);
    }
    
    public void bindLight(final Layer layer, final BooleanValue booleanValue) {
        booleanValue.markInterested();
        layer.bind(booleanValue, hwLight);
    }
    
    public void bindLight(final Layer layer, final BooleanSupplier booleanValue) {
        layer.bind(booleanValue, hwLight);
    }
    
    public void bind(final Layer layer, final Runnable action, final BooleanValue booleanValue) {
        bindPressed(layer, action);
        bindLight(layer, booleanValue);
    }
    
    public void bind(final Layer layer, final Runnable action, final BooleanSupplier booleanValue) {
        bindPressed(layer, action);
        bindLight(layer, booleanValue);
    }
}
