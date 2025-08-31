package com.bitwig.extensions.controllers.neuzeitinstruments;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extensions.controllers.neuzeitinstruments.controls.DropButton;
import com.bitwig.extensions.controllers.neuzeitinstruments.controls.DropColorButton;
import com.bitwig.extensions.controllers.neuzeitinstruments.controls.DropRemoteMapControl;
import com.bitwig.extensions.controllers.neuzeitinstruments.controls.MapButton;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.MidiStatus;

@Component
public class HwElements {
    
    public static final String PREF_BUTTON = "Buttons";
    public static final String PREF_CONTROLLER = "Controller";
    private final List<DropColorButton> layer1Buttons = new ArrayList<>();
    private final List<DropColorButton> layer2Buttons = new ArrayList<>();
    private final DropButton gridLeftButton;
    private final DropButton gridRightButton;
    private final DropButton gridUpButton;
    private final DropButton gridDownButton;
    private final List<DropRemoteMapControl> mappingKnobs = new ArrayList<>();
    private final List<MapButton> mappingButtons = new ArrayList<>();
    
    private record DawControlInfo(String name, int offset, int channel, Integer jumpIndex) {
        public DawControlInfo(final String name, final int offset) {
            this(name, offset, 0, null);
        }
        
        public DawControlInfo(final String name, final int offset, final Integer jumpIndex) {
            this(name, offset, 0, jumpIndex);
        }
        
        public int getValue(final int index) {
            if (jumpIndex != null) {
                final int pos = jumpIndex & 0xF;
                final int amount = jumpIndex >> 4;
                if (index >= pos) {
                    return offset + index + amount;
                }
            }
            return offset + index;
        }
    }
    
    private static final List<DawControlInfo> DAW_KNOBS = List.of(
        new DawControlInfo("FADER A", 0x2C), //
        new DawControlInfo("ROT A-1", 0x01, 0x15), //
        new DawControlInfo("ROT A-2", 0x0A), //
        new DawControlInfo("ROT A-3", 0x12), //
        new DawControlInfo("ROT A-4", 0x1A, 0x16), //
        new DawControlInfo("FADER B", 0x5C, 0x25), // +2
        new DawControlInfo("ROT B-1", 0x34), //
        new DawControlInfo("ROT B-2", 0x3c), //
        new DawControlInfo("ROT B-3", 0x44), //
        new DawControlInfo("ROT B-4", 0x4C));
    
    private static final List<DawControlInfo> DAW_PUSH_ENCODERS = List.of(
        new DawControlInfo("PUSH A-1", 0x01, 0x15), //
        new DawControlInfo("PUSH A-2", 0x0A), //
        new DawControlInfo("PUSH A-3", 0x12), //
        new DawControlInfo("PUSH A-4", 0x1A, 0x16), //
        new DawControlInfo("PUSH B-1", 0x34), //
        new DawControlInfo("PUSH B-2", 0x3c), //
        new DawControlInfo("PUSH B-3", 0x44), //
        new DawControlInfo("PUSH B-4", 0x4C));
    
    private static final List<DawControlInfo> DAW_BUTTONS = List.of(
        new DawControlInfo("BTN A", 0x23, 0x13), //
        new DawControlInfo("BTN B", 0x54) //
    );
    
    public HwElements(final HardwareSurface surface, final DropMidiProcessor midiProcessor, final ControllerHost host) {
        for (int i = 0; i < 20; i++) {
            layer1Buttons.add(new DropColorButton(i, 88 + i, "L1BUTTON", surface, midiProcessor));
            layer2Buttons.add(new DropColorButton(i, 108 + i, "L2BUTTON", surface, midiProcessor));
        }
        
        this.gridLeftButton = new DropButton(0, 84, "NAV_LEFT", surface, midiProcessor);
        this.gridRightButton = new DropButton(0, 87, "NAV_RIGHT", surface, midiProcessor);
        this.gridUpButton = new DropButton(0, 85, "NAV_UP", surface, midiProcessor);
        this.gridDownButton = new DropButton(0, 86, "NAV_DOWN", surface, midiProcessor);
        
        for (final DawControlInfo knobInfo : DAW_KNOBS) {
            for (int i = 0; i < 8; i++) {
                mappingKnobs.add(
                    new DropRemoteMapControl(i, 0, knobInfo.getValue(i), knobInfo.name(), surface, midiProcessor));
            }
        }
        for (final DawControlInfo knobInfo : DAW_PUSH_ENCODERS) {
            for (int i = 0; i < 8; i++) {
                mappingButtons.add(
                    new MapButton(
                        i, MidiStatus.NOTE_ON, 1, knobInfo.getValue(i), knobInfo.name(), surface,
                        midiProcessor));
                mappingKnobs.add(new DropRemoteMapControl(
                    i, 1, knobInfo.getValue(i), knobInfo.name() + " CC", surface,
                    midiProcessor));
            }
        }
        for (final DawControlInfo knobInfo : DAW_BUTTONS) {
            for (int i = 0; i < 8; i++) {
                mappingButtons.add(
                    new MapButton(
                        i, MidiStatus.NOTE_ON, 1, knobInfo.getValue(i), knobInfo.name(), surface,
                        midiProcessor));
            }
        }
    }
    
    public List<DropColorButton> getLayer1Buttons() {
        return layer1Buttons;
    }
    
    public List<DropColorButton> getLayer2Buttons() {
        return layer2Buttons;
    }
    
    public DropButton getGridLeftButton() {
        return gridLeftButton;
    }
    
    public DropButton getGridRightButton() {
        return gridRightButton;
    }
    
    public DropButton getGridUpButton() {
        return gridUpButton;
    }
    
    public DropButton getGridDownButton() {
        return gridDownButton;
    }
    
    public void fullButtonUpdate() {
        layer1Buttons.forEach(DropColorButton::forceLightUpdate);
        layer2Buttons.forEach(DropColorButton::forceLightUpdate);
    }
}
