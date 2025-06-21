package com.bitwig.extensions.controllers.reloop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.framework.di.Component;

@Component
public class HwElements {
    //private final List<RgbButton> ccButtons = new ArrayList<>();
    private final List<RgbButton> noteButtons = new ArrayList<>();
    private final List<ChannelControls> channelControls = new ArrayList<>();
    private final Map<Assignment, LedButton> buttonsMap = new HashMap<>();
    private final StepEncoder mainEncoder;
    private final HardwareButton shiftButton;
    private final StepEncoder shiftEncoder;
    private final HardwareButton encoderButton;
    //private final HardwareButton ccButton;
    
    public HwElements(final HardwareSurface surface, final MidiProcessor midiProcessor,
        final GlobalStates globalStates) {
        final MidiIn midiIn = midiProcessor.getMidiIn();
        mainEncoder = new StepEncoder(surface, midiProcessor, 0x1, 0x64);
        shiftButton = createHwButton(surface, midiIn, "SHIFT", 0x1, 0x78);
        shiftEncoder = new StepEncoder(surface, midiProcessor, 0x1, 0x79);
        encoderButton = createHwButton(surface, midiIn, "ENCODER_BUTTON", 1, 0x7E);
        //ccButton = createHwButton(surface, midiIn, "CC_BUTTON", 1, 0x67);
        for (int i = 0; i < 64; i++) {
            //ccButtons.add(new RgbButton(surface, midiProcessor, RgbButton.Mode.CC, i));
            noteButtons.add(new RgbButton(surface, midiProcessor, RgbButton.Mode.NOTE, i));
        }
        Arrays.stream(Assignment.values())
            .forEach(assignment -> buttonsMap.put(assignment, new LedButton(surface, midiProcessor, assignment)));
        for (int i = 0; i < 32; i++) {
            final ChannelControls control = new ChannelControls(surface, midiProcessor, globalStates, i);
            channelControls.add(control);
        }
    }
    
    private HardwareButton createHwButton(final HardwareSurface surface, final MidiIn midiIn, final String name,
        final int channel, final int ccNr) {
        final HardwareButton encoderButton = surface.createHardwareButton(name);
        encoderButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccNr, 0x7F));
        encoderButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccNr, 0x00));
        return encoderButton;
    }
    
    public HardwareButton getEncoderButton() {
        return encoderButton;
    }
    
    public StepEncoder getMainEncoder() {
        return mainEncoder;
    }
    
    public HardwareButton getShiftButton() {
        return shiftButton;
    }
    
    public StepEncoder getShiftEncoder() {
        return shiftEncoder;
    }
    
    public ChannelControls getChannelControl(final int index) {
        return channelControls.get(index);
    }
    
    public List<RgbButton> getNoteButtons() {
        return noteButtons;
    }
    
    public LedButton get(final Assignment assignment) {
        return buttonsMap.get(assignment);
    }
    
    //    public HardwareButton getCcButton() {
    //        return ccButton;
    //    }
}
