package com.bitwig.extensions.controllers.akai.apcmk2.control;

import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.controllers.akai.apcmk2.ApcConfiguration;
import com.bitwig.extensions.controllers.akai.apcmk2.MidiProcessor;
import com.bitwig.extensions.framework.di.PostConstruct;

public class HardwareElementsApc {
    
    private final Encoder[] encoders = new Encoder[8];
    private final HardwareSlider[] sliders = new HardwareSlider[9];
    private RgbButton[][] buttons;
    private final SingleLedButton[] trackButtons = new SingleLedButton[8];
    private SingleLedButton[] sceneButtons;
    private SingleLedButton shiftButton;
    private SingleLedButton stopAllButton;
    private SingleLedButton playButton;
    private SingleLedButton recButton;
    
    public HardwareElementsApc() {
    }
    
    @PostConstruct
    public void init(ApcConfiguration configuration, HardwareSurface surface, MidiProcessor midiProcessor) {
        MidiIn midiIn = midiProcessor.getMidiIn();
        
        if(configuration.isHasEncoders()) {
            for (int i = 0; i < encoders.length; i++) {
                encoders[i] = new Encoder(0x30 + i, surface, midiIn);
            }
        } else {
            for (int i = 0; i < sliders.length; i++) {
                sliders[i] = surface.createHardwareSlider("SLIDER"+i);
                sliders[i].setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0x30+i));
            }
        }
        shiftButton = new SingleLedButton(configuration.getShiftButtonValue(), surface, midiProcessor);
        int noteNr = 0;
        final int numberOfScenes = configuration.getSceneRows();
        
        buttons = new RgbButton[numberOfScenes][8];
        sceneButtons = new SingleLedButton[numberOfScenes];
        for (int row = 0; row < numberOfScenes; row++) {
            for (int col = 0; col < 8; col++) {
                buttons[row][col] = new RgbButton(noteNr++, surface, midiProcessor);
            }
            sceneButtons[row] = new SingleLedButton(configuration.getSceneLaunchBase() + row, surface, midiProcessor);
        }
        for (int i = 0; i < 8; i++) {
            trackButtons[i] = new SingleLedButton(configuration.getTrackButtonBase() + i, surface, midiProcessor);
        }
        if(numberOfScenes < 8) {
            stopAllButton = new SingleLedButton(0x51, surface, midiProcessor);
            playButton = new SingleLedButton(0x5b, surface, midiProcessor);
            recButton = new SingleLedButton(0x5d, surface, midiProcessor);
        }
    }
    
    public SingleLedButton getShiftButton() {
        return shiftButton;
    }
    
    public SingleLedButton getStopAllButton() {
        return stopAllButton;
    }
    
    public SingleLedButton getRecButton() {
        return recButton;
    }
    
    public SingleLedButton getPlayButton() {
        return playButton;
    }
    
    public SingleLedButton getTrackButton(int index) {
        return trackButtons[index];
    }
    
    public SingleLedButton getSceneButton(int index) {
        return sceneButtons[index];
    }
    
    public Encoder getEncoder(int index) {
        return encoders[index];
    }
    
    public HardwareSlider getSlider(int index) {
        return sliders[index];
    }
    
    public RgbButton getGridButton(final int sceneIndex, final int trackIndex) {
        return buttons[buttons.length - sceneIndex - 1][trackIndex];
    }
    
    public void refreshGridButtons() {
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[row].length; col++) {
                buttons[row][col].reset();
            }
        }
    }
}
