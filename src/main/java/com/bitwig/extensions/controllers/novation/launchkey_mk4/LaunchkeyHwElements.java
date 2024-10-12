package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.ButtonMidiType;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.LaunchkeyButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.MonoButton;
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
    private final RgbButton trackModeButton;
    private final LaunchkeyButton shiftButton;
    private final MonoButton playButton;
    private final static int[] DRUM_MIDI_MAPPING = {
        0x28, 0x29, 0x2A, 0x2B, 0x30, 0x31, 0x32, 0x33, 0x24, 0x25, 0x26, 0x27, 0x2C, 0x2D, 0x2E, 0x2F
    };
    private final MonoButton stopButton;
    private final MonoButton loopButton;
    private final MonoButton recButton;
    private final MonoButton metroButton;
    private final MonoButton captureButton;
    private final MonoButton quantizeButton;
    private final MonoButton undoButton;
    
    private final MonoButton trackLeftButton;
    private final MonoButton trackRightButton;
    private final MonoButton navUpButton;
    private final MonoButton navDownButton;
    private final RgbButton sceneLaunchButton;
    private final RgbButton launchModeButton;
    private final RgbButton paramUpButton;
    private final RgbButton paramDownButton;
    
    
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
        }
        masterSlider = surface.createHardwareSlider("MASTER_SLIDER");
        masterSlider.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0xF, 13));
        trackModeButton = new RgbButton(ButtonMidiType.CC, 0x2D, "TRACK_MODE", surface, midiProcessor);
        playButton = new MonoButton(0x73, "PLAY", surface, midiProcessor);
        stopButton = new MonoButton(0x74, "STOP", surface, midiProcessor);
        loopButton = new MonoButton(0x76, "LOOP", surface, midiProcessor);
        recButton = new MonoButton(0x75, "RECORD", surface, midiProcessor);
        metroButton = new MonoButton(0x4C, "METRO", surface, midiProcessor);
        captureButton = new MonoButton(0x4A, "CAPTURE", surface, midiProcessor);
        undoButton = new MonoButton(0x4D, "CAPTURE", surface, midiProcessor);
        quantizeButton = new MonoButton(0x4B, "CAPTURE", surface, midiProcessor);
        trackLeftButton = new MonoButton(0x67, "TR_LEFT", surface, midiProcessor);
        trackRightButton = new MonoButton(0x66, "TR_RIGHT", surface, midiProcessor);
        navUpButton = new MonoButton(0x6A, "NAV_UP", surface, midiProcessor);
        navDownButton = new MonoButton(0x6B, "NAV_DOWN", surface, midiProcessor);
        sceneLaunchButton = new RgbButton(ButtonMidiType.CC, 0x68, "SCENE", surface, midiProcessor);
        launchModeButton = new RgbButton(ButtonMidiType.CC, 0x69, "MODE", surface, midiProcessor);
        paramUpButton = new RgbButton(ButtonMidiType.CC, 0x33, "PARAM_UP", surface, midiProcessor);
        paramDownButton = new RgbButton(ButtonMidiType.CC, 0x34, "PARAM_DOWN", surface, midiProcessor);
    }
    
    public MonoButton getPlayButton() {
        return playButton;
    }
    
    public LaunchkeyButton getShiftButton() {
        return shiftButton;
    }
    
    public MonoButton getStopButton() {
        return stopButton;
    }
    
    public MonoButton getLoopButton() {
        return loopButton;
    }
    
    public RgbButton getSceneLaunchButton() {
        return sceneLaunchButton;
    }
    
    public RgbButton getParamDownButton() {
        return paramDownButton;
    }
    
    public RgbButton getParamUpButton() {
        return paramUpButton;
    }
    
    public RgbButton getLaunchModeButton() {
        return launchModeButton;
    }
    
    public MonoButton getRecButton() {
        return recButton;
    }
    
    public MonoButton getTrackLeftButton() {
        return trackLeftButton;
    }
    
    public MonoButton getTrackRightButton() {
        return trackRightButton;
    }
    
    public MonoButton getNavUpButton() {
        return navUpButton;
    }
    
    public MonoButton getNavDownButton() {
        return navDownButton;
    }
    
    public MonoButton getCaptureButton() {
        return captureButton;
    }
    
    public MonoButton getUndoButton() {
        return undoButton;
    }
    
    public MonoButton getQuantizeButton() {
        return quantizeButton;
    }
    
    public MonoButton getMetroButton() {
        return metroButton;
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
    
    public RgbButton getTrackModeButton() {
        return trackModeButton;
    }
    
    public RelAbsEncoder[] getValueEncoders() {
        return valueEncoders;
    }
}
