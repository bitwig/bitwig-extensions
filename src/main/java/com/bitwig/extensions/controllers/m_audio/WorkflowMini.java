package com.bitwig.extensions.controllers.m_audio;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.*;
import com.bitwig.extensions.util.NoteInputUtils;

public class WorkflowMini extends HardwareMini {

    protected WorkflowMini(ControllerExtension driver, String modelName) {
        super(driver, modelName);
        this.modelName = modelName;
        BANK_SIZE = modelName == "Mini" ? 4 : 8;

        mLayers = new Layers(driver);

        initNoteTable();

        initAPIElements();

        initMarkInterested();

        initLayers();

        setHardwareFunctions();

    }

    private void initNoteTable() {
        for (int i = 0; i < 128; i++)
            noteTable[i] = i;
        noteBank = 0;
        for (int i = 40; i < (40 + 16); i++) {
            if (i < 44)
                noteTable[i] = noteTable[i];
            else if (i < 52)
                noteTable[i] = i - 3*BANK_SIZE;
        }   
    }

    private boolean updateNoteTable(final int direction) {
        if (direction == 1 && noteBank == 3)
            return false;
        if (direction == 0 && noteBank == 0)
            return false;
        for (int i = 0; i < (BANK_SIZE * 2); i++) {
            int noteIndex = i + PAD_NOTE_OFFSET + (i < BANK_SIZE ? 0 : BANK_SIZE);
            //int increment = (i < BANK_SIZE ? BANK_SIZE * 2 : BANK_SIZE);
            if (direction == 1) {
                noteTable[noteIndex] = noteTable[noteIndex] + (BANK_SIZE * 2);
            } else {
                noteTable[noteIndex] = noteTable[noteIndex] - (BANK_SIZE * 2);
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

    protected void initLayers() {
        mMainLayer = new Layer(mLayers, "Main");
        mDeviceLayer = new Layer(mLayers, "Device");
        mVolumeLayer = new Layer(mLayers, "Volume");
        mPanLayer = new Layer(mLayers, "Pan");
        mSendsLayer = new Layer(mLayers, "Sends");
        mClipLayer = new Layer(mLayers, "Clip");

        // initVolumeLayer();
        // initClipLayer();
        // initDeviceLayer();
        // initMainLayer();
        // initPanLayer();
        // initSendsLayer();
    }

    private void initClipLayer() {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < BANK_SIZE; j++) {
                final HardwareButton button = mPadButtons[i][j];
                final MultiStateHardwareLight led = mPadLights[i][j];
                final int index = i;
                final int jndex = j;

                mClipLayer.bindPressed(button, () -> {
                    if (!mTrackBank.getItemAt(jndex).isQueuedForStop().getAsBoolean()
                            && mClipSlot[index][jndex].isPlaying().getAsBoolean()) {
                        mTrackBank.getItemAt(jndex).stop();
                    } else {
                        mClipSlot[index][jndex].launch();
                        mClipSlot[index][jndex].select();
                    }
                });
                mClipLayer.bindLightState(() -> {
                    if (mClipSlot[index][jndex].isPlaying().getAsBoolean())
                        return RGBLightState.GREEN;
                    else if (mClipSlot[index][jndex].isPlaybackQueued().getAsBoolean())
                        return RGBLightState.GREEN_BLINK;
                    else if (mClipSlot[index][jndex].isRecording().getAsBoolean())
                        return RGBLightState.RED;
                    else if (mClipSlot[index][jndex].isRecordingQueued().getAsBoolean())
                        return RGBLightState.RED_BLINK;
                    else if (mClipSlot[index][jndex].isStopQueued().getAsBoolean())
                        return RGBLightState.YELLOW_BLINK;
                    else if (mClipSlot[index][jndex].hasContent().get())
                        return RGBLightState.YELLOW;
                    else
                        return RGBLightState.OFF;
                }, led);
            }
            mClipLayer.bindPressed(mSceneButtons[i], mSceneBank.getItemAt(i).launchAction());
        }

    }

    private void initSendsLayer() {

        for (int i = 0; i < BANK_SIZE; i++) {
            final int index = i;
            final Parameter parameter = mTrackBank.getItemAt(index).sendBank().getItemAt(0);
            final AbsoluteHardwareKnob knob = mKnobs[i];

            mSendsLayer.bind(knob, parameter);
        }
    }

    private void initPanLayer() {
        for (int i = 0; i < BANK_SIZE; i++) {
            final int index = i;
            final Parameter parameter = mTrackBank.getItemAt(index).pan();
            final AbsoluteHardwareKnob knob = mKnobs[i];

            mPanLayer.bind(knob, parameter);
        }
    }

    private void initVolumeLayer() {
        for (int i = 0; i < BANK_SIZE; i++) {
            final int index = i;
            final Track track = mTrackBank.getItemAt(index);
            track.arm().markInterested();
            track.solo().markInterested();
            track.mute().markInterested();
            track.isStopped().markInterested();
            final Parameter parameter = track.volume();
            final AbsoluteHardwareControl fader = mFaders[i];
            final AbsoluteHardwareKnob knob = mKnobs[i];

            mHost.println(modelName);
            if (modelName == "25") {
                mVolumeLayer.bind(knob, parameter);
            } else {
                mVolumeLayer.bind(fader, parameter);
                mVolumeLayer.bindPressed(mFaderButtons[i], track.arm());
                mVolumeLayer.bindReleased(mFaderButtons[i], track.arm());
                mVolumeLayer.bindLightState(() -> track.arm().get() ? RGBLightState.RED : RGBLightState.OFF,
                        mFaderButtonLights[i]);

                if (modelName != "Mini") {
                    mVolumeLayer.bindPressed(mFaderButtons[i + 8], () -> mTrackBank.cursorIndex().set(index));
                    mVolumeLayer.bindPressed(mFaderButtons[i + 16], track.mute().toggleAction());
                    mVolumeLayer.bindPressed(mFaderButtons[i + 24], track.solo().toggleAction());

                    mVolumeLayer.bindReleased(mFaderButtons[i + 8], () -> mTrackBank.cursorIndex().set(index));
                    mVolumeLayer.bindReleased(mFaderButtons[i + 16], track.mute().toggleAction());
                    mVolumeLayer.bindReleased(mFaderButtons[i + 24], track.solo().toggleAction());

                    mVolumeLayer.bindLightState(
                            () -> mTrackBank.cursorIndex().get() == index ? RGBLightState.RED : RGBLightState.OFF,
                            mFaderButtonLights[i + 8]);
                    mVolumeLayer.bindLightState(() -> track.mute().get() ? RGBLightState.RED : RGBLightState.OFF,
                            mFaderButtonLights[i + 16]);
                    mVolumeLayer.bindLightState(() -> track.solo().get() ? RGBLightState.RED : RGBLightState.OFF,
                            mFaderButtonLights[i + 24]);
                }
            }
        }
    }

    private void initDeviceLayer() {
        for (int i = 0; i < 8; i++) {
            final Parameter parameter = mCursorRemoteControls.getParameter(i);
            
            if (modelName == "Mini" && i >= 4) {
                final AbsoluteHardwareControl fader = mFaders[i - 4];
                mDeviceLayer.bind(fader, parameter);
            } else {
                final AbsoluteHardwareKnob knob = mKnobs[i];
                mDeviceLayer.bind(knob, parameter);
            }
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
                    mVolumeLayer.activate();
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
            
            if (mCursorTrack.trackType().get() == "Instrument")
                mClipLayer.deactivate();
            if (modelName == "Mini")
                mVolumeLayer.deactivate();
            switch (lastLayer) {
                case 0:
                    if (modelName == "25")
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
            // mVolumeLayer.deactivate();
            mPanLayer.activate();
            mSendsLayer.deactivate();
            mDeviceLayer.deactivate();
            mClipLayer.activate();
            switchPadFunction();
        });
        mMainLayer.bindPressed(mDeviceLayerButton, () -> {
            // lastLayer = 2;
            mVolumeLayer.deactivate();
            mPanLayer.deactivate();
            mSendsLayer.deactivate();
            mDeviceLayer.activate();
            mClipLayer.deactivate();
            switchPadFunction();
        });
        mMainLayer.bindPressed(mSendsLayerButton, () -> {
            lastLayer = 3;
            // mVolumeLayer.deactivate();
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

        if (modelName != "Mini")
            mMainLayer.bind(mFader, mCursorTrack.volume());

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < BANK_SIZE; j++) {
                final int index = j + (1 - i) * BANK_SIZE;
                final MultiStateHardwareLight light = mPadLights[i][j];

                mMainLayer.bindLightState(() -> {
                    return RGBstate(index);
                }, light);
            }
        }

        mMainLayer.activate();
        mClipLayer.activate();
        mVolumeLayer.activate();
        if (modelName != "25")
            mPanLayer.activate();
        switchPadFunction();
    }

    protected void activateInitialLayers() {
        for (Layer l : mLayers.getLayers())
            l.deactivate();
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < BANK_SIZE; j++){
                mPadLights[i][j].state().setValue(RGBLightState.OFF);
            }
        }
        mMainLayer.activate();
        mClipLayer.activate();
        mVolumeLayer.activate();
        lastLayer = 0;
        if (modelName != "25") {
            mPanLayer.activate();
            lastLayer = 1;
        }
    }

    private void switchPadFunction() {
        mPadInput.setKeyTranslationTable(mClipLayer.isActive() ? NoteInputUtils.NO_NOTES : noteTable);
        for (int i = 0; i < BANK_SIZE; i++) {
            final Track track = mTrackBank.getItemAt(i);
            final ClipLauncherSlotBank clipBank = track.clipLauncherSlotBank();
            clipBank.setIndication(mClipLayer.isActive() ? true : false);
        }
    }

    private void switchKnobFaderLayer(Layer l) {
        if (modelName == "25") {
            for (Layer layer : mLayers.getLayers()){
                if (layer != mClipLayer || layer != mMainLayer)
                    layer.deactivate();
            }
            l.activate();
        } else if (modelName == "Mini") {
            for (Layer layer : mLayers.getLayers()){
                if (layer != mClipLayer || layer != mMainLayer)
                    layer.deactivate();
            }
            if (l != mDeviceLayer)
                mVolumeLayer.activate();
            l.activate();
        } else {
            for (Layer layer : mLayers.getLayers()){
                if (layer != mClipLayer || layer != mMainLayer)
                    layer.deactivate();
            }
            mVolumeLayer.activate();
            l.activate();
        }
    }

    private void switchMainLayer(Layer l) {
        if (l == mDeviceLayer) {
            
        }
    }

    private RGBLightState RGBstate(final int index) {
        int x = index;// < BANK_SIZE ? index + 4 : index;
        // if (index > BANK_SIZE)
        //     return RGBLightState.OFF;
        if (mPlayingNotes.length != 0) {
            RGBLightState state = RGBLightState.OFF;
            // for (PlayingNote n : mPlayingNotes) {
            //     mMidiOut2.sendMidi(0x90, n.pitch(), 63);
            // }
            if (state == RGBLightState.OFF && mDrumPadBank.getItemAt(x).exists().getAsBoolean())
                return new RGBLightState(mDrumPadBank.getItemAt(x).color().get());
            if (state == RGBLightState.OFF && !mDrumPadBank.exists().getAsBoolean()) {
                return RGBLightState.CYAN;
            }
            return state;
        } else if (mDrumPadBank.exists().get() && mDrumPadBank.getItemAt(x).exists().getAsBoolean()) {
            return new RGBLightState(mDrumPadBank.getItemAt(x).color().get());
        } else if (!mDrumPadBank.exists().getAsBoolean() && mCursorTrack.trackType().get() == "Instrument") {
            return RGBLightState.CYAN;
        } else {
            return RGBLightState.OFF;
        }
    }

    private void initAPIElements() {
        final NoteInput keyboardInput = mMidiIn1.createNoteInput("Keys", "80????", "90????", "D?????");
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

        mCursorDeviceBank = mCursorTrack.createDeviceBank(1);
        mCursorDevice = mCursorTrack.createCursorDevice("01", "track", 8,
                CursorDeviceFollowMode.FIRST_INSTRUMENT_OR_DEVICE);
        mCursorRemoteControls = mCursorDevice.createCursorRemoteControlsPage(8);
        mCursorRemoteControls.setHardwareLayout(HardwareControlType.KNOB, 8);
        
        mCursorClip = mHost.createLauncherCursorClip(0, 0);
        
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
                //clipBank.setIndication(false);

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
        initKnobs();
        initButtonMatrix();
        initNavigation();
        initTransport();
    }


    private void initFaders() {
        for (int i = 0; i < BANK_SIZE; i++) {
            AbsoluteHardwareControl fader = mFaders[i];
            Track track = mTrackBank.getItemAt(i);
            Parameter parameter = track.volume();
            
            mVolumeLayer.bind(fader, parameter);

            parameter = mCursorRemoteControls.getParameter(i+4);
            mDeviceLayer.bind(fader, parameter);
        }

        if (modelName != "Mini")
            mMainLayer.bind(mFader, mCursorTrack.volume());

    }

    private void initFaderButtons() {
        for (int i = 0; i < BANK_SIZE; i++) {
            HardwareButton button = mFaderButtons[i];
            Track track = mTrackBank.getItemAt(i);
            HardwareActionBindable parameter = track.arm().toggleAction();

            mMainLayer.bindPressed(button, parameter);
        }
    }

    private void initKnobs() {
        for (int i = 0; i < BANK_SIZE; i++) {
            AbsoluteHardwareControl knob = mKnobs[i];
            Track track = mTrackBank.getItemAt(i);
            Parameter parameter;

            parameter = track.volume();
            if (modelName == "25")
                mVolumeLayer.bind(knob, parameter);

            parameter = mCursorRemoteControls.getParameter(i);
            mDeviceLayer.bind(knob, parameter);

            parameter = track.pan();
            mPanLayer.bind(knob, parameter);

            parameter = track.sendBank().getItemAt(0);
            mPanLayer.bind(knob, parameter);
        }
    }

    private void initButtonMatrix() {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < BANK_SIZE; j++) {
                int index = i;
                int jndex = j;
                
                HardwareButton button = mPadButtons[i][j];
                MultiStateHardwareLight light = mPadLights[i][j];
                Track track = mTrackBank.getItemAt(j);
                ClipLauncherSlot slot = mClipSlot[i][j];

                mClipLayer.bindPressed(button, () -> {
                    slot.launch();
                });

                mClipLayer.bindLightState(() -> {
                    if (mClipSlot[index][jndex].isPlaying().getAsBoolean())
                        return RGBLightState.GREEN;
                    else if (mClipSlot[index][jndex].isPlaybackQueued().getAsBoolean())
                        return RGBLightState.GREEN_BLINK;
                    else if (mClipSlot[index][jndex].isRecording().getAsBoolean())
                        return RGBLightState.RED;
                    else if (mClipSlot[index][jndex].isRecordingQueued().getAsBoolean())
                        return RGBLightState.RED_BLINK;
                    else if (mClipSlot[index][jndex].isStopQueued().getAsBoolean())
                        return RGBLightState.YELLOW_BLINK;
                    else if (mClipSlot[index][jndex].hasContent().get())
                        return RGBLightState.YELLOW;
                    else
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

        initLayerNavigation();
    }

    private void initLayerNavigation() {
        mMainLayer.bindPressed(mVolumeLayerButton, switchLayer(mVolumeLayer));
        mMainLayer.bindPressed(mPanLayerButton, switchLayer(mPanLayer));
        mMainLayer.bindPressed(mDeviceLayerButton, switchLayer(mDeviceLayer));
        mMainLayer.bindPressed(mSendsLayerButton, switchLayer(mSendsLayer));

        mMainLayer.bindPressed(mEncoderButton, () -> {
            if (mDeviceLayer.isActive())
                switchLayer(mPanLayer); // TO DO rework last layer...
            else 
                switchLayer(mDeviceLayer);
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
        mClipLayer.activate();
        mMainLayer.activate();
        layer.activate();

        if (modelName != "25" && !(modelName == "Mini" && mDeviceLayer.isActive()))
            mVolumeLayer.activate();
        
        return null;
    }

    private void save() {
        final Action saveAction = mApplication.getAction("Save");
        if (saveAction != null) {
            saveAction.invoke();
        }
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
    private Transport mTransport;

    private MasterTrack mMasterTrack;

    private int BANK_SIZE = 8;

    private TrackBank mTrackBank;
    private SceneBank mSceneBank;

    private ClipLauncherSlot[][] mClipSlot = new ClipLauncherSlot[2][8];
    private DeviceBank mCursorDeviceBank;
    private CursorTrack mCursorTrack;
    private PinnableCursorDevice mCursorDevice;
    private CursorRemoteControlsPage mCursorRemoteControls;
    private Clip mCursorClip;

    private DrumPadBank mDrumPadBank;

    private PlayingNote[] mPlayingNotes;

    private int lastLayer = 1;
    private final Layers mLayers;
    private Layer mMainLayer, mVolumeLayer, mPanLayer, mDeviceLayer, mSendsLayer, mClipLayer;

}
