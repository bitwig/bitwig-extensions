package com.bitwig.extensions.controllers.m_audio;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;

import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.util.NoteInputUtils;

public class Workflow extends Hardware {

    protected Workflow(ControllerExtension driver, String modelName) {
        super(driver, modelName);
        this.modelName = modelName;
        MODEL_MINI = modelName == "Mini";
        MODEL_25 = modelName == "25";
        BANK_SIZE = MODEL_MINI ? 4 : 8;

        mLayers = new Layers(driver);

        initNoteTable();

        initAPIElements();

        initMarkInterested();

        initLayers();

        setHardwareFunctions();

        activateInitialLayers();
        switchPadFunction();
    }

    private void initNoteTable() {
        for (int i = 0; i < 128; i++)
            noteTable[i] = i;
        noteBank = 0;
        if (MODEL_MINI) {
            for (int i = 40; i < (40 + 16); i++) {
                if (i < 44)
                    noteTable[i] = noteTable[i];
                else if (i < 52)
                    noteTable[i] = i - 3 * BANK_SIZE;
            }
        }
    }

    private boolean updateNoteTable(final int direction) {
        if (direction == 1 && noteBank == 3)
            return false;
        if (direction == 0 && noteBank == 0)
            return false;
        if (MODEL_MINI) {
            for (int i = 0; i < (BANK_SIZE * 2); i++) {
                int noteIndex = i + PAD_NOTE_OFFSET + (i < BANK_SIZE ? 0 : BANK_SIZE);

                if (direction == 1) {
                    noteTable[noteIndex] = noteTable[noteIndex] + (BANK_SIZE * 2);
                } else {
                    noteTable[noteIndex] = noteTable[noteIndex] - (BANK_SIZE * 2);
                }
                if (!mClipLayer.isActive())
                    mPadInput.setKeyTranslationTable(noteTable);
            }
        } else {
            for (int i = 0; i < (BANK_SIZE * 2); i++) {
                int noteIndex = i + 36;

                if (direction == 1) {
                    noteTable[noteIndex] = noteTable[noteIndex] + (BANK_SIZE * 2);
                } else {
                    noteTable[noteIndex] = noteTable[noteIndex] - (BANK_SIZE * 2);
                }
                if (!mClipLayer.isActive())
                    mPadInput.setKeyTranslationTable(noteTable);
            }
        }
        if (direction == 1)
            noteBank += 1;
        else
            noteBank -= 1;
        return true;
    }

    protected void initLayers() {
        mMainLayer = new Layer(mLayers, "Main");
        mDeviceLayer = new Layer(mLayers, "Device");
        mVolumeLayer = new Layer(mLayers, "Volume");
        mPanLayer = new Layer(mLayers, "Pan");
        mSendsLayer = new Layer(mLayers, "Sends");
        mClipLayer = new Layer(mLayers, "Clip");
    }

    protected void activateInitialLayers() {
        for (Layer l : mLayers.getLayers())
            l.deactivate();
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < BANK_SIZE; j++) {
                mPadLights[i][j].state().setValue(RGBLightState.OFF);
            }
        }
        mMainLayer.activate();
        mClipLayer.activate();
        mVolumeLayer.activate();

        if (!MODEL_25)
            mPanLayer.activate();
    }

    private void switchPadFunction() {
        mPadInput.setKeyTranslationTable(mClipLayer.isActive() ? NoteInputUtils.NO_NOTES : noteTable);
        for (int i = 0; i < BANK_SIZE; i++) {
            final Track track = mTrackBank.getItemAt(i);
            final ClipLauncherSlotBank clipBank = track.clipLauncherSlotBank();
            clipBank.setIndication(mClipLayer.isActive() ? true : false);
        }
    }

    private void initAPIElements() {
        final NoteInput keyboardInput = mMidiIn1.createNoteInput("Keys", "80????", "90????", "D?????", "E0????", "B001??");
        keyboardInput.setShouldConsumeEvents(true);

        mPadInput = mMidiIn2.createNoteInput("Pads", "80????", "90????");
        mPadInput.setShouldConsumeEvents(true);
        mPadInput.setKeyTranslationTable(noteTable);

        mTransport = mHost.createTransport();

        mMasterTrack = mHost.createMasterTrack(8);

        mCursorTrack = mHost.createCursorTrack(BANK_SIZE, 0);
        mCursorTrack.volume().setIndication(true);
        mCursorTrack.playingNotes().addValueObserver(notes -> {
            mPlayingNotes = notes;
        });

        mTrackBank = mHost.createTrackBank(BANK_SIZE, BANK_SIZE, 2);
        mTrackBank.followCursorTrack(mCursorTrack);

        mSceneBank = mTrackBank.sceneBank();

        mCursorDevice = mCursorTrack.createCursorDevice("01", "track", 8,
                CursorDeviceFollowMode.FIRST_INSTRUMENT_OR_DEVICE);
        mCursorRemoteControls = mCursorDevice.createCursorRemoteControlsPage(8);
        mCursorRemoteControls.setHardwareLayout(HardwareControlType.KNOB, 8);

        CursorDevice mInstrument = mCursorTrack.createCursorDevice("02", "track", 8,
                CursorDeviceFollowMode.FIRST_INSTRUMENT);
        mDrumPadBank = mInstrument.createDrumPadBank(BANK_SIZE * 2);

    }

    private void initMarkInterested() {
        mTransport.isPlaying().markInterested();
        mTransport.isArrangerLoopEnabled().markInterested();
        mTransport.isArrangerRecordEnabled().markInterested();
        mTransport.isMetronomeEnabled().markInterested();

        mMasterTrack.volume().markInterested();

        mTrackBank.canScrollBackwards().markInterested();
        mTrackBank.canScrollForwards().markInterested();
        mTrackBank.cursorIndex().markInterested();

        mSceneBank.canScrollBackwards().markInterested();
        mSceneBank.canScrollForwards().markInterested();
        mSceneBank.getScene(0).exists().markInterested();

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < BANK_SIZE; j++) {
                final int index = i;
                final int jndex = j;
                final Track track = mTrackBank.getItemAt(j);
                final ClipLauncherSlotBank clipBank = track.clipLauncherSlotBank();
                final ClipLauncherSlot clip = clipBank.getItemAt(i);

                track.isQueuedForStop().markInterested();
                track.arm().markInterested();
                track.mute().markInterested();
                track.solo().markInterested();
                // clipBank.setIndication(false);

                clip.isPlaying().markInterested();
                clip.isRecording().markInterested();
                clip.isPlaybackQueued().markInterested();
                clip.isRecordingQueued().markInterested();
                clip.isStopQueued().markInterested();
                clip.hasContent().markInterested();

                mClipSlot[index][jndex] = clip;
            }
        }

        mCursorTrack.arm().markInterested();
        mCursorTrack.volume().markInterested();
        mCursorTrack.pan().markInterested();
        mCursorTrack.trackType().markInterested();
        mCursorTrack.sendBank().exists().markInterested();
        mCursorTrack.color().markInterested();
        mCursorTrack.position().markInterested();

        mDrumPadBank.exists().markInterested();

        for (int i = 0; i < (BANK_SIZE * 2); i++) {
            mDrumPadBank.getItemAt(i).color().markInterested();
            mDrumPadBank.getItemAt(i).exists().markInterested();
            mDrumPadBank.getItemAt(i).isMutedBySolo().markInterested();
        }
    }

    private void setHardwareFunctions() {
        initFaders();
        initFaderButtons();
        initFaderButtonsMode();
        initKnobs();
        initButtonMatrix();
        initNavigation();
        initTransport();
    }

    private void initFaders() {
        if (!MODEL_25) {
            for (int i = 0; i < BANK_SIZE; i++) {
                AbsoluteHardwareControl fader = mFaders[i];
                Track track = mTrackBank.getItemAt(i);
                Parameter parameter = track.volume();

                mVolumeLayer.bind(fader, parameter);

                if (MODEL_MINI) {
                    parameter = mCursorRemoteControls.getParameter(i + 4);
                    mDeviceLayer.bind(fader, parameter);
                }
            }
        }
        if (!MODEL_MINI && !MODEL_25)
            mMainLayer.bind(mFader, mMasterTrack.volume());

        if (MODEL_25)
            mMainLayer.bind(mFader, mCursorTrack.volume());

    }

    private void initFaderButtons() {
        for (int i = 0; i < BANK_SIZE; i++) {
            int index = i;
            HardwareButton button = mFaderButtons[i];
            MultiStateHardwareLight light = mFaderButtonLights[i];
            Track track = mTrackBank.getItemAt(i);

            mMainLayer.bindPressed(button, () -> switchFaderButtonModes(track, index));
            mMainLayer.bindReleased(button, () -> switchFaderButtonModes(track, index));

            mMainLayer.bindLightState(
                    () -> switchFaderButtonLightModes(track, index) ? RGBLightState.RED : RGBLightState.OFF, light);
        }
    }

    private void initFaderButtonsMode() {
        for (int i = 0; i < 5; i++) {
            HardwareButton button = mFaderButtonsMode[i];
            int mode = i;
            mMainLayer.bindPressed(button, () -> faderButtonMode = mode);
        }
    }

    private Runnable switchFaderButtonModes(Track track, int index) {
        switch (faderButtonMode) {
            case 1:
                track.arm().toggle();
                break;
            case 2:
                mTrackBank.cursorIndex().set(index);
                break;
            case 3:
                track.mute().toggle();
                break;
            case 4:
                track.solo().set(!track.solo().get()); 
                break;
            default:
                break;
        }
        return null;
    }

    private Boolean switchFaderButtonLightModes(Track track, int index) {
        switch (faderButtonMode) {
            case 1:
                return track.arm().get();
            case 2:
                return mTrackBank.cursorIndex().get() == index;
            case 3:
                return track.mute().get();
            case 4:
                return track.solo().get();
            default:
                return false;
        }
    }

    private void initKnobs() {
        for (int i = 0; i < BANK_SIZE; i++) {
            AbsoluteHardwareControl knob = mKnobs[i];
            Track track = mTrackBank.getItemAt(i);
            Parameter parameter;

            parameter = track.volume();
            if (MODEL_25)
                mVolumeLayer.bind(knob, parameter);

            parameter = mCursorRemoteControls.getParameter(i);
            mDeviceLayer.bind(knob, parameter);

            parameter = track.pan();
            mPanLayer.bind(knob, parameter);

            parameter = track.sendBank().getItemAt(0);
            mSendsLayer.bind(knob, parameter);
        }
    }

    private void initButtonMatrix() {
        for (int i = 0; i < 2; i++) {
            mClipLayer.bindPressed(mSceneButtons[i], mSceneBank.getItemAt(i).launchAction());

            for (int j = 0; j < BANK_SIZE; j++) {
                int index = i;
                int jndex = j;

                HardwareButton button = mPadButtons[i][j];
                MultiStateHardwareLight light = mPadLights[i][j];
                ClipLauncherSlot slot = mClipSlot[i][j];

                mClipLayer.bindPressed(button, () -> {
                    // slot.launch();
                    // delay?
                    STOP_DELAY = false;
                    if (slot.isPlaying().get())
                        mHost.scheduleTask(() -> STOP_DELAY = true, (long) 500.0);
                    else
                        slot.launch();
                });

                mClipLayer.bindReleased(button, () -> {
                    if (STOP_DELAY)
                        mTrackBank.getItemAt(jndex).stop();
                    else
                        slot.launch();
                });

                mClipLayer.bindLightState(() -> {
                    if (mClipSlot[index][jndex].isStopQueued().getAsBoolean() || (mTrackBank.getItemAt(jndex).isQueuedForStop().getAsBoolean() && mClipSlot[index][jndex].isPlaying().getAsBoolean()))
                        return RGBLightState.YELLOW_BLINK;
                    else if (mClipSlot[index][jndex].isPlaybackQueued().getAsBoolean())
                        return RGBLightState.GREEN_BLINK;
                    else if (mClipSlot[index][jndex].isPlaying().getAsBoolean())
                        return RGBLightState.GREEN;
                    else if (mClipSlot[index][jndex].isRecording().getAsBoolean())
                        return RGBLightState.RED;
                    else if (mClipSlot[index][jndex].isRecordingQueued().getAsBoolean())
                        return RGBLightState.RED_BLINK;
                    else if (mClipSlot[index][jndex].hasContent().get())
                        return RGBLightState.YELLOW;
                    else
                        return RGBLightState.WHITE;
                }, light);

                mDeviceLayer.bindLightState(() -> {
                    int noteTabelIndex;
                    if (index == 0)
                        noteTabelIndex = 40 + jndex;
                    else
                        noteTabelIndex = 48 + jndex;
                    int drumBankIndex = jndex + ((1 - index) * BANK_SIZE);
                    if (!MODEL_MINI) {
                        if (jndex < 4 && index == 1)
                            drumBankIndex = jndex; // + ((1 - index) * BANK_SIZE);
                        else if (jndex < 4 && index == 0)
                            drumBankIndex = jndex + 4;
                        else if (index == 1)
                            drumBankIndex = jndex + 4;
                        else if (index == 0)
                            drumBankIndex = jndex + 8;
                    }

                    if (mPlayingNotes.length != 0) {
                        for (PlayingNote n : mPlayingNotes) {
                            if (!MODEL_MINI && n.pitch() == noteTable[36 + drumBankIndex])
                                return RGBLightState.WHITE;
                            if (MODEL_MINI && n.pitch() == noteTable[noteTabelIndex])
                                return RGBLightState.WHITE;
                        }
                    }

                    if (mDrumPadBank.exists().get() && mDrumPadBank.getItemAt(drumBankIndex).exists().get())
                        return new RGBLightState(mDrumPadBank.getItemAt(drumBankIndex).color().get());

                    if (!mDrumPadBank.exists().get() && mCursorTrack.trackType().get() == "Instrument")
                        return RGBLightState.ORANGE;
                    return RGBLightState.OFF;
                }, light);

            }
        }
    }

    private void initNavigation() {
        mMainLayer.bindPressed(mEncoder, () -> {
            if (mDeviceLayer.isActive()) {
                mCursorDevice.selectPrevious();
            } else if (mBackButton.isPressed().getAsBoolean()) {
                mSceneBank.scrollBackwards();
            } else {
                mCursorTrack.selectPrevious();
            }
        });
        mMainLayer.bindReleased(mEncoder, () -> {
            if (mDeviceLayer.isActive()) {
                mCursorDevice.selectNext();
            } else if (mBackButton.isPressed().getAsBoolean()) {
                mSceneBank.scrollForwards();
            } else {
                mCursorTrack.selectNext();
            }
        });
        mMainLayer.bindPressed(mBankNextButton, () -> {
            if (updateNoteTable(1))// && !mClipLayer.isActive())
                mDrumPadBank.scrollPageForwards();
        });
        mMainLayer.bindPressed(mBankPrevButton, () -> {
            if (updateNoteTable(0))// && !mClipLayer.isActive())
                mDrumPadBank.scrollPageBackwards();
        });

        initLayerNavigation();
    }

    private void initLayerNavigation() {
        mMainLayer.bindPressed(mVolumeLayerButton, () -> switchLayer(mVolumeLayer));
        mMainLayer.bindPressed(mPanLayerButton, () -> switchLayer(mPanLayer));
        mMainLayer.bindPressed(mDeviceLayerButton, () -> switchLayer(mDeviceLayer));
        mMainLayer.bindPressed(mSendsLayerButton, () -> switchLayer(mSendsLayer));

        mMainLayer.bindPressed(mEncoderButton, () -> {
            if (mDeviceLayer.isActive()) {
                switchLayer(mPanLayer); // TO DO rework last layer...
            } else {
                switchLayer(mDeviceLayer);
            }
        });
    }

    private void initTransport() {
        mMainLayer.bindPressed(mStopButton, mTransport.stopAction());
        mMainLayer.bindPressed(mPlayButton, mTransport.playAction());
        mMainLayer.bindPressed(mRecordButton, mTransport.recordAction());
        mMainLayer.bindToggle(mLoopButton, mTransport.isArrangerLoopEnabled());
        mMainLayer.bindPressed(mRewindButton, mTransport.rewindAction());
        mMainLayer.bindPressed(mForwardButton, mTransport.fastForwardAction());
        mMainLayer.bindToggle(mMetronomeButton, mTransport.isMetronomeEnabled());

    }

    private Runnable switchLayer(Layer layer) {
        for (Layer l : mLayers.getLayers()) {
            l.deactivate();
        }

        mMainLayer.activate();
        layer.activate();

        // reset DrumBank
        if (mDeviceLayer.isActive()) {
            initNoteTable();
            mDrumPadBank.scrollPosition().set(36);
        }

        if (!(mDeviceLayer.isActive() && mCursorTrack.trackType().get() == "Instrument"))
            mClipLayer.activate();

        if (!MODEL_25 && !(MODEL_MINI && mDeviceLayer.isActive()))
            mVolumeLayer.activate();

        switchPadFunction();
        return null;
    }

    public HardwareSurface getHardwareSurface() {
        return mHardwareSurface;
    }

    public MidiIn getMidiIn(int i) {
        if (i == 1)
            return mMidiIn1;
        else
            return mMidiIn2;
    }

    public MidiOut getMidiOut(int i) {
        if (i == 1)
            return mMidiOut1;
        else
            return mMidiOut2;
    }

    private NoteInput mPadInput;
    protected final Integer[] noteTable = new Integer[128];
    protected int noteBank = 0;
    protected int faderButtonMode = 1;
    private Transport mTransport;

    private MasterTrack mMasterTrack;

    private int BANK_SIZE = 8;
    private Boolean MODEL_MINI, MODEL_25;
    private Boolean STOP_DELAY = false;

    private TrackBank mTrackBank;
    private SceneBank mSceneBank;

    private ClipLauncherSlot[][] mClipSlot = new ClipLauncherSlot[2][8];
    private CursorTrack mCursorTrack;
    private PinnableCursorDevice mCursorDevice;
    private CursorRemoteControlsPage mCursorRemoteControls;

    private DrumPadBank mDrumPadBank;

    private PlayingNote[] mPlayingNotes;

    private final Layers mLayers;
    private Layer mMainLayer, mVolumeLayer, mPanLayer, mDeviceLayer, mSendsLayer, mClipLayer;

}
