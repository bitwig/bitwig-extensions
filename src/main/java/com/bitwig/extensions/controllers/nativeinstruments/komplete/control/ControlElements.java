package com.bitwig.extensions.controllers.nativeinstruments.komplete.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.CcAssignment;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.MidiProcessor;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public class ControlElements {
    
    private final KontrolLightButton previousPresetButton;
    private final KontrolLightButton nextPresetButtonButton;
    
    private final KontrolLightButton trackNavLeftButton;
    private final KontrolLightButton trackRightNavButton;
    
    private final MidiProcessor midiProcessor;
    private final HardwareButton muteSelectedButton;
    private final HardwareButton soloSelectedButton;
    private final HardwareButton shiftButton;
    private final BooleanValueObject shiftHeld = new BooleanValueObject();
    private final List<RelativeHardwareKnob> deviceKnobs;
    private final List<RelativeHardwareKnob> volumeKnobs;
    private final List<RelativeHardwareKnob> panKnobs;
    private final RelativeHardwareKnob fourDKnobMixer;
    private final ModeButton knobPressed;
    private final ModeButton knobShiftPressed;
    private final HardwareSurface surface;
    private final RelativeHardwareKnob fourDKnob;
    private final RelativeHardwareKnob loopModKnob;
    private final RelativeHardwareKnob fourDKnobPan;
    
    private final Map<CcAssignment, ModeButton> modeButtons = new HashMap<>();
    private final KontrolLightButton leftNavButton;
    private final KontrolLightButton rightNavButton;
    private final KontrolLightButton upNavButton;
    private final KontrolLightButton downNavButton;
    
    public ControlElements(final HardwareSurface surface, final MidiProcessor midiProcessor,
        final boolean switchedNavMapping) {
        final MidiIn midiIn = midiProcessor.getMidiIn();
        this.midiProcessor = midiProcessor;
        this.surface = surface;
        shiftButton = createOnOffButton("SHIFT_BUTTON", 0x4);
        
        trackNavLeftButton = new KontrolLightButton("TRACK_LEFT_NAV", 0x31, 127, surface, midiProcessor);
        trackRightNavButton = new KontrolLightButton("TRACK_RIGHT_NAV", 0x31, 1, surface, midiProcessor);
        trackNavLeftButton.linkLights(trackRightNavButton);
        
        Arrays.stream(CcAssignment.values()) //
            .filter(CcAssignment::isMapped)  //
            .forEach(ccAssignment -> modeButtons.put(
                ccAssignment,
                new ModeButton(midiProcessor, ccAssignment.getName(), ccAssignment)));
        
        muteSelectedButton = surface.createHardwareButton("MUTE_SELECTED_BUTTON");
        muteSelectedButton.pressedAction().setActionMatcher(CcAssignment.MUTE_CURRENT.createActionMatcher(midiIn, 1));
        
        soloSelectedButton = surface.createHardwareButton("SOLO_SELECTED_BUTTON");
        soloSelectedButton.pressedAction().setActionMatcher(CcAssignment.SOLO_CURRENT.createActionMatcher(midiIn, 1));
        deviceKnobs = createKnobs(surface, "DEVICE", 0x70);
        volumeKnobs = createKnobs(surface, "VOLUME", 0x50);
        panKnobs = createKnobs(surface, "PAN", 0x58);
        
        previousPresetButton = new KontrolLightButton("PRESET_PREVIOUS", 0x36, 127, surface, midiProcessor);
        nextPresetButtonButton = new KontrolLightButton("PRESET_NEXT", 0x36, 1, surface, midiProcessor);
        previousPresetButton.linkLights(nextPresetButtonButton);
        
        knobPressed = new ModeButton(midiProcessor, "KNOB4D_PRESSED", CcAssignment.PRESS_4D_KNOB);
        knobShiftPressed = new ModeButton(midiProcessor, "KNOB4D_PRESSED_SHIFT", CcAssignment.PRESS_4D_KNOB_SHIFT);
        
        fourDKnobMixer = surface.createRelativeHardwareKnob("4D_WHEEL_MIX_MODE");
        fourDKnobMixer.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x64, 4096));
        fourDKnobMixer.setStepSize(1 / 128.0);
        
        fourDKnobPan = surface.createRelativeHardwareKnob("4D_WHEEL_MIX_MODE_PAN");
        fourDKnobPan.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x65, 4096));
        fourDKnobPan.setStepSize(1 / 128.0);
        
        fourDKnob = surface.createRelativeHardwareKnob("4D_WHEEL_PLUGIN_MODE");
        fourDKnob.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x34, 128));
        fourDKnob.setStepSize(1 / 128.0);
        
        loopModKnob = surface.createRelativeHardwareKnob("4D_LOOP");
        loopModKnob.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x35, 128));
        loopModKnob.setStepSize(1 / 128.0);
        leftNavButton =
            new KontrolLightButton("LEFT_NAV", switchedNavMapping ? 0x32 : 0x30, 0x7f, 1, surface, midiProcessor);
        rightNavButton =
            new KontrolLightButton("RIGHT_NAV", switchedNavMapping ? 0x32 : 0x30, 0x1, 2, surface, midiProcessor);
        leftNavButton.linkLights(rightNavButton);
        
        upNavButton =
            new KontrolLightButton("UP_NAV", switchedNavMapping ? 0x30 : 0x32, 0x7F, 1, surface, midiProcessor);
        downNavButton =
            new KontrolLightButton("DOWN_NAV", switchedNavMapping ? 0x30 : 0x32, 1, 2, surface, midiProcessor);
        upNavButton.linkLights(downNavButton);
    }
    
    private HardwareButton createOnOffButton(final String name, final int ccNr) {
        final HardwareButton button = surface.createHardwareButton(name);
        button.pressedAction().setActionMatcher(midiProcessor.getMidiIn().createCCActionMatcher(0xF, ccNr, 1));
        button.releasedAction().setActionMatcher(midiProcessor.getMidiIn().createCCActionMatcher(0xF, ccNr, 0));
        return button;
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
    
    public KontrolLightButton getLeftNavButton() {
        return leftNavButton;
    }
    
    public KontrolLightButton getRightNavButton() {
        return rightNavButton;
    }
    
    public KontrolLightButton getDownNavButton() {
        return downNavButton;
    }
    
    public KontrolLightButton getUpNavButton() {
        return upNavButton;
    }
    
    public ModeButton getButton(final CcAssignment assignment) {
        return modeButtons.get(assignment);
    }
    
    public RelativeHardwareKnob getFourDKnob() {
        return fourDKnob;
    }
    
    public ModeButton getKnobPressed() {
        return knobPressed;
    }
    
    public ModeButton getKnobShiftPressed() {
        return knobShiftPressed;
    }
    
    public RelativeHardwareKnob getFourDKnobMixer() {
        return fourDKnobMixer;
    }
    
    public RelativeHardwareKnob getFourDKnobPan() {
        return fourDKnobPan;
    }
    
    public RelativeHardwareKnob getLoopModKnob() {
        return loopModKnob;
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
    
    public BooleanValueObject getShiftHeld() {
        return shiftHeld;
    }
    
    public HardwareButton getShiftButton() {
        return shiftButton;
    }
    
    public KontrolLightButton getTrackNavLeftButton() {
        return trackNavLeftButton;
    }
    
    public KontrolLightButton getTrackRightNavButton() {
        return trackRightNavButton;
    }
    
    public HardwareButton getMuteSelectedButton() {
        return muteSelectedButton;
    }
    
    public HardwareButton getSoloSelectedButton() {
        return soloSelectedButton;
    }
    
    public KontrolLightButton getPreviousPresetButton() {
        return previousPresetButton;
    }
    
    public KontrolLightButton getNextPresetButtonButton() {
        return nextPresetButtonButton;
    }
    
    public void updateLights() {
        trackNavLeftButton.updateLights();
        nextPresetButtonButton.updateLights();
    }
}
