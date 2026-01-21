package com.bitwig.extensions.controllers.allenheath.xonek3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.controllers.allenheath.xonek3.control.XoneAssignButton;
import com.bitwig.extensions.controllers.allenheath.xonek3.control.XoneEncoder;
import com.bitwig.extensions.controllers.allenheath.xonek3.control.XoneRgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.MidiStatus;

public class DeviceHwElements {
    private static final DoubleConsumer EMPTY_CONSUMER = v -> {};
    
    private final XoneEncoder layerEncoder;
    private final XoneEncoder shiftEncoder;
    private final XoneRgbButton shiftButton;
    private final XoneRgbButton layerButton;
    public static final int BASE_CHANNEL = 0xE;
    private final List<XoneRgbButton> gridButtons = new ArrayList<>();
    private final List<XoneRgbButton> knobButtons = new ArrayList<>();
    private final List<AbsoluteHardwareControl> sliders = new ArrayList<>();
    private final List<AbsoluteHardwareControl> knobs = new ArrayList<>();
    private final List<XoneEncoder> encoders = new ArrayList<>();
    
    private static final int[] BASE_TOP_ENCODER_CC = {0x16, 0x2C};
    private static final int[] BASE_BOTTOM_ENCODER_CC = {0x2A, 0x44};
    private static final int[] LAYER_BUTTON_BASE = {0x3C, 0x60}; // TOP is + 0x16
    private static final int[] ENCODER_BUTTON_BASE = {0x58, 0x7C}; // TOP is + 0x16
    
    private static final int[] ENCODER_BUTTONS_INDEX = {
        0x30, 0x31, 0x32, 0x33, //
        0x2C, 0x2D, 0x2E, 0x2F, //
        0x28, 0x29, 0x2A, 0x2B, //
    };
    
    private static final int[] GRID_BUTTONS_INDEX = {
        0x24, 0x25, 0x26, 0x27, //
        0x20, 0x21, 0x22, 0x23, //
        0x1C, 0x1D, 0x1E, 0x1F, //
        0x18, 0x19, 0x1A, 0x1B,
    };
    
    
    public DeviceHwElements(final int deviceIndex, final HardwareSurface surface, final XoneMidiDevice midiProcessor,
        final XoneK3GlobalStates globalStates) {
        for (int i = 0; i < 16; i++) {
            final XoneRgbButton button = new XoneRgbButton(
                i, 0xC + i, "GRID%d".formatted(deviceIndex), MidiStatus.NOTE_ON, GRID_BUTTONS_INDEX[i], BASE_CHANNEL,
                surface, midiProcessor);
            gridButtons.add(button);
        }
        final MidiIn midiIn = midiProcessor.getMidiIn();
        for (int i = 0; i < 4; i++) {
            final AbsoluteHardwareKnob control =
                surface.createAbsoluteHardwareKnob("SLIDER%d-%d".formatted(deviceIndex, i + 1));
            control.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(BASE_CHANNEL, 0x10 + i));
            sliders.add(control);
            encoders.add(
                new XoneEncoder(
                    i, 0x1E + i, BASE_CHANNEL, i, 0x34, "ENCODER%d".formatted(deviceIndex), midiProcessor, surface));
        }
        for (int i = 0; i < 12; i++) {
            final AbsoluteHardwareKnob control =
                surface.createAbsoluteHardwareKnob("KNOB%d %d-%d".formatted(deviceIndex, (i / 4) + 1, (i % 4) + 1));
            control.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(BASE_CHANNEL, 0x4 + i));
            knobs.add(control);
            
            final XoneRgbButton button =
                new XoneRgbButton(
                    i, i, "KNOB_BUTTON%d".formatted(deviceIndex), MidiStatus.NOTE_ON, ENCODER_BUTTONS_INDEX[i],
                    BASE_CHANNEL, surface, midiProcessor);
            knobButtons.add(button);
        }
        this.layerEncoder =
            new XoneEncoder(
                0, -1, BASE_CHANNEL, 0x14, 0x0D, "LAYER ENCODER%d".formatted(deviceIndex), midiProcessor, surface);
        this.layerButton = globalStates.usesLayers()
            ? null
            : new XoneRgbButton(
                0, 0x1C, "LAYER%d".formatted(deviceIndex), MidiStatus.NOTE_ON, 0xC, BASE_CHANNEL,
                surface, midiProcessor);
        this.shiftEncoder =
            new XoneEncoder(
                0, -1, BASE_CHANNEL, 0x15, 0x0E, "SHIFT ENCODER%d".formatted(deviceIndex), midiProcessor,
                surface);
        this.shiftButton =
            new XoneRgbButton(
                0, 0x1D, "SHIFT%d".formatted(deviceIndex), MidiStatus.NOTE_ON, 0xF, BASE_CHANNEL, surface,
                midiProcessor);
        
        if (globalStates.usesLayers()) {
            setupFreeAssignmentControls(deviceIndex, surface, midiProcessor, midiIn);
        }
    }
    
    private void setupFreeAssignmentControls(final int deviceIndex, final HardwareSurface surface,
        final XoneMidiDevice midiProcessor, final MidiIn midiIn) {
        final List<XoneAssignButton> assignButtons = new ArrayList<>();
        for (int layer = 0; layer < 2; layer++) {
            final List<RelativeHardwareKnob> assignEncoders = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                final String name = "ENCODER%d Layer %d - %d".formatted(deviceIndex, layer + 2, i + 1);
                assignEncoders.add(createExtControlKnob(surface, midiIn, name, BASE_TOP_ENCODER_CC[layer] + i));
                assignButtons.add(
                    new XoneAssignButton(midiProcessor, surface, layer, 0x1E + i, ENCODER_BUTTON_BASE[layer] + i));
            }
            assignEncoders.add(
                createExtControlKnob(
                    surface, midiIn, "ENCODER%d Layer %d LAYER".formatted(deviceIndex, layer + 2),
                    BASE_BOTTOM_ENCODER_CC[layer]));
            assignEncoders.add(
                createExtControlKnob(
                    surface, midiIn, "ENCODER%d Layer %d SCROLL".formatted(deviceIndex, layer + 2),
                    BASE_BOTTOM_ENCODER_CC[layer] + 1));
            
            //XoneK3ControllerExtension.println("-----top %d-------", layer);
            for (int i = 0; i < 12; i++) {
                final int ledIndex = (2 - (i / 4)) * 4 + (i % 4);
                assignButtons.add(
                    new XoneAssignButton(midiProcessor, surface, layer, ledIndex, LAYER_BUTTON_BASE[layer] + i + 0x10));
            }
            //XoneK3ControllerExtension.println("------bottom %d ------", layer);
            for (int i = 0; i < 16; i++) {
                final int ledIndex = (3 - (i / 4)) * 4 + (i % 4) + 12;
                assignButtons.add(
                    new XoneAssignButton(midiProcessor, surface, layer, ledIndex, LAYER_BUTTON_BASE[layer] + i));
            }
        }
        midiProcessor.setAssignButtons(assignButtons);
    }
    
    public void disableKnobButtonSection(final Layer layer) {
        getKnobButtons().forEach(button -> button.bindDisabled(layer));
        getKnobs().forEach(knob -> layer.bind(knob, EMPTY_CONSUMER));
    }
    
    public XoneRgbButton getLayerButton() {
        return layerButton;
    }
    
    public List<XoneRgbButton> getGridButtons() {
        return gridButtons;
    }
    
    public List<AbsoluteHardwareControl> getSliders() {
        return sliders;
    }
    
    public List<AbsoluteHardwareControl> getKnobs() {
        return knobs;
    }
    
    public List<XoneRgbButton> getKnobButtons() {
        return knobButtons;
    }
    
    public List<XoneEncoder> getEncoders() {
        return encoders;
    }
    
    public XoneEncoder getShiftEncoder() {
        return shiftEncoder;
    }
    
    public XoneEncoder getLayerEncoder() {
        return layerEncoder;
    }
    
    public XoneRgbButton getShiftButton() {
        return shiftButton;
    }
    
    private RelativeHardwareKnob createExtControlKnob(final HardwareSurface surface, final MidiIn midiIn,
        final String name, final int midiCcBase) {
        final RelativeHardwareKnob hwEncoder = surface.createRelativeHardwareKnob(name);
        hwEncoder.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xE, midiCcBase, 40));
        hwEncoder.setStepSize(0.025);
        return hwEncoder;
    }
    
}
