package com.bitwig.extensions.controllers.novation.launchpad_pro_mk3;

import java.util.List;
import java.util.function.Consumer;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.AbsoluteHardwareControlBinding;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class LaunchpadProMK3Workflow extends ControllerExtension {

    public LaunchpadProMK3Workflow(ControllerExtensionDefinition definition, ControllerHost host) {
        super(definition, host);
    }

    @Override
    public void init() {
        mHost = getHost();
        mApplication = mHost.createApplication();
        mApplication.recordQuantizationGrid().addValueObserver(g -> {
            mHost.println(g);
            if (g == "1/32" || g == "1/16" || g == "1/8" || g == "1/4")
                QUANTIZATION_GRID_SIZE = g;
        });

        mHardwareSurface = mHost.createHardwareSurface();
        mHardwareSurface.setPhysicalSize(300, 300);

        initMIDI();

        mNoteInput = mMidiIn1.createNoteInput("Note/Sequencer Ch. 1", "90????", "D0??", "80????");
        mMidiIn1.createNoteInput("Sequencer Ch. 2", "91????", "D0??", "81????");
        mMidiIn1.createNoteInput("Sequencer Ch. 3", "92????", "D0??", "82????");
        mMidiIn1.createNoteInput("Sequencer Ch. 4", "93????", "D0??", "83????");
        mDrumInput = mMidiIn0.createNoteInput("Drum", "98????");
        mDrumInput.setKeyTranslationTable(drumBank.getNoteTable());

        mTransport = mHost.createTransport();
        mTransport.getClipLauncherPostRecordingTimeOffset().addValueObserver(v -> {
            FIXED_LENGTH_VALUE = (int) v / mTransport.timeSignature().numerator().get();
        });

        mCursorClip = mHost.createLauncherCursorClip(192, 128);
        mCursorTrack = mHost.createCursorTrack(8, 8);
        mCursorTrack.canHoldNoteData().addValueObserver(b -> {
            if (b)
                mMidiOut.sendSysex(sysex.PRINT_TO_CLIP_ON);
            else
                mMidiOut.sendSysex(sysex.PRINT_TO_CLIP_OFF);

        });
        mCursorTrack.playingNotes().addValueObserver(notes -> mPlayingNotes = notes);
        mCursorTrack.clipLauncherSlotBank().addIsSelectedObserver((i, b) -> {
            if (b)
                mCursorSlotIndex = i;
        });

        mDeviceBank = mCursorTrack.createDeviceBank(8);
        mCursorDevice = mCursorTrack.createCursorDevice();
        CursorDevice mInstrument = mCursorTrack.createCursorDevice("01", "track", 8,
                CursorDeviceFollowMode.FIRST_INSTRUMENT);
        mCursorRemoteControlsPage = mCursorDevice.createCursorRemoteControlsPage(8);

        mTrackBank = mHost.createTrackBank(8, 8, 8);
        mTrackBank.followCursorTrack(mCursorTrack);

        mSessionOverviewTrackBank = mHost.createTrackBank(64, 64, 64);
        mSessionOverviewTrackBank.followCursorTrack(mCursorTrack); // Keep?

        mDrumPadBank = mInstrument.createDrumPadBank(64);
        mDrumPadBank.scrollPosition().addValueObserver(position -> drumBank.update(position));

        initHardwareControlls();

        initLayers();
        initPadMatrix();
        initVolumeLayer();
        initPanLayer();
        initScenes();
        initMarkInterested();

        initDeviceLayer();
        initSendsLayer();
        initNavigation();
        initTransport();
        initFunctionButtons();
        initBottomButtons();
        initFixedLengthLayer();
        initClearLayer();
        initDuplicateLayer();
        initDoubleLayer();
        initQuantizeLayer();
        initShiftLayer();

        initValueObserverForMidiCallback();

        initSessionOverview();

        mSessionLayer.activate();

        mHost.showPopupNotification("Launchpad Pro Mk3 initialized...");
    }

    private void initValueObserverForMidiCallback() {
        for (int i = 0; i < 8; i++) {
            int ix = i;
            final Track track = mTrackBank.getItemAt(i);
            track.volume().value().addValueObserver(128, value -> {
                if (mVolumeLayer.isActive())
                    mMidiOut.sendMidi(0xb4, ix, value);
            });
            track.pan().value().addValueObserver(128, value -> {
                if (mPanLayer.isActive())
                    mMidiOut.sendMidi(0xb4, ix + 8, value);
            });
            for (int sendIndex = 0; sendIndex < 8; sendIndex++) {
                final int s = sendIndex;
                track.sendBank().getItemAt(sendIndex).value().addValueObserver(128, value -> {
                    if (mSendsLayer.isActive() && mSendIndex == s)
                        mMidiOut.sendMidi(0xb4, ix + 16, value);
                });
            }
            mCursorRemoteControlsPage.getParameter(i).value().addValueObserver(128, value -> {
                if (mDeviceLayer.isActive())
                    mMidiOut.sendMidi(0xb4, ix + 24, value);
            });
        }
    }

    // TODO: Rework for Performance!!
    private void midiCallback(String s) {
        // mHost.println(s);
        if (isNoteModeActive && mPlayingNotes != null) {
            sendNotesToDevice();
        }

        if (s.startsWith(sysex.SESSION_MODE_PREFIX)
                && (layerManagement.getLastLayer() == null || layerManagement.getLastLayer() == mSessionLayer)) {
            isNoteModeActive = false;
            // In LayerManagement auslagern
            List<Layer> l = mLayers.getLayers();
            for (int i = 0; i < l.size(); i++) {
                l.get(i).deactivate();
            }
            mSessionLayer.activate();
        }

        if (s.startsWith(sysex.NOTE_MODE_PREFIX) || s.startsWith(sysex.CHORD_MODE_PREFIX)) {
            isNoteModeActive = true;
            mHost.println("Note or Chord Mode");
            List<Layer> l = mLayers.getLayers();
            for (int i = 0; i < l.size(); i++) {
                l.get(i).deactivate();
            }
            mSessionLayer.activate();

        }

        if (s.startsWith(sysex.PRINT_TO_CLIP_PREFIX)) {
            printToClip.print(this, s);
        }

        if ((isTrackBankNavigated || layerManagement.getLastLayer() != null)
                && (mVolumeLayer.isActive() || mPanLayer.isActive()
                        || mSendsLayer.isActive() || mDeviceLayer.isActive())) {
            sendFaderValues();
        }
    }

    public void sendFaderValues() {
        for (int i = 0; i < 8; i++) {
            final Track track = mTrackBank.getItemAt(i);

            if (mVolumeLayer.isActive()) {
                mMidiOut.sendMidi(0xb5, i,
                        track.exists().get() ? new RGBState(track.color().get()).getMessage() : 0);
                final Parameter volume = track.volume();
                mMidiOut.sendMidi(0xb4, i, (int) (volume.get() * 127));
            }
            if (mPanLayer.isActive()) {
                mMidiOut.sendMidi(0xb5, i + 8,
                        track.exists().get() ? new RGBState(track.color().get()).getMessage() : 0);
                final Parameter pan = track.pan();
                mMidiOut.sendMidi(0xb4, i + 8, (int) (pan.get() * 127));
            }
            if (mSendsLayer.isActive()) {
                RGBState r = new RGBState(track.sendBank().getItemAt(mSendIndex).sendChannelColor().get());
                r = r.getMessage() == 0 ? RGBState.WHITE : r;
                mMidiOut.sendMidi(0xb5, i + 16,
                        track.exists().get() && track.trackType().get() != "Master" ? r.getMessage() : 0);
                final Parameter send = track.sendBank().getItemAt(mSendIndex);
                mMidiOut.sendMidi(0xb4, i + 16, (int) (send.get() * 127));
            }
            if (mDeviceLayer.isActive()) {
                mMidiOut.sendMidi(0xb5, i + 24, mCursorRemoteControlsPage.getParameter(i).exists().get() ? 79 : 0);
                final Parameter device = mCursorRemoteControlsPage.getParameter(i);
                mMidiOut.sendMidi(0xb4, i + 24, (int) (device.get() * 127));
            }
        }
    }

    private void sendNotesToDevice() {
        if (!mDrumPadBank.exists().get()) {
            for (int i = 0; i < 88; i++) {
                mMidiOut.sendMidi(0x8f, i, 21);
                if (mPlayingNotes.length != 0 && mPlayingNotes != null) {
                    mHost.println("PLayingNotes");
                    for (PlayingNote n : mPlayingNotes) {
                        mMidiOut.sendMidi(0x9f, n.pitch(), 21);
                    }
                }
            }
        }
        if (mDrumPadBank.exists().get()) {
            mMidiOut.sendSysex(sysex.DAW_DRUM);
            for (int i = 0; i < 64; i++) {
                if (mDrumPadBank.getItemAt(i).exists().get()) {
                    mMidiOut.sendMidi(0x98, 36 + i,
                            new RGBState(mDrumPadBank.getItemAt(i).color().get()).getMessage());
                } else
                    mMidiOut.sendMidi(0x98, 36 + i, RGBState.OFF.getMessage());
                if (mPlayingNotes.length != 0 && mPlayingNotes != null) {
                    for (PlayingNote n : mPlayingNotes) {
                        mMidiOut.sendMidi(0x98, n.pitch() - drumBank.getOffset(), RGBState.WHITE.getMessage());
                    }
                }
            }

        } else
            mMidiOut.sendSysex(sysex.DAW_NOTE);
    }

    private void initMIDI() {
        mMidiIn0 = mHost.getMidiInPort(0);
        mMidiIn1 = mHost.getMidiInPort(1);
        mMidiOut = mHost.getMidiOutPort(0);

        mMidiOut.sendSysex(sysex.DAW_MODE);
        mMidiOut.sendSysex(sysex.SESSION_LAYOUT);

        mMidiOut.sendMidi(0x90, 99, 3); // Logo Light

        mMidiOut.sendSysex(sysex.PRINT_TO_CLIP_ON);
        mMidiOut.sendSysex(sysex.DAW_VOLUME_FADER);
        mMidiOut.sendSysex(sysex.DAW_PAN_FADER);
        mMidiOut.sendSysex(sysex.DAW_SENDS_FADER);
        mMidiOut.sendSysex(sysex.DAW_DEVICE_FADER);

        mMidiIn0.setSysexCallback(s -> {
            midiCallback(s);
        });
    }

    private void initHardwareControlls() {
        // FUNCTION BUTTONS
        mShiftButton = createCCButton("shift", 90);
        mClearButton = createCCButton("clear", 60);
        mDuplicateButton = createCCButton("duplicate", 50);
        mQuantizeButton = createCCButton("quantize", 40);
        mFixedLengthButton = createCCButton("fixed length", 30);

        // SESSION BUTTON
        mSessionButton = createCCButton("Session", 93);

        // TRANSPORT BUTTONS
        mPlayButton = createCCButton("play", 20);
        mRecButton = createCCButton("record", 10);

        // NAVIGATION BUTTONS
        mUpButton = createCCButton("up", 80);
        mDownButton = createCCButton("down", 70);
        mLeftButton = createCCButton("left", 91);
        mRightButton = createCCButton("right", 92);

        // SCENE BUTTONS
        for (int i = 0; i < 8; i++) {
            mRightButtons[i] = createCCButton("scene" + (7 - i), (i + 1) * 10 + 9);
            mRightLights[i] = createLight("scene_led" + (7 - i), (i + 1) * 10 + 9);
            mRightButtons[i].setBackgroundLight(mRightLights[i]);
        }

        // BUTTON MATRIX
        for (int i = 0; i < 8; i++)
            mLeftLights[i] = createLight("left_led" + i, (i + 1) * 10);
        mLeftLight = createLight("left_arrow_led", 91);
        mRightLight = createLight("right_arrow_led", 92);
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                mButtons[i][j] = createNoteButton("" + (i * 10 + (7 - j)), (1 + i) + (j + 1) * 10);
                mPadLights[i][j] = createLight("led" + (i * 10 + (7 - j)), (1 + i) + (j + 1) * 10);
                mButtons[i][j].setBackgroundLight(mPadLights[i][j]);
            }
        }

        // FADER
        for (int i = 0; i < 32; i++) {
            mFader[i] = mHardwareSurface.createHardwareSlider("fader" + i);
            mFader[i].setAdjustValueMatcher(mMidiIn0.createAbsoluteCCValueMatcher(4, i));
        }

        // BOTTOM ROW BUTTONS
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 8; j++) {
                mBottomButtons[i][j] = createCCButton("bottom" + i + "" + j, (i * 100) + (j + 1));
                mBottomLights[i][j] = createLight("bottom_led" + i + "" + j, (i * 100) + (j + 1));
                mBottomButtons[i][j].setBackgroundLight(mBottomLights[i][j]);
            }
        }
    }

    private HardwareButton createCCButton(String name, int midi) {
        final HardwareButton button = mHardwareSurface.createHardwareButton(name);
        button.pressedAction().setActionMatcher(mMidiIn0.createCCActionMatcher(0, midi, 127));
        button.releasedAction().setActionMatcher(mMidiIn0.createCCActionMatcher(0, midi, 0));

        return button;
    };

    private HardwareButton createNoteButton(String name, int midi) {
        final HardwareButton button = mHardwareSurface.createHardwareButton(name);
        button.pressedAction().setActionMatcher(mMidiIn0.createNoteOnActionMatcher(0, midi));
        button.releasedAction().setActionMatcher(mMidiIn0.createNoteOffActionMatcher(0, midi));

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

    private void initMarkInterested() {
        // Hardware Elements
        mUpButton.isPressed().markInterested();
        mDownButton.isPressed().markInterested();
        mLeftButton.isPressed().markInterested();
        mRightButton.isPressed().markInterested();
        mFixedLengthButton.isPressed().markInterested();
        mQuantizeButton.isPressed().markInterested();
        mSessionButton.isPressed().markInterested();
        mDuplicateButton.isPressed().markInterested();
        mClearButton.isPressed().markInterested();
        mShiftButton.isPressed().markInterested();
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 8; j++) {
                mBottomButtons[i][j].isPressed().markInterested();
            }
        }

        // API Elements
        mApplication.recordQuantizeNoteLength().markInterested();
        mApplication.recordQuantizationGrid().markInterested();

        mTransport.isArrangerRecordEnabled().markInterested();
        mTransport.isPlaying().markInterested();
        mTransport.isClipLauncherOverdubEnabled().markInterested();
        mTransport.isMetronomeEnabled().markInterested();
        mTransport.isClipLauncherOverdubEnabled().markInterested();
        mTransport.clipLauncherPostRecordingAction().markInterested();
        mTransport.getClipLauncherPostRecordingTimeOffset().markInterested();
        mTransport.timeSignature().numerator().markInterested();
        mTransport.playPositionInSeconds().markInterested();

        mCursorClip.getLoopLength().markInterested();
        mCursorClip.clipLauncherSlot().isRecordingQueued().markInterested();
        mCursorClip.clipLauncherSlot().isPlaying().markInterested();
        mCursorClip.clipLauncherSlot().isPlaybackQueued().markInterested();
        mCursorClip.clipLauncherSlot().isRecording().markInterested();
        mCursorClip.clipLauncherSlot().sceneIndex().markInterested();
        mCursorClip.clipLauncherSlot().hasContent().markInterested();

        mCursorTrack.canHoldNoteData().markInterested();
        mCursorTrack.arm().markInterested();

        mDeviceBank.cursorIndex().markInterested();

        mCursorDevice.position().markInterested();
        mCursorDevice.deviceType().markInterested();
        mCursorDevice.hasDrumPads().markInterested();

        mCursorRemoteControlsPage.hasPrevious().markInterested();
        mCursorRemoteControlsPage.hasNext().markInterested();
        for (int i = 0; i < 8; i++) {
            mCursorRemoteControlsPage.getParameter(i).markInterested();
            mCursorRemoteControlsPage.getParameter(i).exists().markInterested();
            mDeviceBank.getDevice(i).exists().markInterested();
        }

        mTrackBank.cursorIndex().markInterested();
        mTrackBank.canScrollBackwards().markInterested();
        mTrackBank.canScrollForwards().markInterested();
        mTrackBank.scrollPosition().markInterested();

        mSessionOverviewTrackBank.cursorIndex().markInterested();
        mSessionOverviewTrackBank.sceneBank().cursorIndex().markInterested();
        mSessionOverviewTrackBank.sceneBank().scrollPosition().markInterested();
        mSessionOverviewTrackBank.sceneBank().canScrollBackwards().markInterested();
        mSessionOverviewTrackBank.sceneBank().canScrollForwards().markInterested();
        mSessionOverviewTrackBank.canScrollBackwards().markInterested();
        mSessionOverviewTrackBank.canScrollForwards().markInterested();

        mSessionOverviewTrackBank.canScrollBackwards().markInterested();
        mSessionOverviewTrackBank.canScrollForwards().markInterested();

        mDrumPadBank.exists().markInterested();
        mDrumPadBank.canScrollBackwards().markInterested();
        mDrumPadBank.canScrollForwards().markInterested();
        mDrumPadBank.scrollPosition().markInterested();

        for (int i = 0; i < 64; i++) {
            mDrumPadBank.getItemAt(i).color().markInterested();
            mDrumPadBank.getItemAt(i).exists().markInterested();
            mDrumPadBank.getItemAt(i).isMutedBySolo().markInterested();
        }

        mSceneBank.canScrollBackwards().markInterested();
        mSceneBank.canScrollForwards().markInterested();
        mSceneBank.cursorIndex().markInterested();
        mSceneBank.scrollPosition().markInterested();

        for (int i = 0; i < 8; i++) {
            Scene s = mSceneBank.getScene(7 - i);
            s.exists().markInterested();
            s.color().markInterested();
        }

        for (int i = 0; i < 8; i++) {
            final Track track = mTrackBank.getItemAt(i);
            final ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
            track.arm().markInterested();
            track.mute().markInterested();
            track.solo().markInterested();
            track.volume().markInterested();
            track.pan().markInterested();
            track.color().markInterested();
            track.exists().markInterested();
            track.name().markInterested();
            track.trackType().markInterested();
            track.isStopped().markInterested();
            track.isQueuedForStop().markInterested();
            for (int j = 0; j < 8; j++) {
                track.sendBank().getItemAt(j).markInterested();
                track.sendBank().getItemAt(j).exists().markInterested();
                track.sendBank().getItemAt(j).sendChannelColor().markInterested();

                final ClipLauncherSlot slot = slotBank.getItemAt(7 - j);
                slot.isSelected().markInterested();
                slot.isPlaybackQueued().markInterested();
                slot.isPlaying().markInterested();
                slot.isRecording().markInterested();
                slot.isRecordingQueued().markInterested();
                slot.isStopQueued().markInterested();
                slot.hasContent().markInterested();
                slot.color().markInterested();
            }
        }

        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < 64; j++) {
                ClipLauncherSlotBank slotBank = mSessionOverviewTrackBank.getItemAt(i).clipLauncherSlotBank();
                ClipLauncherSlot s = slotBank.getItemAt(j);
                s.hasContent().markInterested();
                s.isPlaying().markInterested();
            }
        }

    }

    private void initBottomButtons() {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 8; j++) {
                int jx = j;
                mSessionLayer.bindLightState(() -> RGBState.DARKGREY, mBottomLights[i][j]);
                if (i == 1) {
                    mSessionLayer.bindPressed(mBottomButtons[i][j], () -> mTrackBank.cursorIndex().set(jx));
                    mArmRecLayer.bindPressed(mBottomButtons[i][j], mTrackBank.getItemAt(j).arm());
                    mMuteLayer.bindPressed(mBottomButtons[i][j], mTrackBank.getItemAt(j).mute().toggleAction());
                    // TODO: WORK (GET Solo PREFERECES)
                    mSoloLayer.bindPressed(mBottomButtons[i][j], () -> customSoloAction(jx));
                    mStopLayer.bindPressed(mBottomButtons[i][j], mTrackBank.getItemAt(j).stopAction());
                    bottomTopRowLED(i, j, jx);
                }
            }
        }

        // Layer Toggle
        mSessionLayer.bindPressed(mBottomButtons[0][0], () -> layerManagement.tempLayerSwitch(mArmRecLayer));
        mSessionLayer.bindPressed(mBottomButtons[0][1], () -> layerManagement.tempLayerSwitch(mMuteLayer));
        mSessionLayer.bindPressed(mBottomButtons[0][2], () -> layerManagement.tempLayerSwitch(mSoloLayer));
        mSessionLayer.bindPressed(mBottomButtons[0][3],
                () -> layerManagement.tempLayerSwitch(mVolumeLayer, sysex.DAW_VOLUME));
        mSessionLayer.bindPressed(mBottomButtons[0][4],
                () -> layerManagement.tempLayerSwitch(mPanLayer, sysex.DAW_PAN));
        mSessionLayer.bindPressed(mBottomButtons[0][5],
                () -> layerManagement.tempLayerSwitch(mSendsLayer, sysex.DAW_SENDS));
        mSessionLayer.bindPressed(mBottomButtons[0][6],
                () -> layerManagement.tempLayerSwitch(mDeviceLayer, sysex.DAW_DEVICE));
        mSessionLayer.bindPressed(mBottomButtons[0][7], () -> layerManagement.tempLayerSwitch(mStopLayer));
        for (int i = 0; i < 8; i++) {
            mSessionLayer.bindReleased(mBottomButtons[0][i], () -> layerManagement.tempLayerSwitchRelease());
        }

        // Light
        setBottomLED(mArmRecLayer, 0, RGBState.RED, true);
        setBottomLED(mMuteLayer, 1, RGBState.ORANGE, true);
        setBottomLED(mSoloLayer, 2, RGBState.YELLOW, true);
        setBottomLED(mVolumeLayer, 3, RGBState.WHITE);
        setBottomLED(mPanLayer, 4, RGBState.PURPLE);
        setBottomLED(mSendsLayer, 5, RGBState.GREEN, true);
        setBottomLED(mDeviceLayer, 6, RGBState.BLUE);
        setBottomLED(mStopLayer, 7, RGBState.RED);
    }

    private void customSoloAction(int jx) {
        Track t = mTrackBank.getItemAt(jx);
        if (!mShiftButton.isPressed().getAsBoolean()) {
            if (t.solo().get())
                t.solo().set(false);
            else
                t.solo().set(true);
        } else
            mTrackBank.getItemAt(jx).solo().toggle();
    }

    private void bottomTopRowLED(int i, int j, int jx) {
        final Track track = mTrackBank.getItemAt(jx);

        mSessionLayer.bindLightState(() -> {
            if (mTrackBank.cursorIndex().get() == jx) {
                if (new RGBState(track.color().get()).getMessage() == RGBState.GREY.getMessage())
                    return RGBState.WHITE;
                return new RGBState(track.color().get());
            }
            if (track.trackType().get() == "Master" || track.trackType().get() == "Effect")
                return RGBState.GREY;
            if (track.exists().get())
                return RGBState.DARKGREY;
            return RGBState.OFF;
        }, mBottomLights[i][j]);
        mArmRecLayer.bindLightState(() -> {
            if (track.arm().getAsBoolean())
                return RGBState.RED;
            if (track.exists().get()
                    && track.trackType().get() != "Master"
                    && track.trackType().get() != "Effect")
                return RGBState.DARKRED;
            return RGBState.OFF;
        }, mBottomLights[i][j]);
        mSoloLayer.bindLightState(() -> {
            if (track.solo().getAsBoolean())
                return RGBState.YELLOW;
            if (track.exists().get())
                return new RGBState(15);
            return RGBState.OFF;
        }, mBottomLights[i][j]);
        mMuteLayer.bindLightState(() -> {
            if (track.mute().getAsBoolean())
                return RGBState.ORANGE;
            if (track.exists().get())
                return new RGBState(11);
            return RGBState.OFF;
        }, mBottomLights[i][j]);
        mStopLayer.bindLightState(() -> {
            if (track.trackType().get() != "Master"
                    && track.exists().get()
                    && track.isQueuedForStop().get())
                return RGBState.RED_BLINK;
            if (track.trackType().get() != "Master"
                    && track.exists().get()
                    && !track.isStopped().get())
                return RGBState.RED_PULS;
            return RGBState.OFF;
        }, mBottomLights[i][j]);
    }

    private void setBottomLED(Layer l, int i, RGBState c, boolean shift) {
        mSessionLayer.bindLightState(() -> {
            if (l.isActive())
                return c;
            return RGBState.DARKGREY;
        }, mBottomLights[0][i]);
    }

    private void setBottomLED(Layer l, int i, RGBState c) {
        mSessionLayer.bindLightState(() -> {
            if (mShiftButton.isPressed().get())
                return RGBState.DARKGREY;
            if (l.isActive())
                return c;
            return RGBState.DARKGREY;
        }, mBottomLights[0][i]);
    }

    private void initFunctionButtons() {
        initFixedLenghtButton();

        mSessionLayer.bindLightState(() -> mSessionButton.isPressed().get() ? RGBState.OFF : RGBState.DARKGREY, mLeftLights[5]);
        mSessionLayer.bindLightState(() -> mSessionButton.isPressed().get() ? RGBState.OFF : RGBState.DARKGREY, mLeftLights[4]);
        mSessionLayer.bindLightState(() -> mSessionButton.isPressed().get() ? RGBState.OFF : RGBState.DARKGREY, mLeftLights[3]);
    }

    // TODO: Capture Midi concept
    private void initTransport() {
        mSessionLayer.bindPressed(mPlayButton, () -> {
            mTransport.playStartPosition().set(0.0); // Usefull?
            mTransport.play();
        });
        mSessionLayer.bindPressed(mRecButton, () -> {
            if (mCursorTrack.arm().get() && !mCursorClip.clipLauncherSlot().isRecording().get())
                mCursorTrack.recordNewLauncherClip(mCursorSlotIndex);
            else if (mCursorClip.clipLauncherSlot().isRecording().get())
                mCursorClip.launch();
            mCursorTrack.selectSlot(mCursorSlotIndex);
        });

        mSessionLayer.bindLightState(() -> mTransport.isPlaying().get() ? RGBState.GREEN : RGBState.DARKGREY,
                mLeftLights[1]);
        mSessionLayer.bindLightState(() -> {
            if (mCursorClip.clipLauncherSlot().isPlaybackQueued().get()
                    || mCursorClip.clipLauncherSlot().isRecordingQueued().get())
                return RGBState.RED_BLINK;
            else if (mCursorClip.clipLauncherSlot().isRecording().get())
                return RGBState.RED;
            else if (mCursorTrack.arm().get())
                return RGBState.DARKRED;
            return RGBState.DARKGREY;
        }, mLeftLights[0]);
    }

    private void initNavigation() {
        mSessionLayer.bindPressed(mSessionButton, () -> layerManagement.switchLayer(mSessionLayer));

        bindNavigationAction(mUpButton, () -> mSceneBank.scrollBackwards(), () -> drumBank.scroll("UP_PAGE"),
                () -> mSessionOverviewTrackBank.sceneBank().scrollBackwards());
        bindNavigationAction(mDownButton, () -> mSceneBank.scrollForwards(), () -> drumBank.scroll("DOWN_PAGE"),
                () -> mSessionOverviewTrackBank.sceneBank().scrollForwards());
        bindNavigationAction(mLeftButton, () -> mTrackBank.scrollBackwards(), () -> drumBank.scroll("DOWN"),
                () -> mSessionOverviewTrackBank.scrollBackwards());
        bindNavigationAction(mRightButton, () -> mTrackBank.scrollForwards(), () -> drumBank.scroll("UP"),
                () -> mSessionOverviewTrackBank.scrollForwards());

        bindNavigationLight(mLeftLight, mTrackBank.canScrollBackwards(), mDrumPadBank.canScrollBackwards(),
                mSessionOverviewTrackBank.canScrollBackwards());
        bindNavigationLight(mRightLight, mTrackBank.canScrollForwards(), mDrumPadBank.canScrollForwards(),
                mSessionOverviewTrackBank.canScrollForwards());
        bindNavigationLight(mLeftLights[7], mSceneBank.canScrollBackwards(), mDrumPadBank.canScrollForwards(),
                mSessionOverviewTrackBank.sceneBank().canScrollBackwards());
        bindNavigationLight(mLeftLights[6], mSceneBank.canScrollForwards(), mDrumPadBank.canScrollBackwards(),
                mSessionOverviewTrackBank.sceneBank().canScrollForwards());
    }

    private void bindNavigationAction(HardwareButton button, Runnable defaultAction, Runnable noteModeAction,
            Runnable sessionOverviewAction) {
        mSessionLayer.bindPressed(button, () -> {
            if (mSessionButton.isPressed().get())
                longPress.pressedAction(sessionOverviewAction);
            else if (isNoteModeActive)
                longPress.pressedAction(noteModeAction);
            else
                longPress.pressedAction(defaultAction);
        });
        mSessionLayer.bindReleased(button, () -> longPress.releasedAction());
    }

    private void bindNavigationLight(MultiStateHardwareLight light, BooleanValue defaultValue,
            BooleanValue noteModeValue, BooleanValue sessionOverviewValue) {
        mSessionLayer.bindLightState(
                () -> {
                    if (mSessionButton.isPressed().get() && sessionOverviewValue.get()
                            || !mSessionButton.isPressed().get() && !isNoteModeActive && defaultValue.get()
                            || isNoteModeActive && noteModeValue.get())
                        return RGBState.WHITE;
                    else
                        return RGBState.DARKGREY;
                }, light);
    }

    private void initScenes() {
        mSceneBank = mTrackBank.sceneBank();
        mSceneBank.setIndication(true);

        for (int i = 0; i < 8; i++) {
            Scene s = mSceneBank.getScene(7 - i);

            mSessionLayer.bindPressed(mRightButtons[i], () -> {
                // ALEX FEATURE
                // if (mShiftButton.isPressed().get())
                //     s.launchWithOptions("none", "continue_immediately");
                // else if (!mSessionButton.isPressed().get())
                    s.launch();
            });

            // ALEX FEATURE
            // mSessionLayer.bindReleased(mRightButtons[i], () -> {
            //     if (mShiftButton.isPressed().get())
            //         s.launchLastClipWithOptions("none", "continue_immediately");
            // });
        }

        for (int i = 0; i < 8; i++) {
            int ix = i;
            mSessionLayer.bindLightState(() -> {
                // TODO: implement SessionOverviewLayer!!!
                if (mSessionButton.isPressed().get())
                    return RGBState.OFF;
                if (mSceneBank.getScene(7 - ix).exists().get())
                    return RGBState.DARKGREY;
                return RGBState.OFF;
            }, mRightLights[ix]);
        }
    }

    // TODO: Make better
    private void initPadMatrix() {
        for (int i = 0; i < 8; i++) {
            final Track track = mTrackBank.getItemAt(i);
            final ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
            slotBank.setIndication(true);
            for (int j = 0; j < 8; j++) {
                final int ix = i;
                final int jx = j;
                final ClipLauncherSlot slot = slotBank.getItemAt(7 - j);
                mSessionLayer.bindPressed(mButtons[i][j], () -> {
                    if (mSessionButton.isPressed().get()) {
                        mTrackBank.scrollPosition().set(ix * 8);
                        mSceneBank.scrollPosition().set((7 - jx) * 8);
                        return;
                    }
                    slot.launch();
                    slot.select();

                    // ALEX FEATURE
                    // if (mShiftButton.isPressed().get())
                    //     slot.launchWithOptions("none", "continue_immediately");
                    // else
                    //     slot.launch();
                    
                });

                // ALEX FEATURE
                // mSessionLayer.bindReleased(mButtons[i][j], () -> {
                //     if (mShiftButton.isPressed().get())
                //         track.launchLastClipWithOptions("none", "continue_immediately");
                // });

                mSessionLayer.bindLightState(() -> {
                    if (mSessionButton.isPressed().get()) {
                        updateSessionOverview();
                        if (mSessionOverview[ix][jx] == 3)
                            return RGBState.BLUE;
                        if (mSessionOverview[ix][jx] == 2)
                            return RGBState.GREEN_PULS;
                        if (mSessionOverview[ix][jx] == 1)
                            return RGBState.WHITE;
                        else
                            return RGBState.OFF;
                    }

                    if (slot.isRecordingQueued().get())
                        return RGBState.RED_BLINK;
                    else if (slot.isRecording().get() && slot.isPlaybackQueued().get())
                        return RGBState.RED_BLINK;
                    else if (slot.isRecording().get())
                        return RGBState.RED_PULS;
                    else if (slot.isPlaybackQueued().get())
                        return RGBState.GREEN_BLINK;
                    else if (slot.isPlaying().get())
                        return RGBState.GREEN_PULS;
                    else if (slot.hasContent().get())
                        return new RGBState(slot.color().get());
                    else if (track.arm().get())
                        return RGBState.TRACK_ARM;
                    return RGBState.OFF;
                }, mPadLights[i][j]);
            }
        }
    }

    private void initLayers() {
        mSessionLayer = new Layer(mLayers, "Session");
        mArmRecLayer = new Layer(mLayers, "Arm Record");
        mMuteLayer = new Layer(mLayers, "Mute");
        mSoloLayer = new Layer(mLayers, "Solo");
        mVolumeLayer = new Layer(mLayers, "Volume");
        mPanLayer = new Layer(mLayers, "Pan");
        mSendsLayer = new Layer(mLayers, "Sends");
        mDeviceLayer = new Layer(mLayers, "Device");
        mStopLayer = new Layer(mLayers, "Stop");
        mFixedLengthLayer = new Layer(mLayers, "Fixed Lenght");
        mClearLayer = new Layer(mLayers, "Clear");
        mDuplicateLayer = new Layer(mLayers, "Duplicate");
        mDoubleLayer = new Layer(mLayers, "Double");
        mSessionOverviewLayer = new Layer(mLayers, "Session Overview");
        mQuantizeLayer = new Layer(mLayers, "Quantize");
        mShiftLayer = new Layer(mLayers, "Shift");
    }

    private void initVolumeLayer() {
        mVolumeLayer.bindPressed(mUpButton, () -> {
        });
        mVolumeLayer.bindPressed(mDownButton, () -> {
        });

        mVolumeLayer.bindLightState(() -> RGBState.OFF, mLeftLights[7]);
        mVolumeLayer.bindLightState(() -> RGBState.OFF, mLeftLights[6]);

        for (int i = 0; i < 8; i++) {
            final Track track = mTrackBank.getItemAt(i);
            final Parameter parameter = track.volume();
            mVolumeLayer.bind(mFader[i], parameter);
            mVolumeLayer.bindPressed(mRightButtons[7 - i], () -> {
            });
            mVolumeLayer.bindLightState(() -> RGBState.OFF, mRightLights[7 - i]);
        }
    }

    private void initPanLayer() {
        mPanLayer.bindPressed(mUpButton, () -> {
        });
        mPanLayer.bindPressed(mDownButton, () -> {
        });

        mPanLayer.bindLightState(() -> RGBState.OFF, mLeftLights[7]);
        mPanLayer.bindLightState(() -> RGBState.OFF, mLeftLights[6]);

        for (int i = 0; i < 8; i++) {
            int ix = i;
            final Track track = mTrackBank.getItemAt(ix);
            final Parameter parameter = track.pan();
            mPanLayer.bind(mFader[ix + 8], parameter);
            mPanLayer.bindPressed(mRightButtons[7 - i], () -> {
            });
            mPanLayer.bindLightState(() -> RGBState.OFF, mRightLights[7 - i]);
        }
    }

    private void initSendsLayer() {
        mSendsLayer.bindPressed(mUpButton, () -> {
        });
        mSendsLayer.bindPressed(mDownButton, () -> {
        });

        mSendsLayer.bindLightState(() -> RGBState.OFF, mLeftLights[7]);
        mSendsLayer.bindLightState(() -> RGBState.OFF, mLeftLights[6]);

        for (int i = 0; i < 8; i++) {
            int ix = i;
            final Track track = mTrackBank.getItemAt(i);
            mSend[i] = track.sendBank().getItemAt(0);
            mSendsLayer.bindPressed(mRightButtons[7 - i], () -> {
                if (mTrackBank.getItemAt(ix).sendBank().getItemAt(ix).exists().getAsBoolean())
                    mSendIndex = ix;
                for (int j = 0; j < 8; j++) {
                    if (mActiveBindings[7 - j] != null)
                        mActiveBindings[7 - j].removeBinding();
                    mActiveBindings[7 - j] = mTrackBank.getItemAt(7 - j).sendBank().getItemAt(mSendIndex)
                            .addBinding(mFader[(7 - j) + 16]);
                }
                isTrackBankNavigated = true;
                mHost.scheduleTask(() -> isTrackBankNavigated = false, (long) 100.0);

                mMidiOut.sendSysex(sysex.DAW_FADER_ON + sysex.DAW_SENDS);

            });
            mSendsLayer.bindLightState(() -> {
                RGBState r = new RGBState(track.sendBank().getItemAt(ix).sendChannelColor().get());
                if (mSendIndex == ix)
                    return r.getMessage() != 0 ? r : RGBState.WHITE;
                else if (track.sendBank().getItemAt(ix).exists().get())
                    return RGBState.DARKGREY;
                return RGBState.OFF;
            }, mRightLights[7 - i]);
        }

    }

    private void initDeviceLayer() {
        mDeviceLayer.bindPressed(mUpButton, () -> mCursorRemoteControlsPage.selectPrevious());
        mDeviceLayer.bindPressed(mDownButton, () -> mCursorRemoteControlsPage.selectNext());

        mDeviceLayer.bindLightState(
                () -> mCursorRemoteControlsPage.hasPrevious().get() ? RGBState.WHITE : RGBState.DARKGREY,
                mLeftLights[7]);
        mDeviceLayer.bindLightState(
                () -> mCursorRemoteControlsPage.hasNext().get() ? RGBState.WHITE : RGBState.DARKGREY, mLeftLights[6]);

        for (int i = 0; i < 8; i++) {
            final int ix = i;
            final Parameter parameter = mCursorRemoteControlsPage.getParameter(i);
            mDeviceLayer.bind(mFader[i + 24], parameter);
            mDeviceLayer.bindPressed(mRightButtons[7 - i], () -> {
                mDeviceBank.getItemAt(ix).selectInEditor();// set(ix);
                mDeviceBank.getItemAt(ix).isRemoteControlsSectionVisible().set(true);
                isTrackBankNavigated = true;
                mHost.scheduleTask(() -> isTrackBankNavigated = false, (long) 100.0);

            });
            mDeviceLayer.bindLightState(() -> {
                if (mCursorDevice.position().get() == ix)
                    return RGBState.WHITE;
                else if (mDeviceBank.getItemAt(ix).exists().get())
                    return RGBState.DARKGREY;
                return RGBState.OFF;
            }, mRightLights[7 - i]);
        }
    }

    private void initFixedLengthLayer() {
        for (int i = 0; i < 8; i++) {
            int ix = i;
            mFixedLengthLayer.bindPressed(mBottomButtons[1][ix], () -> {
                changeFixedLeghtBars(ix + 1);
            });
            mFixedLengthLayer.bindLightState(() -> {
                if (FIXED_LENGTH_VALUE >= (ix + 1))
                    return RGBState.BLUE_PULS;
                else
                    return RGBState.DARKGREY_PULS;
            }, mBottomLights[1][ix]);
        }
    }

    private void changeFixedLeghtBars(int bars) {
        FIXED_LENGTH_VALUE = bars;
        int num = mTransport.timeSignature().numerator().get();
        mTransport.getClipLauncherPostRecordingTimeOffset().set(FIXED_LENGTH_VALUE * num);
    }

    private void switchFixedLenghtMode() {
        if (mTransport.clipLauncherPostRecordingAction().get() != "off")
            mTransport.clipLauncherPostRecordingAction().set("off");
        else
            mTransport.clipLauncherPostRecordingAction().set("play_recorded");
    }

    private void initFixedLenghtButton() {
        mSessionLayer.bindPressed(mFixedLengthButton, () -> {
            longPress.delayedAction(() -> mFixedLengthLayer.activate());
        });
        mSessionLayer.bindReleased(mFixedLengthButton, () -> {
            longPress.releasedAction();
            if (mFixedLengthLayer.isActive())
                mFixedLengthLayer.deactivate();
            else
                switchFixedLenghtMode();
        });
        mSessionLayer.bindLightState(() -> {
            if (mTransport.clipLauncherPostRecordingAction().get() != "off")
                return RGBState.BLUE;
            else
                return RGBState.DARKGREY;
        }, mLeftLights[2]);
    }

    private void initClearLayer() {
        mSessionLayer.bindPressed(mClearButton, () -> {
            if (isNoteModeActive)
                mCursorClip.clipLauncherSlot().deleteObject();
            else
                mClearLayer.activate();
        });
        mSessionLayer.bindReleased(mClearButton, () -> {
            if (!isNoteModeActive)
                mClearLayer.deactivate();
        });

        for (int i = 0; i < 8; i++) {
            final Track track = mTrackBank.getItemAt(i);
            final ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
            mClearLayer.bindPressed(mBottomButtons[1][i], track.deleteObjectAction());
            for (int j = 0; j < 8; j++) {
                final ClipLauncherSlot slot = slotBank.getItemAt(7 - j);
                mClearLayer.bindPressed(mButtons[i][j], slot.deleteObjectAction());
            }
        }
        mClearLayer.bindLightState(() -> RGBState.WHITE, mLeftLights[5]);

    }

    private void initDuplicateLayer() {
        mSessionLayer.bindPressed(mDuplicateButton, () -> {
            if (isNoteModeActive && mShiftButton.isPressed().get()) {
                mCursorClip.duplicateContent();
                mCursorClip.getLoopLength().set(mCursorClip.getLoopLength().get() * 2);
            } else if (isNoteModeActive)
                mCursorClip.clipLauncherSlot().duplicateClip();
            else {
                mDuplicateLayer.activate();
            }
        });
        mSessionLayer.bindReleased(mDuplicateButton, () -> {
            if (!isNoteModeActive)
                mDuplicateLayer.deactivate();
        });

        for (int i = 0; i < 8; i++) {
            final Track track = mTrackBank.getItemAt(i);
            final ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
            mDuplicateLayer.bindPressed(mBottomButtons[1][i], () -> track.duplicate());
            for (int j = 0; j < 8; j++) {
                final ClipLauncherSlot slot = slotBank.getItemAt(7 - j);
                if (mShiftButton.isPressed().get()) {
                    mDuplicateLayer.bindPressed(mButtons[i][j], () -> {
                        slot.select();
                        mCursorClip.duplicateContent();
                        mCursorClip.getLoopLength().set(mCursorClip.getLoopLength().get() * 2);
                    });
                } else
                    mDuplicateLayer.bindPressed(mButtons[i][j], () -> slot.duplicateClip());
            }
        }

        mDuplicateLayer.bindLightState(() -> RGBState.WHITE, mLeftLights[4]);
    }

    private void initDoubleLayer() {
        for (int i = 0; i < 8; i++) {
            final Track track = mTrackBank.getItemAt(i);
            final ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
            for (int j = 0; j < 8; j++) {
                final ClipLauncherSlot slot = slotBank.getItemAt(7 - j);
                mDoubleLayer.bindPressed(mButtons[i][j], () -> {
                    slot.select();
                    mCursorClip.duplicateContent();
                    mCursorClip.getLoopLength().set(mCursorClip.getLoopLength().get() * 2);
                });
            }
        }

        mDoubleLayer.bindLightState(() -> RGBState.RED, mLeftLights[4]);
    }

    private void initQuantizeLayer() {
        mSessionLayer.bindPressed(mQuantizeButton, () -> {
            if (isNoteModeActive)
                mCursorClip.quantize(1.0);
            else
                mQuantizeLayer.activate();
        });
        mSessionLayer.bindReleased(mQuantizeButton, () -> mQuantizeLayer.deactivate());

        for (int i = 0; i < 8; i++) {
            final Track track = mTrackBank.getItemAt(i);
            final ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
            for (int j = 0; j < 8; j++) {
                final ClipLauncherSlot slot = slotBank.getItemAt(7 - j);
                mQuantizeLayer.bindPressed(mButtons[i][j], () -> {
                    slot.select();
                    mCursorClip.quantize(1.0);
                });
            }
        }

        mQuantizeLayer.bindLightState(() -> RGBState.WHITE, mLeftLights[3]);
    }

    private void initShiftLayer() {
        mSessionLayer.bindPressed(mShiftButton, () -> {
            if (!mSoloLayer.isActive())
                mShiftLayer.activate();
        });
        mSessionLayer.bindReleased(mShiftButton, () -> mShiftLayer.deactivate());

        removeAllFuntionAndLightsForLayer(mShiftLayer);

        mShiftLayer.bindPressed(mPlayButton, mTransport.playAction());
        // TODO: RecordButton and Light überarbeiten
        mShiftLayer.bindPressed(mRecButton, mTransport.isClipLauncherOverdubEnabled().toggleAction());
        mShiftLayer.bindPressed(mBottomButtons[0][0], () -> mApplication.undo());
        mShiftLayer.bindPressed(mBottomButtons[0][1], () -> mApplication.redo());
        mShiftLayer.bindPressed(mBottomButtons[0][2], () -> mTransport.isMetronomeEnabled().toggle());
        mShiftLayer.bindPressed(mBottomButtons[0][5], () -> mTransport.tapTempo());
        mShiftLayer.bindPressed(mQuantizeButton, () -> {
            if (mApplication.recordQuantizationGrid().get() == "OFF")
                mApplication.recordQuantizationGrid().set(QUANTIZATION_GRID_SIZE);
            else
                mApplication.recordQuantizationGrid().set("OFF");
        });

        for (int i = 0; i < 8; i++) {
            final Track track = mTrackBank.getItemAt(i);
            final ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
            for (int j = 0; j < 8; j++) {
                final ClipLauncherSlot slot = slotBank.getItemAt(7 - j);
                mShiftLayer.bindPressed(mButtons[i][j], () -> {
                    //slot.select();

                    if (mDuplicateButton.isPressed().get()) {
                        mCursorClip.duplicateContent();
                        mCursorClip.getLoopLength().set(mCursorClip.getLoopLength().get() * 2);
                    } else {
                        slot.launchWithOptions("none", "continue_immediately");
                    }
                });
                mShiftLayer.bindReleased(mButtons[i][j], () -> {
                    if (mShiftButton.isPressed().get())
                        track.launchLastClipWithOptions("none", "continue_immediately");
                });

            }
            // ALEX FEATURE
            // Scene s = mSceneBank.getScene(7 - i);
            // mShiftLayer.bindPressed(mRightButtons[i], () -> {
            //     s.launchWithOptions("none", "continue_immediately");
            // });
            // mShiftLayer.bindReleased(mRightButtons[i], () -> {
            //     s.launchLastClipWithOptions("none", "continue_immediately");
            // });
        }


        

        // Lights
        mShiftLayer.bindLightState(() -> {
            if (mTransport.isClipLauncherOverdubEnabled().get())
                return RGBState.ORANGE;
            return RGBState.WHITE;
        }, mLeftLights[0]);
        mShiftLayer.bindLightState(() -> mTransport.isPlaying().get() ? RGBState.PURPLE : RGBState.WHITE,
                mLeftLights[1]);
        mShiftLayer.bindLightState(() -> RGBState.WHITE, mBottomLights[0][0]);
        mShiftLayer.bindLightState(() -> RGBState.WHITE, mBottomLights[0][1]);
        mShiftLayer.bindLightState(() -> RGBState.WHITE, mBottomLights[0][5]);
        mShiftLayer.bindLightState(() -> {
            if (mTransport.isMetronomeEnabled().get())
                return RGBState.BLUE;
            else
                return RGBState.WHITE;
        }, mBottomLights[0][2]);

        mShiftLayer.bindLightState(() -> {
            if (mApplication.recordQuantizationGrid().get() == "OFF")
                return RGBState.RED;
            else
                return RGBState.GREEN;
        }, mLeftLights[3]);
        mShiftLayer.bindLightState(() -> RGBState.WHITE, mLeftLights[4]);

    }

    private void removeAllFuntionAndLightsForLayer(Layer layer) {
        for (int i = 0; i < 8; i++) {
            layer.bindPressed(mRightButtons[i], () -> {
            });
            layer.bindPressed(mBottomButtons[0][i], () -> {
            });
            layer.bindPressed(mBottomButtons[1][i], () -> {
            });

            layer.bindLightState(() -> RGBState.OFF, mRightLights[i]);
            layer.bindLightState(() -> RGBState.OFF, mBottomLights[0][i]);
            layer.bindLightState(() -> RGBState.OFF, mBottomLights[1][i]);
            layer.bindLightState(() -> RGBState.OFF, mLeftLights[i]);
        }
        layer.bindPressed(mUpButton, () -> {
        });
        layer.bindPressed(mDownButton, () -> {
        });
        layer.bindPressed(mLeftButton, () -> {
        });
        layer.bindPressed(mRightButton, () -> {
        });
        layer.bindPressed(mFixedLengthButton, () -> {
        });
        layer.bindPressed(mClearButton, () -> {
        });
        layer.bindPressed(mRecButton, () -> {
        });

        layer.bindLightState(() -> RGBState.OFF, mLeftLight);
        layer.bindLightState(() -> RGBState.OFF, mRightLight);
    }

    private void initSessionOverview() {
        sessionOverview();
    }

    private void sessionOverview() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                mSessionOverview[i][j] = 0;
            }
        }
    }

    private void updateSessionOverview() {
        sessionOverview();
        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < 64; j++) {
                ClipLauncherSlotBank slotBank = mSessionOverviewTrackBank.getItemAt(i).clipLauncherSlotBank();
                ClipLauncherSlot s = slotBank.getItemAt(j);
                if (s.isPlaying().get() && mSessionOverview[(i / 8) % 8][7 - (j / 8) % 8] != 3)
                    mSessionOverview[(i / 8) % 8][7 - (j / 8) % 8] = 2;
                else if (s.hasContent().get() && mSessionOverview[(i / 8) % 8][7 - (j / 8) % 8] != 2)
                    mSessionOverview[(i / 8) % 8][7 - (j / 8) % 8] = 1;
            }
        }

        mSessionOverview[(mTrackBank.scrollPosition().get() / 8) % 8][7
                - (mSceneBank.scrollPosition().get() / 8) % 8] = 3;
        mSessionOverview[((mTrackBank.scrollPosition().get() + 7) / 8) % 8][7
                - (mSceneBank.scrollPosition().get() / 8) % 8] = 3;
        mSessionOverview[(mTrackBank.scrollPosition().get() / 8) % 8][7
                - ((mSceneBank.scrollPosition().get() + 7) / 8) % 8] = 3;
        mSessionOverview[((mTrackBank.scrollPosition().get() + 7) / 8) % 8][7
                - ((mSceneBank.scrollPosition().get() + 7) / 8) % 8] = 3;

    }

    @Override
    public void exit() {
        mMidiOut.sendSysex(sysex.STANDALONE_MODE);
        mHost.showPopupNotification("Launchpad Pro Mk3 exited...");
    }

    @Override
    public void flush() {
        mHardwareSurface.updateHardware();
        midiCallback("flush");
    }

    // Sysex
    private Sysex sysex = new Sysex();

    // Print to Clip Constants
    public PrintToClip printToClip = new PrintToClip();

    // Long Press Action
    private LongPress longPress = new LongPress(this);
    public Boolean isTrackBankNavigated = false;

    // DrumBank
    public DrumBank drumBank = new DrumBank(this);

    // Midi Management
    // private MidiManagement midiManagement = new MidiManagement(this);

    // DIVERSE
    private int FIXED_LENGTH_VALUE = 1;

    public Boolean isNoteModeActive = false;
    private int mCursorSlotIndex = 0;
    // private Boolean isTemporarySwitch = false;

    // API Objects
    private HardwareSurface mHardwareSurface;
    private Application mApplication;
    public ControllerHost mHost;
    public MidiIn mMidiIn0;
    public MidiIn mMidiIn1;
    public MidiOut mMidiOut;
    public NoteInput mNoteInput;
    public NoteInput mDrumInput;
    public DrumPadBank mDrumPadBank;
    public PlayingNote[] mPlayingNotes;

    private Transport mTransport;
    public TrackBank mTrackBank;
    private TrackBank mSessionOverviewTrackBank;
    private int mSessionOverview[][] = new int[8][8];
    private SceneBank mSceneBank;
    public AbsoluteHardwareControlBinding[] mActiveBindings = new AbsoluteHardwareControlBinding[8];
    public CursorTrack mCursorTrack;
    private DeviceBank mDeviceBank;
    private CursorDevice mCursorDevice;
    public CursorRemoteControlsPage mCursorRemoteControlsPage;
    public Clip mCursorClip;
    private Send[] mSend = new Send[8];
    public int mSendIndex = 0;
    private String QUANTIZATION_GRID_SIZE = "1/32";

    // Layers
    public Layers mLayers = new Layers(this);
    public Layer mSessionLayer;
    public Layer mArmRecLayer;
    public Layer mMuteLayer;
    public Layer mSoloLayer;
    public Layer mVolumeLayer;
    public Layer mPanLayer;
    public Layer mSendsLayer;
    public Layer mDeviceLayer;
    public Layer mStopLayer;
    public Layer mFixedLengthLayer;
    public Layer mDuplicateLayer;
    public Layer mDoubleLayer;
    public Layer mClearLayer;
    public Layer mSessionOverviewLayer;
    public Layer mQuantizeLayer;
    public Layer mShiftLayer;

    public LayerManagement layerManagement = new LayerManagement(this);

    // Hardware Proxy
    private HardwareButton[][] mButtons = new HardwareButton[8][8];
    private MultiStateHardwareLight[][] mPadLights = new MultiStateHardwareLight[8][8];
    private HardwareButton[] mRightButtons = new HardwareButton[8];
    private MultiStateHardwareLight[] mRightLights = new MultiStateHardwareLight[8];
    private HardwareButton mUpButton;
    private HardwareButton mDownButton;
    private HardwareButton mLeftButton;
    private MultiStateHardwareLight mLeftLight;
    private HardwareButton mRightButton;
    private MultiStateHardwareLight mRightLight;
    private HardwareButton mSessionButton;
    private HardwareButton mPlayButton;
    private HardwareButton mRecButton;
    private HardwareButton mShiftButton;
    private HardwareButton mClearButton;
    private HardwareButton mDuplicateButton;
    private HardwareButton mQuantizeButton;
    private HardwareButton mFixedLengthButton;
    private MultiStateHardwareLight[] mLeftLights = new MultiStateHardwareLight[8];
    private HardwareButton[][] mBottomButtons = new HardwareButton[2][8];
    private MultiStateHardwareLight[][] mBottomLights = new MultiStateHardwareLight[2][8];
    public HardwareSlider[] mFader = new HardwareSlider[32];
}
