package com.bitwig.extensions.controllers.akai.apc64;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.controllers.akai.apc.common.control.ClickEncoder;
import com.bitwig.extensions.controllers.akai.apc.common.control.RgbButton;
import com.bitwig.extensions.controllers.akai.apc64.control.OledBacklight;
import com.bitwig.extensions.controllers.akai.apc64.control.SingleLedButton;
import com.bitwig.extensions.controllers.akai.apc64.control.TouchSlider;
import com.bitwig.extensions.framework.di.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class HardwareElements {
    private RgbButton[][] buttons;
    private RgbButton[][] drumButtons;
    private SingleLedButton[] sceneButtons;
    private TouchSlider[] sliders = new TouchSlider[8];
    private final RgbButton[] trackButtons = new RgbButton[8];
    private final RgbButton[] trackControlButtons = new RgbButton[8];
    private final ClickEncoder mainEncoder;
    private final SingleLedButton encoderPress;
    private final Map<Apc64CcAssignments, SingleLedButton> mainButtons;
    private final OledBacklight oledBackLight;

    public HardwareElements(ControllerHost host, HardwareSurface surface, Apc64MidiProcessor midiProcessor) {
        MidiIn midiIn = midiProcessor.getMidiIn();
        final int numberOfScenes = 8;
        drumButtons = new RgbButton[numberOfScenes][8];
        int noteNr = Apc64CcAssignments.GRID_BASE.getStateId();
        buttons = new RgbButton[numberOfScenes][8];
        sceneButtons = new SingleLedButton[numberOfScenes];
        for (int row = 0; row < numberOfScenes; row++) {
            for (int col = 0; col < 8; col++) {
                buttons[row][col] = new RgbButton(6, noteNr++, "PAD", surface, midiProcessor);
            }
            sceneButtons[row] = new SingleLedButton(Apc64CcAssignments.SCENE_BUTTON_BASE.getStateId() + row, "SCENE",
                    surface, midiProcessor);
        }
        mainEncoder = new ClickEncoder(0x5A, host, surface, midiIn);
        encoderPress = new SingleLedButton(0x5A, "ENCODER_PRESS", surface, midiProcessor);
        oledBackLight = new OledBacklight(surface, midiProcessor, 0x59);

        mainButtons = Arrays.stream(Apc64CcAssignments.values()) //
                .filter(Apc64CcAssignments::isSingle) //
                .collect(Collectors.toMap(assignment -> assignment,//
                        assignment -> new SingleLedButton(assignment.getStateId(), assignment.toString(), surface,
                                midiProcessor)));

        for (int i = 0; i < 8; i++) {
            sliders[i] = new TouchSlider(i, surface, midiProcessor);
            trackButtons[i] = new RgbButton(0, Apc64CcAssignments.TRACKS_BASE.getStateId() + i, "TRACK_SEL", surface,
                    midiProcessor);
            trackControlButtons[i] = new RgbButton(0, Apc64CcAssignments.TRACK_CONTROL_BASE.getStateId() + i,
                    "TRACK_CTL", surface, midiProcessor);
        }
    }

    public void invokeRefresh() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                buttons[i][j].refresh();
            }
        }
    }

    public SingleLedButton getSceneButton(int index) {
        return sceneButtons[index];
    }

    public OledBacklight getOledBackLight() {
        return oledBackLight;
    }

    public SingleLedButton getButton(Apc64CcAssignments assignment) {
        return mainButtons.get(assignment);
    }

    public ClickEncoder getMainEncoder() {
        return mainEncoder;
    }

    public SingleLedButton getEncoderPress() {
        return encoderPress;
    }

    public RgbButton getTrackSelectButton(int index) {
        return trackButtons[index];
    }

    public RgbButton getTrackControlButtons(int index) {
        return trackControlButtons[index];
    }

    public RgbButton getGridButton(final int sceneIndex, final int trackIndex) {
        return buttons[buttons.length - sceneIndex - 1][trackIndex];
    }

    public RgbButton getDrumButton(final int sceneIndex, final int trackIndex) {
        return drumButtons[buttons.length - sceneIndex - 1][trackIndex];
    }

    public TouchSlider[] getTouchSliders() {
        return sliders;
    }
}
