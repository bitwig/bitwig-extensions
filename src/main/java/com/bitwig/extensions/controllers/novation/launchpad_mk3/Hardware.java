package com.bitwig.extensions.controllers.novation.launchpad_mk3;

import java.util.function.Consumer;

import com.bitwig.extension.api.Host;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;

public class Hardware {

    protected HardwareSurface mHardwareSurface;
    protected ControllerHost mHost;
    protected MidiIn mMidiIn0;
    protected MidiIn mMidiIn1;
    protected MidiOut mMidiOut;

    // Hardware Proxy
    protected HardwareButton[][] mButtons = new HardwareButton[8][8];
    protected MultiStateHardwareLight[][] mPadLights = new MultiStateHardwareLight[8][8];
    protected HardwareButton[] mRightButtons = new HardwareButton[8];
    protected MultiStateHardwareLight[] mRightLights = new MultiStateHardwareLight[8];
    protected HardwareButton mUpButton;
    protected MultiStateHardwareLight mUpLight;
    protected HardwareButton mDownButton;
    protected MultiStateHardwareLight mDownLight;
    protected HardwareButton mLeftButton;
    protected MultiStateHardwareLight mLeftLight;
    protected HardwareButton mRightButton;
    protected MultiStateHardwareLight mRightLight;
    protected HardwareButton mSessionButton;
    protected HardwareButton mNoteButton;
    protected HardwareButton mCustomButton;
    protected HardwareButton mRecButton;
    protected MultiStateHardwareLight mRecLight;
    protected HardwareSlider[] mFader = new HardwareSlider[32];

    public Hardware(ControllerExtension driver) {
        initHardware(driver);
    }

    private void initHardware(ControllerExtension driver) {
        mHost = driver.getHost();
        mHardwareSurface = mHost.createHardwareSurface();

        mMidiIn0 = mHost.getMidiInPort(0);
        mMidiIn1 = mHost.getMidiInPort(1);
        mMidiOut = mHost.getMidiOutPort(0);

        initHardwareControlls();
    }

    private void initHardwareControlls() {
        // MODE BUTTON
        mSessionButton = createCCButton("session", 95);
        mNoteButton = createCCButton("note", 96);
        mCustomButton = createCCButton("custom", 97);
        // TRANSPORT BUTTONS
        mRecButton = createCCButton("record", 98);
        mRecLight = createLight("rec_light", 98);
        mRecButton.setBackgroundLight(mRecLight);
        // NAVIGATION BUTTONS
        mUpButton = createCCButton("up", 91);
        mDownButton = createCCButton("down", 92);
        mLeftButton = createCCButton("left", 93);
        mRightButton = createCCButton("right", 94);
        mLeftLight = createLight("left_arrow_led", 93);
        mRightLight = createLight("right_arrow_led", 94);
        mUpLight = createLight("up_light", 91);
        mDownLight = createLight("down_light", 92);
        mUpButton.setBackgroundLight(mUpLight);
        mDownButton.setBackgroundLight(mDownLight);
        mLeftButton.setBackgroundLight(mLeftLight);
        mRightButton.setBackgroundLight(mRightLight);
        // SCENE BUTTONS
        for (int i = 0; i < 8; i++) {
            int midi = (i + 1) * 10 + 9;
            String name = "scene" + (7 - i);
            mRightButtons[i] = createCCButton(name, midi);
            mRightLights[i] = createLight("led_" + name, midi);
            mRightButtons[i].setBackgroundLight(mRightLights[i]);
        }
        // BUTTON MATRIX
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                int midi = (1 + i) + (j + 1) * 10;
                String name = "" + (i * 10 + (7 - j));
                mButtons[i][j] = createNoteButton(name, midi);
                mPadLights[i][j] = createLight("led_" + name, midi);
                mButtons[i][j].setBackgroundLight(mPadLights[i][j]);
            }
        }
        // FADER
        for (int i = 0; i < 32; i++) {
            mFader[i] = mHardwareSurface.createHardwareSlider("fader" + i);
            mFader[i].setAdjustValueMatcher(mMidiIn0.createAbsoluteCCValueMatcher(4, i));
        }
    }


    private HardwareButton createCCButton(String name, int midi) {
        final HardwareButton button = mHardwareSurface.createHardwareButton(name);
        button.pressedAction().setActionMatcher(mMidiIn0.createCCActionMatcher(0, midi, 127));
        button.releasedAction().setActionMatcher(mMidiIn0.createCCActionMatcher(0, midi, 0));
        button.isPressed().markInterested();

        return button;
    };

    private HardwareButton createNoteButton(String name, int midi) {
        final HardwareButton button = mHardwareSurface.createHardwareButton(name);
        button.pressedAction().setActionMatcher(mMidiIn0.createNoteOnActionMatcher(0, midi));
        button.releasedAction().setActionMatcher(mMidiIn0.createNoteOffActionMatcher(0, midi));
        button.isPressed().markInterested();

        return button;
    };

    private MultiStateHardwareLight createLight(String name, int midi) {
        final MultiStateHardwareLight light = mHardwareSurface.createMultiStateHardwareLight(name);
        light.state().onUpdateHardware(new Consumer<RGBState>() {
            @Override
            public void accept(RGBState state) {
                if (state != null)
                    RGBState.sendSys(mMidiOut, midi, state); // TO Do: Try!
            }
        });
        light.setColorToStateFunction(color -> new RGBState(color));

        return light;
    }

    
}
