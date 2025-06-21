package com.bitwig.extensions.controllers.arturia.keylab.mk3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.controls.RgbButton;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.controls.RgbCcButton;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.controls.RgbNoteButton;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.controls.TouchEncoder;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.controls.TouchSlider;
import com.bitwig.extensions.framework.di.Component;

@Component
public class KeylabHardwareElements {
    
    private final int ENCODER_TOUCH = 0x08;
    private final int[] ENCODER_CC = new int[] {0x5B, 0x5C, 0x5E, 0x5F, 0x60, 0x61, 0x66, 0x67, 0x68};
    private final int SLIDER_TOUCH = 0x1C;
    private final int SLIDER = 0x69;
    private final int CONTEXT_BUTTON = 0x2D;
    private final int CONTEXT_BUTTON_LIGHT = 0x15;
    private final int PAD_BUTTON_LIGHT = 0x22;  // BANK A=0 BANK B=+12 BANK C=+24 BANK D=+36
    private final int PAD_BUTTON_DAW_LIGHT = 0x52;
    
    private final Map<CcAssignment, RgbButton> buttonMap = new HashMap<>();
    private final List<RgbButton> contextButtons = new ArrayList<>();
    private final List<TouchEncoder> touchEncoders = new ArrayList<>();
    private final List<TouchSlider> touchSliders = new ArrayList<>();
    private final List<RgbNoteButton> padButtons = new ArrayList<>();
    private final TouchEncoder mainEncoder;
    private final MultiStateHardwareLight sceneStateDisplay;
    
    public KeylabHardwareElements(final HardwareSurface surface, final MidiProcessor midiProcessor) {
        mainEncoder = new TouchEncoder(40, 0x74, 0x75, surface, midiProcessor);
        Arrays.stream(CcAssignment.values()).forEach(
            ccAssignments -> buttonMap.put(ccAssignments, new RgbCcButton(ccAssignments, surface, midiProcessor)));
        for (int i = 0; i < 8; i++) {
            contextButtons.add(
                new RgbCcButton("CONTEXT_%d".formatted(i + 1), CONTEXT_BUTTON + i, CONTEXT_BUTTON_LIGHT + i, surface,
                    midiProcessor));
        }
        for (int i = 0; i < 9; i++) {
            final int encoderTouchId = i < 8 ? ENCODER_TOUCH + i : 38;
            touchEncoders.add(new TouchEncoder(i, ENCODER_CC[i], encoderTouchId, surface, midiProcessor));
            touchSliders.add(
                new TouchSlider(i + 9, SLIDER + i, SLIDER_TOUCH + (i < 4 ? i : i + 1), surface, midiProcessor));
        }
        for (int i = 0; i < 12; i++) {
            padButtons.add(
                new RgbNoteButton("PAD_%d".formatted(i), i, PAD_BUTTON_DAW_LIGHT + i, surface, midiProcessor));
        }
        sceneStateDisplay = surface.createMultiStateHardwareLight("SceneState");
    }
    
    public RgbButton getButton(final CcAssignment assignment) {
        return buttonMap.get(assignment);
    }
    
    public RgbButton getContextButton(final int index) {
        return contextButtons.get(index);
    }
    
    public RgbButton getPadButton(final int index) {
        return padButtons.get(index);
    }
    
    public TouchEncoder getMainEncoder() {
        return mainEncoder;
    }
    
    public TouchEncoder getEncoder(final int index) {return touchEncoders.get(index);}
    
    public TouchSlider getTouchSlider(final int index) {return touchSliders.get(index);}
    
    public MultiStateHardwareLight getSceneStateDisplay() {
        return sceneStateDisplay;
    }
}
