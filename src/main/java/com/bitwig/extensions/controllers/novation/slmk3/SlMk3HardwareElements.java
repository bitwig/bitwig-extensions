package com.bitwig.extensions.controllers.novation.slmk3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.controllers.novation.slmk3.control.RgbButton;
import com.bitwig.extensions.controllers.novation.slmk3.control.SlEncoder;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;
import com.bitwig.extensions.framework.di.Component;

@Component
public class SlMk3HardwareElements {
    
    private final List<SlEncoder> encoders = new ArrayList<>();
    private final List<HardwareSlider> sliders = new ArrayList<>();
    private final List<RgbButton> padButtons = new ArrayList<>();
    private final List<RgbButton> selectButtons = new ArrayList<>();
    private final List<RgbButton> softButtons = new ArrayList<>();
    private final List<MultiStateHardwareLight> trackLights = new ArrayList<>();
    private final Map<CcAssignment, RgbButton> individualButtons = new HashMap<>();
    private final HardwareButton shiftButton;
    
    public SlMk3HardwareElements(final HardwareSurface surface, final MidiProcessor midiProcessor) {
        shiftButton = surface.createHardwareButton("SHIFT_BUTTON");
        midiProcessor.setCcMatcher(shiftButton, 0x5B);
        
        for (int i = 0; i < 8; i++) {
            encoders.add(new SlEncoder(surface, midiProcessor, i));
            final HardwareSlider slider = surface.createHardwareSlider("SLIDER_%d".formatted(i + 1));
            slider.setAdjustValueMatcher(midiProcessor.createAbsoluteHardwareMatcher(0x29 + i));
            sliders.add(slider);
            final MultiStateHardwareLight trackLight =
                surface.createMultiStateHardwareLight("TRACK_LED_%d".formatted(i + 1));
            final int lightIndex = 0x36 + i;
            trackLights.add(trackLight);
            trackLight.state().setValue(SlRgbState.OFF);
            trackLight.state().onUpdateHardware(state -> midiProcessor.updateLightState(lightIndex, state));
            
            selectButtons.add(
                new RgbButton(RgbButton.Type.CC, 0x33 + i, 0x4 + i, "SELECT_BUTTON_%d".formatted(i), surface,
                    midiProcessor));
        }
        for (int i = 0; i < 16; i++) {
            softButtons.add(new RgbButton(RgbButton.Type.CC, 0x3B + i, 0xC + i, "SOFT_BUTTON_%d".formatted(i), surface,
                midiProcessor));
        }
        for (int row = 0; row < 2; row++) {
            for (int i = 0; i < 8; i++) {
                final RgbButton padButton = new RgbButton(RgbButton.Type.NOTE, 0x60 + row * 16 + i, 0x26 + row * 8 + i,
                    "PAD_%d_%d".formatted(i + 1, row + 1), surface, midiProcessor);
                padButtons.add(padButton);
            }
        }
        Arrays.stream(CcAssignment.values()).forEach(ccAssignment -> individualButtons.put(ccAssignment,
            new RgbButton(RgbButton.Type.CC, ccAssignment.getMidiId(), ccAssignment.getLedIndex(),
                ccAssignment.toString(), surface, midiProcessor)));
    }
    
    public List<SlEncoder> getEncoders() {
        return encoders;
    }
    
    public List<HardwareSlider> getSliders() {
        return sliders;
    }
    
    public List<RgbButton> getPadButtons() {
        return padButtons;
    }
    
    public List<MultiStateHardwareLight> getTrackLights() {
        return trackLights;
    }
    
    public List<RgbButton> getSelectButtons() {
        return selectButtons;
    }
    
    public List<RgbButton> getSoftButtons() {
        return softButtons;
    }
    
    public RgbButton getButton(final CcAssignment assignment) {
        return individualButtons.get(assignment);
    }
    
    public HardwareButton getShiftButton() {
        return shiftButton;
    }
}
