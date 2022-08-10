package com.bitwig.extensions.controllers.m_audio;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.*;
import com.bitwig.extensions.util.NoteInputUtils;


public class Workflow extends Hardware{

    protected Workflow(ControllerExtension driver, String modelName) {
        super(driver, modelName);
        this.modelName = modelName;
        mLayers = new Layers(driver);
        //TODO Auto-generated constructor stub

        final NoteInput keyboardInput = mMidiIn1.createNoteInput("Keys", "80????", "90????", "D?????");
        keyboardInput.setShouldConsumeEvents(true);

        initNoteTable();
        mPadInput = mMidiIn2.createNoteInput("Pads", "80????", "90????");
        mPadInput.setShouldConsumeEvents(true);
        mPadInput.setKeyTranslationTable(noteTable);

        mTransport = mHost.createTransport();
        mTransport.isPlaying().markInterested();
        mTransport.isArrangerLoopEnabled().markInterested();
        mTransport.isArrangerRecordEnabled().markInterested();
        mTransport.isMetronomeEnabled().markInterested();

        mMasterTrack = mHost.createMasterTrack(8);
        mMasterTrack.volume().markInterested();

        mTrackBank = mHost.createTrackBank(8, 8, 2);    
        mTrackBank.canScrollBackwards().markInterested();
        mTrackBank.canScrollForwards().markInterested();
        mTrackBank.cursorIndex().markInterested();
        
        mSceneBank = mTrackBank.sceneBank();
        mSceneBank.canScrollBackwards().markInterested();
        mSceneBank.canScrollForwards().markInterested();


        for (int j = 0; j < 2; ++j) {
            final Scene scene = mSceneBank.getScene(j);
            scene.exists().markInterested();
        }
        for (int i = 0; i < 8; i++) {
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

        mCursorTrack = mHost.createCursorTrack(8, 0);
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

        mCursorClip = mHost.createLauncherCursorClip(0, 0);

        CursorDevice mInstrument = mCursorTrack.createCursorDevice("02", "track", 8,
                CursorDeviceFollowMode.FIRST_INSTRUMENT);


        mDrumPadBank = mInstrument.createDrumPadBank(16);
        mDrumPadBank.exists().markInterested();
        for (int i = 0; i < 16; i++) {
            mDrumPadBank.getItemAt(i).color().markInterested();
            mDrumPadBank.getItemAt(i).exists().markInterested();
            mDrumPadBank.getItemAt(i).isMutedBySolo().markInterested();
        }


        initLayers();

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
        for (int i = 0; i < 16; i++) {
            if (direction == 1) {
                noteTable[i + PAD_NOTE_OFFSET] = noteTable[i + PAD_NOTE_OFFSET] + 16;
            } else {
                noteTable[i + PAD_NOTE_OFFSET] = noteTable[i + PAD_NOTE_OFFSET] - 16;
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

        initVolumeLayer();
        initClipLayer();
        initDeviceLayer();
        initMainLayer();
        initPanLayer();
        initSendsLayer();
    }

    private void initClipLayer() {
        for (int i = 0; i < 16; i++) {
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

        for (int i = 0; i < 8; i++) {
            final int index = i;
            final Parameter parameter = mCursorTrack.sendBank().getItemAt(index);
            final AbsoluteHardwareKnob knob = mKnobs[i];

            mSendsLayer.bind(knob, parameter);
        }
    }

    private void initPanLayer() {
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final Parameter parameter = mTrackBank.getItemAt(index).pan();
            final AbsoluteHardwareKnob knob = mKnobs[i];

            mPanLayer.bind(knob, parameter);
        }
    }

    private void initVolumeLayer() {
        for (int i = 0; i < 8; i++) {
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
                mVolumeLayer.bindPressed(mFaderButtons[i+8], () -> mTrackBank.cursorIndex().set(index));
                mVolumeLayer.bindPressed(mFaderButtons[i+16], track.mute().toggleAction());
                mVolumeLayer.bindPressed(mFaderButtons[i+24], track.solo().toggleAction());
        
                mVolumeLayer.bindLightState(() -> track.arm().get() ? RGBLightState.RED : RGBLightState.OFF, mFaderButtonLights[i]);
                mVolumeLayer.bindLightState(() -> mTrackBank.cursorIndex().get() == index ? RGBLightState.RED : RGBLightState.OFF, mFaderButtonLights[i+8]);
                mVolumeLayer.bindLightState(() -> track.mute().get() ? RGBLightState.RED : RGBLightState.OFF, mFaderButtonLights[i+16]);
                mVolumeLayer.bindLightState(() -> track.solo().get() ? RGBLightState.RED : RGBLightState.OFF, mFaderButtonLights[i+24]);
        
            }
        }
    }


    private void initDeviceLayer() {
        for (int i = 0; i < 8; i++) {
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
            if (mCursorTrack.trackType().get() == "Instrument")
                mClipLayer.deactivate();
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
            //mVolumeLayer.deactivate();
            mPanLayer.activate();
            mSendsLayer.deactivate();
            mDeviceLayer.deactivate();
            mClipLayer.activate();
            switchPadFunction();
        });
        mMainLayer.bindPressed(mDeviceLayerButton, () -> {
            //lastLayer = 2;
            //mVolumeLayer.deactivate();
            mPanLayer.deactivate();
            mSendsLayer.deactivate();
            mDeviceLayer.activate();
            mClipLayer.deactivate();
            switchPadFunction();
        });
        mMainLayer.bindPressed(mSendsLayerButton, () -> {
            lastLayer = 3;
            //mVolumeLayer.deactivate();
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

        for (int i = 0; i < 16; i++) {
            final MultiStateHardwareLight light = mPadLights[i];
            final int index = i;

            mMainLayer.bindLightState(() -> {
                return RGBstate(index);
            }, light);
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
        for (int i = 0; i < 16; i++)
            mPadLights[i].state().setValue(RGBLightState.OFF);
        mMainLayer.activate();
        mClipLayer.activate();
        mVolumeLayer.activate();
        if (modelName != "25")
            mPanLayer.activate();
    }

    private void switchPadFunction() {
        mPadInput.setKeyTranslationTable(mClipLayer.isActive() ? NoteInputUtils.NO_NOTES : noteTable);
        for (int j = 0; j < 2; ++j) {
            final Scene scene = mSceneBank.getScene(j);
        }
        for (int i = 0; i < 8; i++) {
            final Track track = mTrackBank.getItemAt(i);
            final ClipLauncherSlotBank clipBank = track.clipLauncherSlotBank();
            clipBank.setIndication(mClipLayer.isActive() ? true : false);
        }
    }

    private void switchLayer(Layer l) {
        for (Layer layer : mLayers.getLayers()) {
            
        }
    }

    private RGBLightState RGBstate(final int index) {
        if (mPlayingNotes.length != 0) {
            RGBLightState state = RGBLightState.OFF;
            for (PlayingNote n : mPlayingNotes) {
                if (n.pitch() == 36 + index + (16 * noteBank))
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

    private TrackBank mTrackBank;
    private SceneBank mSceneBank;

    private ClipLauncherSlot[] mClipSlot = new ClipLauncherSlot[16];

    private CursorTrack mCursorTrack;
    private PinnableCursorDevice mCursorDevice;
    private CursorRemoteControlsPage mCursorRemoteControls;
    private Clip mCursorClip;

    private DrumPadBank mDrumPadBank;

    private PlayingNote[] mPlayingNotes;

    private int lastLayer = 0;
    private final Layers mLayers;
    private Layer mMainLayer, mVolumeLayer, mPanLayer, mDeviceLayer, mSendsLayer, mClipLayer;
    
}
