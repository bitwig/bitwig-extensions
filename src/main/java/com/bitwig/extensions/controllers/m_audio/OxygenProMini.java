package com.bitwig.extensions.controllers.m_audio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Consumer;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.util.NoteInputUtils;

public class OxygenProMini extends ControllerExtension {

    // final static int CC_VALUES
    final static int CC_PAD_CHANNEL = 0; // 0-3 Banks ~ Channels
    final static int CC_PAD_START = 0x24;
    final static int PAD_NOTE_OFFSET = 36;

    final static int CC_ARROW_1 = 0x77;
    final static int CC_ARROW_2 = 0x78;

    final static int CC_LAYER_CHANNEL = 15;
    final static int CC_LAYER_VOLUME = 0x53; // Channel 15
    final static int CC_LAYER_PAN = 0x55;
    final static int CC_LAYER_DEVICE = 0x56;
    final static int CC_LAYER_SENDS = 0x57;
    final static int CC_LAYER_CLIP = 0x3C;

    final static int CC_HOTPAD_SAVE = 0x38; // Channel 0
    final static int CC_HOTPAD_QUANTIZE = 0x39;
    final static int CC_HOTPAD_VIEW = 0x3a;
    final static int CC_HOTPAD_UNDO = 0x3b;

    final static int CC_PLAY = 0x75; // Channel 0
    final static int CC_STOP = 0x74;
    final static int CC_RECORD = 0x76;
    final static int CC_LOOP = 0x73;
    final static int CC_FORWARD = 0x72;
    final static int CC_REWIND = 0x71;
    final static int CC_BANK_PREV = 0x6e;
    final static int CC_BANK_NEXT = 0x6f;
    final static int CC_METRONOME = 0x70;

    final static int CC_ENCODER_PESS = 0x64; // Channel 0
    final static int CC_ENCODER = 0x63;
    final static int CC_ENCODER_LEFT = 63; // value
    final static int CC_ENCODER_RIGHT = 65; // value

    final static int CC_BACK_BUTTON = 0x3d;

    final static int KNOB_CHANNEL = 15;
    final static int CC_KNOB_START = 0x11; // Channel 15

    final static int CC_FADER = 0x21; // Channel 0

    final static int OFF = 0;
    final static int BLINK = 64;
    final static int WHITE = 63;
    final static int CHARTREUSE = 14;
    final static int GREEN = 12;
    final static int AQUA = 60;
    final static int CYAN = 56;
    final static int AZURE = 44;
    final static int BLUE = 48;
    final static int VIOLET = 50;
    final static int MAGENTA = 51;
    final static int ROSE = 35;
    final static int RED = 3;
    final static int ORANGE = 11;
    final static int YELLOW = 15;

    protected OxygenProMini(ControllerExtensionDefinition definition, ControllerHost host) {
        super(definition, host);
    }

    @Override
    public void init() {
        final ControllerHost host = getHost();
        mApplication = host.createApplication();
        mHardwareSurface = host.createHardwareSurface();

        // for Keyboard input
        mMidiIn1 = host.getMidiInPort(0);

        // for Editor Output (sending DAW Preset)
        mMidiOut1 = host.getMidiOutPort(0);

        // for Controller in- and output
        mMidiIn2 = host.getMidiInPort(1);
        mMidiOut2 = host.getMidiOutPort(1);

        //host.scheduleTask(this::initAfterDelay, 35);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        initAfterDelay();
       
        final NoteInput keyboardInput = mMidiIn1.createNoteInput("Keys", "80????", "90????", "D?????");
        keyboardInput.setShouldConsumeEvents(true);

        initNoteTable();
        mPadInput = mMidiIn2.createNoteInput("Pads", "80????", "90????");
        mPadInput.setShouldConsumeEvents(true);
        mPadInput.setKeyTranslationTable(noteTable);

        mTransport = host.createTransport();
        mTransport.isPlaying().markInterested();
        mTransport.isArrangerLoopEnabled().markInterested();
        mTransport.isArrangerRecordEnabled().markInterested();
        mTransport.isMetronomeEnabled().markInterested();

        mMasterTrack = host.createMasterTrack(8);
        mMasterTrack.volume().markInterested();

        mTrackBank = host.createTrackBank(8, 8, 2);    
        mTrackBank.canScrollBackwards().markInterested();
        mTrackBank.canScrollForwards().markInterested();
        
        mSceneBank = mTrackBank.sceneBank();
        mSceneBank.canScrollBackwards().markInterested();
        mSceneBank.canScrollForwards().markInterested();


        for (int j = 0; j < 2; ++j) {
            final Scene scene = mSceneBank.getScene(j);
            scene.exists().markInterested();
        }
        for (int i = 0; i < 4; i++) {
            final Track track = mTrackBank.getItemAt(i);
            track.isQueuedForStop().markInterested();
            final ClipLauncherSlotBank clipBank = track.clipLauncherSlotBank();
            clipBank.setIndication(false);

            for (int j = 1; j >= 0; j--) {
                ClipLauncherSlot clip = clipBank.getItemAt(j);
                clip.isPlaying().markInterested();
                clip.isRecording().markInterested();
                clip.isPlaybackQueued().markInterested();
                clip.isRecordingQueued().markInterested();
                clip.isStopQueued().markInterested();
                clip.hasContent().markInterested();

                if (i < 4)
                    mClipSlot[i + 4 * (1 - j)] = clip;
                else
                    mClipSlot[(4 + i) + 4 * (1 - j)] = clip;
            }
        }

        mCursorTrack = host.createCursorTrack(8, 0);
        mCursorTrack.arm().markInterested();
        mCursorTrack.volume().markInterested();
        mCursorTrack.pan().markInterested();
        mCursorTrack.volume().setIndication(true);
        mCursorTrack.playingNotes().addValueObserver(notes -> mPlayingNotes = notes);
        mCursorTrack.trackType().markInterested();
        mCursorTrack.sendBank().exists().markInterested();
        mCursorTrack.color().markInterested();
        mCursorTrack.position().markInterested();
        
        mTrackBank.followCursorTrack(mCursorTrack);

        mCursorDevice = mCursorTrack.createCursorDevice("01", "track", 8,
                CursorDeviceFollowMode.FIRST_INSTRUMENT_OR_DEVICE);

        mCursorRemoteControls = mCursorDevice.createCursorRemoteControlsPage(8);
        mCursorRemoteControls.setHardwareLayout(HardwareControlType.KNOB, 8);
        for (int i = 0; i < 8; i++)
            mCursorRemoteControls.getParameter(i);

        mCursorClip = host.createLauncherCursorClip(0, 0);

        CursorDevice mInstrument = mCursorTrack.createCursorDevice("02", "track", 8,
                CursorDeviceFollowMode.FIRST_INSTRUMENT);


        mDrumPadBank = mInstrument.createDrumPadBank(16);
        mDrumPadBank.exists().markInterested();
        for (int i = 0; i < 16; i++) {
            mDrumPadBank.getItemAt(i).color().markInterested();
            mDrumPadBank.getItemAt(i).exists().markInterested();
            mDrumPadBank.getItemAt(i).isMutedBySolo().markInterested();
        }

        createHardwareSurface();
        initLayers();

        getHost().showPopupNotification("M-Audio Oxygen Pro 25 Initialized");
    }

    private void initAfterDelay() {
        mMidiOut1.sendSysex("F0 00 01 05 7F 00 38 6A 00 01 01 F7"); // init
        try {
            mMidiOut1.sendSysex(Files.readAllBytes(Paths.get(
                    "/Users/Elias/Documents/GitHub/bitwig-extensions/src/main/java/com/bitwig/extensions/controllers/m_audio/sysex2.syx"))); // Send DAW Preset                                                                                                                            // Preset
        } catch (IOException e) {
            getHost().println(String.valueOf(e));
        }
        mMidiOut2.sendSysex("F0 00 01 05 7F 00 00 6D 00 01 0B F7"); // Changing to the Bitwig DAW-Program
        // mMidiOut2.sendSysex("F0 00 01 05 7F 00 00 6E 00 01 0B F7");
        // mMidiOut2.sendSysex("F0 00 01 05 7F 00 00 6E 00 01 07 F7");
        mMidiOut2.sendSysex("F0 00 01 05 7F 00 00 6B 00 01 01 F7");
        mMidiOut2.sendSysex("F0 00 01 05 7F 00 00 6C 00 01 03 F7"); // activate Light Controll
    }

    private void initLayers() {
        mMainLayer = new Layer(mLayers, "Main");
        mDeviceLayer = new Layer(mLayers, "Device");
        mVolumeLayer = new Layer(mLayers, "Volume");
        mPanLayer = new Layer(mLayers, "Pan");
        mSendsLayer = new Layer(mLayers, "Sends");
        mClipLayer = new Layer(mLayers, "Clip");

        initVolumeLayer();
        initClipLayer();
        initDeviceLayer();
        initMainLayer();
        initPanLayer();
        initSendsLayer();
    }

    private void initClipLayer() {
        for (int i = 0; i < 8; i++) {
            final HardwareButton button = mPadButtons[i];
            final MultiStateHardwareLight led = mPadLights[i];
            final int index = i;

            mClipLayer.bindPressed(button, () -> {
                if (!mTrackBank.getItemAt(index < 8 ? index % 4 : index % 4 + 4).isQueuedForStop().getAsBoolean() && mClipSlot[index].isPlaying().getAsBoolean()) {
                    mTrackBank.getItemAt(index < 8 ? index % 4 : index % 4 + 4).stop();
                } 
                else {
                    mClipSlot[index].launch();
                    mClipSlot[index].select();
                }
            });
            mClipLayer.bindLightState(() -> {
                if (mClipSlot[index].isPlaying().getAsBoolean())
                    return RGBLightState.GREEN;
                else if (mClipSlot[index].isPlaybackQueued().getAsBoolean())
                    return RGBLightState.GREEN_BLINK;
                else if (mClipSlot[index].isRecording().getAsBoolean())
                    return RGBLightState.RED;
                else if (mClipSlot[index].isRecordingQueued().getAsBoolean())
                    return RGBLightState.RED_BLINK;
                else if (mClipSlot[index].isStopQueued().getAsBoolean())
                    return RGBLightState.YELLOW_BLINK;
                else if (mClipSlot[index].hasContent().get())
                    return RGBLightState.YELLOW;
                else
                    return RGBLightState.OFF;
            }, led);
        }

        for (int i = 0; i < 2; i++) {
            mClipLayer.bindPressed(mSceneButtons[i], mSceneBank.getItemAt(i).launchAction());
        }

    }

    private void initSendsLayer() {

        for (int i = 0; i < 4; i++) {
            final int index = i;
            final Parameter parameter = mCursorTrack.sendBank().getItemAt(index);
            final AbsoluteHardwareKnob knob = mKnobs[i];

            mSendsLayer.bind(knob, parameter);
        }
    }

    private void initPanLayer() {
        for (int i = 0; i < 4; i++) {
            final int index = i;
            final Parameter parameter = mTrackBank.getItemAt(index).pan();
            final AbsoluteHardwareKnob knob = mKnobs[i];

            mPanLayer.bind(knob, parameter);
        }
    }

    private void initVolumeLayer() {
        for (int i = 0; i < 4; i++) {
            final int index = i;
            final Parameter parameter = mTrackBank.getItemAt(index).volume();
            final AbsoluteHardwareKnob knob = mKnobs[i];

            mVolumeLayer.bind(knob, parameter);
        }

    }

    private void initDeviceLayer() {
        for (int i = 0; i < 4; i++) {
            final Parameter parameter = mCursorRemoteControls.getParameter(i);
            final AbsoluteHardwareKnob knob = mKnobs[i];

            mDeviceLayer.bind(knob, parameter);
        }

        mDeviceLayer.bindPressed(mEncoder, mCursorDevice.selectPreviousAction());
        mDeviceLayer.bindReleased(mEncoder, mCursorDevice.selectNextAction());
        mDeviceLayer.bindPressed(mEncoderButton, () -> {
            mClipLayer.activate();
            switch (lastLayer) {
                case 0:
                    mVolumeLayer.activate();
                    mDeviceLayer.deactivate();
                    break;
                case 1:
                    mPanLayer.activate();
                    mDeviceLayer.deactivate();
                    break;
                case 2:
                    // mVolumeLayer.activate();
                    // mDeviceLayer.deactivate();
                    break;
                case 3:
                    mSendsLayer.activate();
                    mDeviceLayer.deactivate();
                    break;
                default:
                    break;
            }
            switchPadFunction();
        });
    }

    private void initMainLayer() {
        /* Transport Button */
        

        mMainLayer.bindPressed(mStopButton, mTransport.stopAction());
        mMainLayer.bindPressed(mPlayButton, mTransport.playAction());
        mMainLayer.bindPressed(mRecordButton, mTransport.recordAction());
        mMainLayer.bindToggle(mLoopButton, mTransport.isArrangerLoopEnabled());
        mMainLayer.bindPressed(mRewindButton, mTransport.rewindAction());
        mMainLayer.bindPressed(mForwardButton, mTransport.fastForwardAction());
        mMainLayer.bindToggle(mMetronomeButton, mTransport.isMetronomeEnabled());

        mMainLayer.bindPressed(mEncoder, () -> {
            if (mBackButton.isPressed().getAsBoolean()) {
                mSceneBank.scrollBackwards();
            } else {
                mCursorTrack.selectPrevious();
            }
        });
        mMainLayer.bindReleased(mEncoder, () -> {
            if (mBackButton.isPressed().getAsBoolean()) {
                mSceneBank.scrollForwards();
            } else {
                mCursorTrack.selectNext();
            }
        });

        mMainLayer.bindPressed(mEncoderButton, () -> {
            mDeviceLayer.activate();
            mClipLayer.deactivate();
            switch (lastLayer) {
                case 0:
                    mVolumeLayer.deactivate();
                    break;
                case 1:
                    mPanLayer.deactivate();
                    break;
                case 3:
                    mSendsLayer.deactivate();
                    break;
                default:
                    break;
            }
            switchPadFunction();
        });

        mMainLayer.bindPressed(mVolumeLayerButton, () -> {
            lastLayer = 0;
            mVolumeLayer.activate();
            mPanLayer.deactivate();
            mSendsLayer.deactivate();
            mDeviceLayer.deactivate();
            mClipLayer.activate();
            switchPadFunction();
        });
        mMainLayer.bindPressed(mPanLayerButton, () -> {
            lastLayer = 1;
            mVolumeLayer.deactivate();
            mPanLayer.activate();
            mSendsLayer.deactivate();
            mDeviceLayer.deactivate();
            mClipLayer.activate();
            switchPadFunction();
        });
        mMainLayer.bindPressed(mDeviceLayerButton, () -> {
            //lastLayer = 2;
            mVolumeLayer.deactivate();
            mPanLayer.deactivate();
            mSendsLayer.deactivate();
            mDeviceLayer.activate();
            mClipLayer.deactivate();
            switchPadFunction();
        });
        mMainLayer.bindPressed(mSendsLayerButton, () -> {
            lastLayer = 3;
            mVolumeLayer.deactivate();
            mPanLayer.deactivate();
            mSendsLayer.activate();
            mDeviceLayer.deactivate();
            mClipLayer.activate();
            switchPadFunction();
        });

        mMainLayer.bindPressed(mBankNextButton, () -> {
            if (updateNoteTable(1))// && !mClipLayer.isActive())
                mDrumPadBank.scrollPageForwards();
        });
        mMainLayer.bindPressed(mBankPrevButton, () -> {
            if (updateNoteTable(0))// && !mClipLayer.isActive())
                mDrumPadBank.scrollPageBackwards();
        });

        mMainLayer.bindPressed(mSaveButton, () -> save());
        mMainLayer.bindPressed(mQuantizeButton, () -> {
            mCursorClip.quantize(1.0);
        });
        mMainLayer.bindPressed(mViewButton, () -> mApplication.previousPanelLayout());
        mMainLayer.bindPressed(mUndoButton, mApplication.undoAction());

        mMainLayer.bind(mFader, mCursorTrack.volume());

        for (int i = 0; i < 8; i++) {
            final MultiStateHardwareLight light = mPadLights[i];
            final int index = i;

            mMainLayer.bindLightState(() -> {
                return RGBstate(index);
            }, light);
        }

        mMainLayer.activate();
        mClipLayer.activate();
        mVolumeLayer.activate();
        switchPadFunction();
    }

    private void createHardwareSurface() {
        final ControllerHost host = getHost();
        final HardwareSurface surface = host.createHardwareSurface();
        mHardwareSurface = surface;

        surface.setPhysicalSize(500, 200);

        mStopButton = createButton("stop", CC_STOP);
        mStopButton.setLabel("Stop");
        mPlayButton = createButton("play", CC_PLAY);
        mPlayButton.setLabel("Play");
        mRecordButton = createButton("record", CC_RECORD);
        mRecordButton.setLabel("Rec");

        mRewindButton = createButton("rewind", CC_REWIND);
        mRewindButton.setLabel("<<");
        mForwardButton = createButton("forward", CC_FORWARD);
        mForwardButton.setLabel(">>");
        mLoopButton = createButton("loop", CC_LOOP);
        mLoopButton.setLabel("Loop");

        mBankPrevButton = createButton("prevBank", CC_BANK_PREV);
        mBankPrevButton.setLabel("Bank <-");
        mBankNextButton = createButton("nextBank", CC_BANK_NEXT);
        mBankNextButton.setLabel("Bank ->");

        mVolumeLayerButton = createButton("volumeLayer", CC_LAYER_VOLUME, CC_LAYER_CHANNEL);
        mPanLayerButton = createButton("panLayer", CC_LAYER_PAN, CC_LAYER_CHANNEL);
        mDeviceLayerButton = createButton("deviceLayer", CC_LAYER_DEVICE, CC_LAYER_CHANNEL);
        mSendsLayerButton = createButton("sendsLayer", CC_LAYER_SENDS, CC_LAYER_CHANNEL);

        mSaveButton = createButton("save", CC_HOTPAD_SAVE, false);
        mQuantizeButton = createButton("quantize", CC_HOTPAD_QUANTIZE, false);
        mViewButton = createButton("view", CC_HOTPAD_VIEW, false);
        mUndoButton = createButton("undo", CC_HOTPAD_UNDO, false);

        mMetronomeButton = createButton("metronome", CC_METRONOME);
        mMetronomeButton.setLabel("Metro");

        for (int i = 0; i < 4; i++)
            createKnob(i);

        for (int i = 0; i < 8; i++) {
            createRGBPadButton(i);
            mMidiOut2.sendMidi(0x90, CC_PAD_START + i, OFF);
        }
        mSceneButtons[0] = createButton("sceneLaunch1", CC_ARROW_1);
        mSceneButtons[1] = createButton("sceneLaunch2", CC_ARROW_2);

        mFader = mHardwareSurface.createAbsoluteHardwareKnob("fader");
        mFader.setLabel("Fader");
        mFader.setAdjustValueMatcher(mMidiIn2.createAbsoluteCCValueMatcher(0, CC_FADER));

        mBackButton = createButton("backShift", CC_BACK_BUTTON);
        mBackButton.isPressed().markInterested();

        mEncoderButton = createButton("encoderKnob", CC_ENCODER_PESS);
        mEncoder = mHardwareSurface.createHardwareButton("encoder");
        mEncoder.setLabel("Encoder");
        mEncoder.pressedAction().setActionMatcher(mMidiIn2.createCCActionMatcher(0, CC_ENCODER, CC_ENCODER_LEFT));
        mEncoder.releasedAction().setActionMatcher(mMidiIn2.createCCActionMatcher(0, CC_ENCODER, CC_ENCODER_RIGHT));

    }

    private void switchPadFunction() {
        mPadInput.setKeyTranslationTable(mClipLayer.isActive() ? NoteInputUtils.NO_NOTES : noteTable);
        for (int j = 0; j < 2; ++j) {
            final Scene scene = mSceneBank.getScene(j);
            scene.setIndication(mClipLayer.isActive() ? true : false);
        }
        for (int i = 0; i < 4; i++) {
            final Track track = mTrackBank.getItemAt(i);
            final ClipLauncherSlotBank clipBank = track.clipLauncherSlotBank();
            clipBank.setIndication(mClipLayer.isActive() ? true : false);
        }
    }

    private void createKnob(int index) {
        assert index >= 0 && index < 8;

        final AbsoluteHardwareKnob knob = mHardwareSurface.createAbsoluteHardwareKnob("knob" + index);
        knob.setLabel(String.valueOf(index + 1));
        knob.setIndexInGroup(index);
        knob.setAdjustValueMatcher(mMidiIn2.createAbsoluteCCValueMatcher(KNOB_CHANNEL, CC_KNOB_START + index));

        mKnobs[index] = knob;
    }

    private HardwareButton createButton(final String id, final int controlNumber, final int channel) {
        final HardwareButton button = mHardwareSurface.createHardwareButton(id);

        button.pressedAction().setActionMatcher(mMidiIn2.createCCActionMatcher(channel, controlNumber, 127));
        button.releasedAction().setActionMatcher(mMidiIn2.createCCActionMatcher(channel, controlNumber, 0));

        return button;
    }

    private HardwareButton createButton(final String id, final int controlNumber) {
        final HardwareButton button = mHardwareSurface.createHardwareButton(id);

        button.pressedAction().setActionMatcher(mMidiIn2.createCCActionMatcher(0, controlNumber, 127));
        button.releasedAction().setActionMatcher(mMidiIn2.createCCActionMatcher(0, controlNumber, 0));

        return button;
    }

    private HardwareButton createButton(final String id, final int controlNumber, final boolean value) {
        final HardwareButton button = mHardwareSurface.createHardwareButton(id);

        button.pressedAction().setActionMatcher(mMidiIn2.createCCActionMatcher(0, controlNumber));

        return button;
    }

    private HardwareButton createNoteButton(final String id, final int controlNumber) {
        final HardwareButton button = mHardwareSurface.createHardwareButton(id);

        button.pressedAction().setActionMatcher(mMidiIn2.createNoteOnActionMatcher(0, controlNumber));
        button.releasedAction().setActionMatcher(mMidiIn2.createNoteOffActionMatcher(0, controlNumber));

        return button;
    }

    private void createRGBPadButton(int index) {
        assert index >= 0 && index < 16;

        final HardwareButton button = createNoteButton("padButton" + index, CC_PAD_START + index);
        button.setLabel(String.valueOf(index + 1));

        mPadButtons[index] = button;

        final MultiStateHardwareLight light = mHardwareSurface.createMultiStateHardwareLight("light" + index);

        light.state().onUpdateHardware(new Consumer<RGBLightState>() {

            @Override
            public void accept(RGBLightState state) {
                if (state != null)
                    RGBLightState.send(mMidiOut2, CC_PAD_START + index, state.getMessage(), false);
            }
        });

        light.setColorToStateFunction(color -> new RGBLightState(color));

        button.setBackgroundLight(light);

        mPadLights[index] = light;
    }

    private RGBLightState RGBstate(final int index) {
        if (mPlayingNotes.length != 0) {
            RGBLightState state = RGBLightState.OFF;
            for (PlayingNote n : mPlayingNotes) {
                if (n.pitch() == 36 + index + (8 * noteBank))
                    state = RGBLightState.WHITE;
            }
            if (state == RGBLightState.OFF && mDrumPadBank.getItemAt(index).exists().getAsBoolean())
                return new RGBLightState(mDrumPadBank.getItemAt(index).color().get());
            if (state == RGBLightState.OFF && !mDrumPadBank.exists().getAsBoolean()) {
                return RGBLightState.CYAN;
            }
            return state;
        } else if (mDrumPadBank.exists().get() && mDrumPadBank.getItemAt(index).exists().getAsBoolean()) {
            return new RGBLightState(mDrumPadBank.getItemAt(index).color().get());
        } else if (!mDrumPadBank.exists().getAsBoolean() && mCursorTrack.trackType().get() == "Instrument") {
            return RGBLightState.CYAN;
        } else {
            return RGBLightState.OFF;
        }
    }

    private void initNoteTable() {
        for (int i = 0; i < 128; i++)
            noteTable[i] = i;
        noteBank = 0;
    }

    private boolean updateNoteTable(final int direction) {
        if (direction == 1 && noteBank == 3)
            return false;
        if (direction == 0 && noteBank == 0)
            return false;
        for (int i = 0; i < 8; i++) {
            if (direction == 1) {
                noteTable[i + PAD_NOTE_OFFSET] = noteTable[i + PAD_NOTE_OFFSET] + 8;
            } else {
                noteTable[i + PAD_NOTE_OFFSET] = noteTable[i + PAD_NOTE_OFFSET] - 8;
            }
            if (!mClipLayer.isActive())
                mPadInput.setKeyTranslationTable(noteTable);
        }
        if (direction == 1)
            noteBank += 1;
        else
            noteBank -= 1;
        return true;
    }

    private void save() {
        final Action saveAction = mApplication.getAction("Save");
        if (saveAction != null) {
            saveAction.invoke();
        }
    }

    @Override
    public void exit() {
        // mMidiOut2.sendSysex("F0 00 01 05 7F 00 00 6C 00 01 00 F7");
        for (int i = 0; i < 8; i++) 
            mMidiOut2.sendMidi(0x90, CC_PAD_START + i, WHITE);

        getHost().showPopupNotification("M-Audio Oxygen Pro 25 Exited");
    }

    @Override
    public void flush() {
        mHardwareSurface.updateHardware();

    }


    /* API Objects */
    private HardwareSurface mHardwareSurface;

    private Application mApplication;

    private MidiIn mMidiIn1, mMidiIn2;
    private MidiOut mMidiOut1, mMidiOut2;

    private NoteInput mPadInput;
    protected final Integer[] noteTable = new Integer[128];
    protected int noteBank = 0;
    private Transport mTransport;

    private MasterTrack mMasterTrack;

    private TrackBank mTrackBank;
    private SceneBank mSceneBank;

    private ClipLauncherSlot[] mClipSlot = new ClipLauncherSlot[8];

    private CursorTrack mCursorTrack;
    private PinnableCursorDevice mCursorDevice;
    private CursorRemoteControlsPage mCursorRemoteControls;
    private Clip mCursorClip;

    private DrumPadBank mDrumPadBank;

    private PlayingNote[] mPlayingNotes;

    private HardwareButton mPlayButton, mStopButton, mRecordButton, mLoopButton, mForwardButton, mRewindButton,
            mBankNextButton, mBankPrevButton, mMetronomeButton, mEncoderButton, mBackButton, mVolumeLayerButton,
            mDeviceLayerButton,
            mSendsLayerButton, mPanLayerButton, mSaveButton, mQuantizeButton, mViewButton,
            mUndoButton;
    private AbsoluteHardwareKnob[] mKnobs = new AbsoluteHardwareKnob[8];
    private AbsoluteHardwareKnob mFader;
    private HardwareButton mEncoder;

    private HardwareButton[] mSceneButtons = new HardwareButton[2];

    private HardwareButton[] mPadButtons = new HardwareButton[8];
    private MultiStateHardwareLight[] mPadLights = new MultiStateHardwareLight[8];

    private int lastLayer = 0;
    private final Layers mLayers = new Layers(this);
    private Layer mMainLayer, mVolumeLayer, mPanLayer, mDeviceLayer, mSendsLayer, mClipLayer;
}
