package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.ButtonMidiType;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.LaunchRelEncoder;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.LaunchkeyButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.RelAbsEncoder;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.RgbButton;
import com.bitwig.extensions.framework.di.Component;

@Component
public class LaunchkeyHwElements {
    private final HardwareSlider[] sliders = new HardwareSlider[8];
    private final HardwareSlider masterSlider;
    private final RgbButton[] sessionButtons = new RgbButton[16];
    private final RgbButton[] drumButtons = new RgbButton[16];
    private final RgbButton[] trackButtons = new RgbButton[8];
    private final RelAbsEncoder[] valueEncoders = new RelAbsEncoder[8];
    private final LaunchRelEncoder[] incEncoders = new LaunchRelEncoder[8];
    private final LaunchkeyButton shiftButton;
    private final static int[] DRUM_MIDI_MAPPING = {
        0x28, 0x29, 0x2A, 0x2B, 0x30, 0x31, 0x32, 0x33, 0x24, 0x25, 0x26, 0x27, 0x2C, 0x2D, 0x2E, 0x2F
    };
    
    private final Map<CcAssignments, RgbButton> buttonMap = new HashMap<>();
    
    public LaunchkeyHwElements(final HardwareSurface surface, final MidiProcessor midiProcessor,
        final ControllerHost host) {
        shiftButton = new LaunchkeyButton(ButtonMidiType.CC, 0x6, 0x3F, "SHIFT", surface, midiProcessor);
        for (int i = 0; i < sessionButtons.length; i++) {
            final int noteId = 0x60 + (0x10 * (i / 8)) + i % 8;
            sessionButtons[i] = new RgbButton(ButtonMidiType.PAD, noteId, "PAD", surface, midiProcessor);
            drumButtons[i] = new RgbButton(ButtonMidiType.PAD, DRUM_MIDI_MAPPING[i], "DRUM", surface, midiProcessor);
        }
        final MidiIn midiIn = midiProcessor.getMidiIn();
        for (int i = 0; i < 8; i++) {
            sliders[i] = surface.createHardwareSlider("SLIDER_" + (i + 1));
            sliders[i].setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0xF, 5 + i));
            trackButtons[i] = new RgbButton(ButtonMidiType.CC, 0x25 + i, "TRACK", surface, midiProcessor);
            valueEncoders[i] = new RelAbsEncoder(0x15 + i, 0xF, surface, midiProcessor);
            incEncoders[i] = new LaunchRelEncoder(surface, midiProcessor, i);
        }
        masterSlider = surface.createHardwareSlider("MASTER_SLIDER");
        masterSlider.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0xF, 13));
        
        for (final CcAssignments assignment : CcAssignments.values()) {
            buttonMap.put(
                assignment,
                new RgbButton(ButtonMidiType.CC, assignment.getCcNr(), assignment.toString(), surface, midiProcessor));
        }
        
    }
    
    public LaunchkeyButton getShiftButton() {
        return shiftButton;
    }
    
    public RgbButton[] getSessionButtons() {
        return sessionButtons;
    }
    
    public RgbButton[] getTrackButtons() {
        return trackButtons;
    }
    
    public RgbButton[] getDrumButtons() {
        return drumButtons;
    }
    
    public HardwareSlider[] getSliders() {
        return sliders;
    }
    
    public HardwareSlider getMasterSlider() {
        return masterSlider;
    }
    
    public RelAbsEncoder[] getValueEncoders() {
        return valueEncoders;
    }
    
    public LaunchRelEncoder[] getIncEncoders() {
        return incEncoders;
    }
    
    public RgbButton getButton(final CcAssignments assignment) {
        return buttonMap.get(assignment);
    }
}
