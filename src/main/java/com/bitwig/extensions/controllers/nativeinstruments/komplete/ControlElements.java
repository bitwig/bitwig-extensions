package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.MidiProcessor;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public class ControlElements {
    
    private final HardwareButton trackNavLeftButton;
    private final HardwareButton trackRightNavButton;
    private final OnOffHardwareLight trackNavLeftButtonLight;
    private final OnOffHardwareLight trackNavRightButtonLight;
    private final MidiProcessor midiProcessor;
    private final HardwareButton muteSelectedButton;
    private final HardwareButton soloSelectedButton;
    private final HardwareButton shiftButton;
    private final BooleanValueObject shiftHeld = new BooleanValueObject();
    private final List<RelativeHardwareKnob> deviceKnobs;
    private final List<RelativeHardwareKnob> volumeKnobs;
    private final List<RelativeHardwareKnob> panKnobs;
    private final RelativeHardwareKnob fourDKnobMixer;
    
    public ControlElements(final HardwareSurface surface, final MidiProcessor midiProcessor) {
        final MidiIn midiIn = midiProcessor.getMidiIn();
        this.midiProcessor = midiProcessor;
        shiftButton = surface.createHardwareButton("SHIFT_BUTTON");
        shiftButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x4, 1));
        shiftButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x4, 0));
        
        trackNavLeftButton = surface.createHardwareButton("TRACK_LEFT_NAV_BUTTON");
        trackNavLeftButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x31, 127));
        trackNavLeftButtonLight = surface.createOnOffHardwareLight("LEFT_NAV_BUTTON_LIGHT");
        
        trackRightNavButton = surface.createHardwareButton("TRACK_RIGHT_NAV_BUTTON");
        trackRightNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x31, 1));
        trackNavRightButtonLight = surface.createOnOffHardwareLight("RIGHT_NAV_BUTTON_LIGHT");
        
        trackNavLeftButtonLight.isOn().onUpdateHardware(this::updateLeftLight);
        trackNavRightButtonLight.isOn().onUpdateHardware(this::updateRightLight);
        
        muteSelectedButton = surface.createHardwareButton("MUTE_SELECTED_BUTTON");
        muteSelectedButton.pressedAction().setActionMatcher(CcAssignment.MUTE_CURRENT.createActionMatcher(midiIn, 1));
        
        soloSelectedButton = surface.createHardwareButton("SOLO_SELECTED_BUTTON");
        soloSelectedButton.pressedAction().setActionMatcher(CcAssignment.SOLO_CURRENT.createActionMatcher(midiIn, 1));
        deviceKnobs = createKnobs(surface, "DEVICE", 0x70);
        volumeKnobs = createKnobs(surface, "VOLUME", 0x50);
        panKnobs = createKnobs(surface, "PAN", 0x58);
        
        
        fourDKnobMixer = surface.createRelativeHardwareKnob("4D_WHEEL_MIX_MODE");
        fourDKnobMixer.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x64, 4096));
        fourDKnobMixer.setStepSize(1 / 128.0);
    }
    
    protected List<RelativeHardwareKnob> createKnobs(final HardwareSurface surface, final String name,
        final int baseIndex) {
        final ArrayList<RelativeHardwareKnob> knobs = new ArrayList<>();
        final MidiIn midiIn = midiProcessor.getMidiIn();
        for (int i = 0; i < 8; i++) {
            final RelativeHardwareKnob knob = surface.createRelativeHardwareKnob("%s_KNOB_%d".formatted(name, i));
            knob.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, baseIndex + i, 128));
            knob.setStepSize(1);
            knobs.add(knob);
        }
        return knobs;
    }
    
    public RelativeHardwareKnob getFourDKnobMixer() {
        return fourDKnobMixer;
    }
    
    public List<RelativeHardwareKnob> getDeviceKnobs() {
        return deviceKnobs;
    }
    
    public List<RelativeHardwareKnob> getVolumeKnobs() {
        return volumeKnobs;
    }
    
    public List<RelativeHardwareKnob> getPanKnobs() {
        return panKnobs;
    }
    
    private void updateLeftLight(final boolean on) {
        final int bw = trackNavRightButtonLight.isOn().currentValue() ? 0x1 : 0x0;
        midiProcessor.sendLedUpdate(0x31, (on ? 0x2 : 0x0) | bw);
    }
    
    private void updateRightLight(final boolean on) {
        final int bw = trackNavLeftButtonLight.isOn().currentValue() ? 0x2 : 0x0;
        midiProcessor.sendLedUpdate(0x31, (on ? 0x1 : 0x0) | bw);
    }
    
    public void updateLights() {
        final boolean left = trackNavLeftButtonLight.isOn().currentValue();
        final boolean right = trackNavRightButtonLight.isOn().currentValue();
        final int value = (left ? 0x2 : 0x0) | (right ? 0x1 : 0x0);
        // KompleteKontrolExtension.println(" VALUE = %s %s => %d", left, right, value);
        midiProcessor.sendLedUpdate(0x31, value);
    }
    
    public BooleanValueObject getShiftHeld() {
        return shiftHeld;
    }
    
    public HardwareButton getShiftButton() {
        return shiftButton;
    }
    
    public HardwareButton getTrackNavLeftButton() {
        return trackNavLeftButton;
    }
    
    public HardwareButton getTrackRightNavButton() {
        return trackRightNavButton;
    }
    
    public OnOffHardwareLight getTrackNavLeftButtonLight() {
        return trackNavLeftButtonLight;
    }
    
    public OnOffHardwareLight getTrackNavRightButtonLight() {
        return trackNavRightButtonLight;
    }
    
    public HardwareButton getMuteSelectedButton() {
        return muteSelectedButton;
    }
    
    public HardwareButton getSoloSelectedButton() {
        return soloSelectedButton;
    }
}
