package com.bitwig.extensions.controllers.akai.mpkmk4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extensions.controllers.akai.apc.common.control.ClickEncoder;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.Encoder;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkButton;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkCcAssignment;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkMultiStateButton;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.LineDisplay;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkDisplayFont;
import com.bitwig.extensions.framework.di.Component;

@Component
public class MpkHwElements {
    
    private final List<MpkMultiStateButton> gridButtons = new ArrayList<>();
    private final List<Encoder> encoders = new ArrayList<>();
    private final Map<MpkCcAssignment, MpkMultiStateButton> onOffButtons = new HashMap<>();
    private final ClickEncoder mainEncoder;
    private final MpkButton mainEncoderPressButton;
    private final MpkButton shiftButton;
    private final LineDisplay mainLineDisplay;
    private final LineDisplay menuLineDisplay;
    
    public MpkHwElements(final ControllerHost host, final HardwareSurface surface, final MpkMidiProcessor midiProcessor,
        final GlobalStates globalStates) {
        for (int i = 0; i < 16; i++) {
            final int rowIndex = i / 4;
            final int columnIndex = i % 4;
            final String name = "GRID %d %d".formatted(rowIndex + 1, columnIndex + 1);
            gridButtons.add(new MpkMultiStateButton(9, 0x24 + i, false, name, surface, midiProcessor));
        }
        
        mainLineDisplay = new LineDisplay(midiProcessor, MpkDisplayFont.PT24, 3);
        menuLineDisplay = new LineDisplay(midiProcessor, MpkDisplayFont.PT24, 3);
        
        mainEncoder = new ClickEncoder(0xE, host, surface, midiProcessor.getDawMidiIn());
        mainEncoderPressButton = new MpkButton(0, 0xD, true, "ENCODER_PRESS", surface, midiProcessor);
        shiftButton = new MpkButton(0, 0x11, true, "SHIFT", surface, midiProcessor);
        for (final MpkCcAssignment assignment : MpkCcAssignment.values()) {
            final MpkMultiStateButton button =
                new MpkMultiStateButton(0, assignment.getCcNr(), true, assignment.toString(), surface, midiProcessor);
            onOffButtons.put(assignment, button);
        }
        for (int i = 0; i < 8; i++) {
            final Encoder encoder = new Encoder(i, 0x18 + i, surface, midiProcessor.getDawMidiIn());
            encoders.add(encoder);
        }
        mainLineDisplay.setActive(true);
    }
    
    public LineDisplay getMainLineDisplay() {
        return mainLineDisplay;
    }
    
    public LineDisplay getMenuLineDisplay() {
        return menuLineDisplay;
    }
    
    public List<MpkMultiStateButton> getGridButtons() {
        return gridButtons;
    }
    
    public ClickEncoder getMainEncoder() {
        return mainEncoder;
    }
    
    public MpkButton getShiftButton() {
        return shiftButton;
    }
    
    public MpkButton getMainEncoderPressButton() {
        return mainEncoderPressButton;
    }
    
    public MpkMultiStateButton getButton(final MpkCcAssignment assignment) {
        return onOffButtons.get(assignment);
    }
    
    public List<Encoder> getEncoders() {
        return encoders;
    }
    
    public void applyShiftToEncoders(final boolean shiftHeld) {
        for (final Encoder encoder : encoders) {
            //encoder.getEncoder().setStepSize(shiftHeld ? 0.1 : 0.01);
            encoder.getEncoder().setSensitivity(shiftHeld ? 0.5 : 1.0);
        }
    }
    
}
