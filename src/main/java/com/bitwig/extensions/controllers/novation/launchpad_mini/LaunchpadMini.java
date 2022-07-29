package com.bitwig.extensions.controllers.novation.launchpad_mini;

import java.util.List;
import java.util.function.Consumer;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.Application;
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
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extension.controller.api.RelativePosition;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class LaunchpadMini extends ControllerExtension {

    private static final int NOTES_MIDI_CHANNEL = 0x9f;
    private static final int DRUM_MIDI_CHANNEL = 0x98;
    // SYSEX
    private final String DAW_MODE = "F0 00 20 29 02 0D 10 01 F7";
    private final String STANDALONE_MODE = "F0 00 20 29 02 0D 10 00 F7";

    private final String SESSION_LAYOUT = "F0 00 20 29 02 0D 00 00 00 00 F7";
    private final String SESSION_MODE_PREFIX = "f0002029020d00000000";

    private final String SESSION_MODE_LED = "F0 00 20 29 02 0D 14 14 01 F7";
    private final String MIXER_MODE_LED = "F0 00 20 29 02 0D 14 24 01 F7";

    private final String DRUM_MODE = "F0 00 20 29 02 0D 0F 01 F7";
    private final String NOTE_MODE = "F0 00 20 29 02 0D 0F 00 F7";

    private final String DAW_VOLUME_FADER = "F0 00 20 29 02 0D 01 00 00 00 00 00 00 01 00 01 00 02 00 02 00 03 00 03 00 04 00 04 00 05 00 05 00 06 00 06 00 07 00 07 00 F7";
    private final String DAW_PAN_FADER = "F0 00 20 29 02 0D 01 01 01 00 01 08 00 01 01 09 00 02 01 0A 00 03 01 0B 00 04 01 0C 00 05 01 0D 00 06 01 0E 00 07 01 0F 00 F7";
    private final String DAW_SEND_A_FADER = "F0 00 20 29 02 0D 01 02 00 00 00 10 00 01 00 11 00 02 00 12 00 03 00 13 00 04 00 14 00 05 00 15 00 06 00 16 00 07 00 17 00 F7";
    private final String DAW_SEND_B_FADER = "F0 00 20 29 02 0D 01 03 00 00 00 18 00 01 00 19 00 02 00 1A 00 03 00 1B 00 04 00 1C 00 05 00 1D 00 06 00 1E 00 07 00 1F 00 F7";

    private final String[] DAW_FADER_MODES = {
            DAW_VOLUME_FADER,
            DAW_PAN_FADER,
            DAW_SEND_A_FADER,
            DAW_SEND_B_FADER
    };

    private final String DAW_FADER_ON = "F0 00 20 29 02 0D 00 0D F7";
    private final String DAW_FADER_OFF = "F0 00 20 29 02 0D 00 00 F7";

    public LaunchpadMini(ControllerExtensionDefinition definition, ControllerHost host) {
        super(definition, host);
    }

    @Override
    public void init() {
        mHost = getHost();
        mApplication = mHost.createApplication();
        mHardwareSurface = mHost.createHardwareSurface();

        mHardwareSurface.setPhysicalSize(300, 300);

        initMIDI();

        mNoteInput = mMidiIn1.createNoteInput("Note", "90????", "D0??", "80????");
        mDrumInput = mMidiIn0.createNoteInput("Drum", "98????");

        mTransport = mHost.createTransport();
        mTransport.isArrangerRecordEnabled().markInterested();
        mTransport.isPlaying().markInterested();
        mTransport.isClipLauncherOverdubEnabled().markInterested();
        mTransport.isMetronomeEnabled().markInterested();
        mTransport.isClipLauncherOverdubEnabled().markInterested();
        
        initCursorElements();
        
        mTrackBank = mHost.createTrackBank(8, 8, 8);
        mTrackBank.followCursorTrack(mCursorTrack);
        mTrackBank.cursorIndex().markInterested();
        mTrackBank.canScrollBackwards().markInterested();
        mTrackBank.canScrollForwards().markInterested();

        initHardwareControlls();
        initLayers();
        initPadMatrix();
        initScenes();
        for (int i = 0; i < 4; i++) {
            initFaderLayer(mMixerLayers[i], i, i * 8);
        }
        initButtonLayer(mMixerLayers[4], 0, RGBState.RED, RGBState.DARKRED);
        initButtonLayer(mMixerLayers[5], 1, RGBState.ORANGE, RGBState.DARKORANGE);
        initButtonLayer(mMixerLayers[6], 2, RGBState.YELLOW, RGBState.DARKYELLOW);
        initButtonLayer(mMixerLayers[7], 3, RGBState.RED, RGBState.DARKRED);

        initNavigation();
        initTransport();

        initHardwareLayout();

        mSessionLayer.activate();

        mHost.showPopupNotification("Launchpad Pro Mk3 initialized...");
    }

    private void initCursorElements() {
        mCursorTrack = mHost.createCursorTrack(8, 8);
        mCursorClip = mHost.createLauncherCursorClip(192, 128);
        mCursorClip.getLoopLength().markInterested();
        mCursorTrack.canHoldNoteData().markInterested();
        mCursorTrack.playingNotes().addValueObserver(notes -> mPlayingNotes = notes);

        mDeviceBank = mCursorTrack.createDeviceBank(8);
        mDeviceBank.cursorIndex().markInterested();

        mCursorDevice = mCursorTrack.createCursorDevice();
        mCursorDevice.position().markInterested();
        mCursorDevice.deviceType().markInterested();
        mCursorDevice.hasDrumPads().markInterested();

        CursorDevice mInstrument = mCursorTrack.createCursorDevice("01", "track", 8,
                CursorDeviceFollowMode.FIRST_INSTRUMENT);

        mDrumPadBank = mInstrument.createDrumPadBank(64);
        mDrumPadBank.exists().markInterested();
        for (int i = 0; i < 64; i++) {
            mDrumPadBank.getItemAt(i).color().markInterested();
            mDrumPadBank.getItemAt(i).exists().markInterested();
            mDrumPadBank.getItemAt(i).isMutedBySolo().markInterested();
        }

        mCursorRemoteControlsPage = mCursorDevice.createCursorRemoteControlsPage(8);
        for (int i = 0; i < 8; i++) {
            mCursorRemoteControlsPage.getParameter(i).markInterested();
            mCursorRemoteControlsPage.getParameter(i).exists().markInterested();
            mDeviceBank.getDevice(i).exists().markInterested();
        }
    }

    private void resetLastLayer() {
        mLastLayer = null;
    }

    private void midiCallback(String s) {
        mHost.println(s);
        if (isNoteIntputActive) {
            if (mDrumPadBank.exists().get()) {
                mMidiOut.sendSysex(DRUM_MODE);
                sendPlayingNotesToDevice(true, DRUM_MIDI_CHANNEL, RGBState.WHITE);

            } else {
                mMidiOut.sendSysex(NOTE_MODE);
                sendPlayingNotesToDevice(false, NOTES_MIDI_CHANNEL, RGBState.GREEN);
            }
        }

        if (s.startsWith(SESSION_MODE_PREFIX) && (mLastLayer == null || mLastLayer == mSessionLayer)) {
            List<Layer> l = mLayers.getLayers();
            for (int i = 0; i < l.size(); i++) {
                l.get(i).deactivate();
            }
            mSessionLayer.activate();
        }

        if (mMixerLayer.isActive()) {
            sendFaderValuesToDevice();
            sendTrackControlsToDevice();
        }
    }

    private void sendTrackControlsToDevice() {
        for (int i = 0; i < 8; i++) {
            final Track track = mTrackBank.getItemAt(i);

            final Parameter volume = track.volume();
            mMidiOut.sendMidi(0xb4, i, (int) (volume.get() * 127));

            final Parameter pan = track.pan();
            mMidiOut.sendMidi(0xb4, i + 8, (int) (pan.get() * 127));

            final Parameter sendA = track.sendBank().getItemAt(0);
            mMidiOut.sendMidi(0xb4, i + 16, (int) (sendA.get() * 127));

            final Parameter sendB = track.sendBank().getItemAt(1);
            mMidiOut.sendMidi(0xb4, i + 24, (int) (sendB.get() * 127));
        }
    }

    private void sendFaderValuesToDevice() {
        for (int i = 0; i < 32; i++) {
            final Track track = mTrackBank.getItemAt(i % 8);
            if (i < 16)
                mMidiOut.sendMidi(0xb5, i,
                        track.exists().get() ? new RGBState(track.color().get()).getMessage() : 0);
            else if (i < 24) {
                sendSendsValuesToDevice(i, track, 0);
            } else {
                sendSendsValuesToDevice(i, track, 1);
            }
        }
    }

    private void sendSendsValuesToDevice(int i, final Track track, int sendIndex) {
        RGBState r = new RGBState(track.sendBank().getItemAt(1).sendChannelColor().get());
        r = r.getMessage() == 0 ? RGBState.WHITE : r;
        mMidiOut.sendMidi(0xb5, i,
                track.exists().get() && track.trackType().get() != "Master" ? r.getMessage() : 0);
    }

    private void sendPlayingNotesToDevice(boolean drums, int midi, RGBState onColor) {
        if (drums) {
            for (int i = 0; i < 64; i++) {
                if (mDrumPadBank.getItemAt(i).exists().get()) 
                    mMidiOut.sendMidi(0x98, 36 + i, new RGBState(mDrumPadBank.getItemAt(i).color().get()).getMessage());
            }
        } else {
            for (int j = 0; j < 88; j++) 
                mMidiOut.sendMidi(0x8f, j, onColor.getMessage());
        } 
        if (mPlayingNotes != null && mPlayingNotes.length != 0) {
            for (PlayingNote n : mPlayingNotes) 
                mMidiOut.sendMidi(midi, n.pitch(), onColor.getMessage());
        }
    }

    private void initMIDI() {
        mMidiIn0 = mHost.getMidiInPort(0);
        mMidiIn1 = mHost.getMidiInPort(1);
        mMidiOut = mHost.getMidiOutPort(0);

        mMidiOut.sendSysex(DAW_MODE);
        mMidiOut.sendSysex(SESSION_LAYOUT);

        mMidiOut.sendMidi(0xB0, 99, 3); // Logo Light

        mMidiIn0.setSysexCallback(s -> {
            midiCallback(s);
        });

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

    private void initTransport() {
        mSessionLayer.bindPressed(mRecButton, mTransport.isClipLauncherOverdubEnabled().toggleAction());
        mSessionLayer.bindLightState(() -> {
            if (mTransport.isClipLauncherOverdubEnabled().get())
                return RGBState.RED;
            else
                return RGBState.DARKRED;
        }, mRecLight);
    }

    private void initNavigation() {
        mSessionLayer.bindPressed(mSessionButton, () -> {
            if (!mMixerLayer.isActive() && !isNoteIntputActive) {
                mMixerLayer.activate();
                mMixerLayers[0].activate();
                mMidiOut.sendSysex(DAW_VOLUME_FADER);
                mMidiOut.sendSysex(DAW_FADER_ON);
                mMidiOut.sendSysex(MIXER_MODE_LED);
            } else {
                mMidiOut.sendSysex(DAW_FADER_OFF);
                switchLayer(mSessionLayer);
                mMixerLayer.deactivate();
                mMidiOut.sendSysex(SESSION_MODE_LED);
            }
            isNoteIntputActive = false;

        });

        mSessionLayer.bindPressed(mNoteButton, () -> {
            mMixerLayer.deactivate();
            isNoteIntputActive = true;
            mMidiOut.sendSysex(SESSION_MODE_LED);

        });
        mSessionLayer.bindPressed(mCustomButton, () -> {
            mMixerLayer.deactivate();
            isNoteIntputActive = true;
            mMidiOut.sendSysex(SESSION_MODE_LED);

        });

        //mSessionLayer.bindPressed(mUpButton, mSceneBank.scrollBackwardsAction());
        //mSessionLayer.bindPressed(mDownButton, mSceneBank.scrollForwardsAction());
        //mSessionLayer.bindPressed(mLeftButton, mTrackBank.scrollBackwardsAction());
        //mSessionLayer.bindPressed(mRightButton, mTrackBank.scrollForwardsAction());
        mSessionLayer.bindPressed(mRightButton, () -> pressedAction(mRightButton, () ->mTrackBank.scrollForwards()));
        mSessionLayer.bindPressed(mLeftButton, () -> pressedAction(mLeftButton, () ->mTrackBank.scrollBackwards()));
        mSessionLayer.bindPressed(mUpButton, () -> pressedAction(mUpButton, ()-> mSceneBank.scrollBackwards()));
        mSessionLayer.bindPressed(mDownButton, () -> pressedAction(mDownButton, ()-> mSceneBank.scrollForwards()));

        mSessionLayer.bindLightState(
                () -> mTrackBank.canScrollBackwards().get() ? RGBState.WHITE : RGBState.DARKGREY, mLeftLight);
        mSessionLayer.bindLightState(
                () -> mTrackBank.canScrollForwards().get() ? RGBState.WHITE : RGBState.DARKGREY, mRightLight);
        mSessionLayer.bindLightState(
                () -> mSceneBank.canScrollBackwards().get() ? RGBState.WHITE : RGBState.DARKGREY, mUpLight);
        mSessionLayer.bindLightState(
                () -> mSceneBank.canScrollForwards().get() ? RGBState.WHITE : RGBState.DARKGREY, mDownLight);
    }

    private void pressedAction(HardwareButton button, Runnable action) {
        action.run();
        longPressed(button, action);
    }

    private void longPressed(HardwareButton button, Runnable action) {
        longPressed(button, action, (long) 300.0);
    }

    private void longPressed(HardwareButton button, Runnable action,long timeout) {
        mHost.scheduleTask(() -> {
            if (button.isPressed().get()) {
                action.run();
                longPressed(button, action, (long) 100.0);
            }
        }, timeout);
    }
    
    private void initScenes() {
        mSceneBank = mTrackBank.sceneBank();
        mSceneBank.setIndication(true);
        mSceneBank.canScrollBackwards().markInterested();
        mSceneBank.canScrollForwards().markInterested();

        for (int i = 0; i < 8; i++) {
            int ix = i;
            mSceneBank.getScene(7 - i).exists().markInterested();
            mSceneBank.getScene(7 - i).color().markInterested();
            mSessionLayer.bindPressed(mRightButtons[i], mSceneBank.getScene(7 - ix).launchAction());
        }

        for (int i = 0; i < 8; i++) {
            int ix = i;
            mMixerLayer.bindPressed(mRightButtons[7 - ix], () -> {
                if (ix < 4) {
                    mMidiOut.sendSysex(DAW_FADER_MODES[ix]);
                    switchLayer(mMixerLayers[ix]);
                    mMidiOut.sendSysex(DAW_FADER_ON);
                } else {
                    switchLayer(mMixerLayers[ix]);
                    mMidiOut.sendSysex(DAW_FADER_OFF);

                }
            });
            mMixerLayer.bindLightState(() -> {
                if (mMixerLayers[ix].isActive())
                    return RGBState.WHITE;
                else
                    return RGBState.DARKGREY;
            }, mRightLights[7 - ix]);
        }

        for (int i = 0; i < 8; i++) {
            int ix = i;
            mSessionLayer.bindLightState(() -> {
                return RGBState.OFF;
            }, mRightLights[ix]);
        }
    }

    private void initPadMatrix() {
        for (int i = 0; i < 8; i++) {
            final Track track = mTrackBank.getItemAt(i);
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
            }
            final ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
            slotBank.setIndication(true);
            for (int j = 0; j < 8; j++) {
                final ClipLauncherSlot slot = slotBank.getItemAt(7 - j);
                slot.isSelected().markInterested();
                slot.isPlaybackQueued().markInterested();
                slot.isPlaying().markInterested();
                slot.isRecording().markInterested();
                slot.isRecordingQueued().markInterested();
                slot.isStopQueued().markInterested();
                slot.hasContent().markInterested();
                slot.color().markInterested();
                mSessionLayer.bindPressed(mButtons[i][j], () -> {
                    slot.launch();
                });

                mSessionLayer.bindLightState(() -> {
                    if (slot.isRecording().getAsBoolean())
                        return new RGBState(120, 2); //TODO 
                    if (slot.isPlaying().getAsBoolean())
                        return RGBState.GREEN_PULS;
                    else if (slot.isPlaybackQueued().getAsBoolean())
                        return RGBState.GREEN_BLINK;
                    else if (slot.isRecordingQueued().getAsBoolean())
                        return RGBState.RED_BLINK;
                    else if (slot.isStopQueued().getAsBoolean())
                        return RGBState.YELLOW;
                    else if (slot.hasContent().get())
                        return new RGBState(slot.color().get());
                    else if (track.arm().getAsBoolean())
                        return RGBState.TRACK_ARM;
                    else
                        return RGBState.OFF;
                }, mPadLights[i][j]);
            }
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

    private void switchLayer(Layer layer) {
        mLastLayer = layer;
        mHost.scheduleTask(() -> resetLastLayer(), (long) 100);
        List<Layer> l = mLayers.getLayers();
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i) == layer)
                l.get(i).activate();
            else
                l.get(i).deactivate();
        }
        mSessionLayer.activate();
        mMixerLayer.activate();
    }

    private void initLayers() {
        mSessionLayer = new Layer(mLayers, "Session");
        mMixerLayer = new Layer(mLayers, "Mixer");
        for (int i = 0; i < 8; i++) {
            mMixerLayers[i] = new Layer(mLayers, "MixerLayer" + i);
        }
    }

    private void initFaderLayer(Layer l, int p, int indexOffset) {
        for (int i = 0; i < 8; i++) {
            final Track track = mTrackBank.getItemAt(i);
            final Parameter[] parameter = new Parameter[4];
            parameter[0] = track.volume();
            parameter[1] = track.pan();
            parameter[2] = track.sendBank().getItemAt(0);
            parameter[3] = track.sendBank().getItemAt(1);
            l.bind(mFader[indexOffset + i], parameter[p]);
        }
    }

    private void initButtonLayer(Layer l, int p, RGBState rOn, RGBState rOff) {
        for (int i = 0; i < 8; i++) {
            int ix = i;
            Track t = mTrackBank.getItemAt(ix);
            HardwareActionBindable[] parameter = new HardwareActionBindable[4];
            parameter[0] = t.stopAction();
            parameter[1] = t.mute().toggleAction();
            parameter[2] = t.solo().toggleAction();
            parameter[3] = t.arm().toggleAction();

            l.bindPressed(mButtons[ix][0], parameter[p]);
            l.bindLightState(() -> {
                if ((p == 0 && !t.isStopped().get() && t.exists().get()) 
                        || (p == 1 && t.mute().get())
                        || (p == 2 && t.solo().get())
                        || (p == 3 && t.arm().get()))
                    if (p == 0 && t.isQueuedForStop().get())
                        return RGBState.RED_BLINK;
                    else
                        return rOn;
                else if (t.exists().get())
                    return rOff;
                else
                    return RGBState.OFF;
            }, mPadLights[ix][0]);
        }
    }

    private void initHardwareLayout() {
        HardwareSurface s = mHardwareSurface;
        s.setPhysicalSize(93, 93 * 2);

        // Top Row
        s.hardwareElementWithId("up").setBounds(1, 1, 10, 10);
        s.hardwareElementWithId("down").setBounds(10 + 1, 1, 10, 10);
        s.hardwareElementWithId("left").setBounds(20 + 1, 1, 10, 10);
        s.hardwareElementWithId("right").setBounds(30 + 1, 1, 10, 10);
        s.hardwareElementWithId("session").setBounds(40 + 1, 1, 10, 10);
        s.hardwareElementWithId("note").setBounds(50 + 1, 1, 10, 10);
        s.hardwareElementWithId("custom").setBounds(60 + 1, 1, 10, 10);
        s.hardwareElementWithId("record").setBounds(70 + 1, 1, 10, 10);

        for (int i = 0; i < 8; i++) {
            // Scenes
            s.hardwareElementWithId("scene" + (7 - i)).setBounds(82, 12 + 10 * (7 - i), 10, 10);
            // Fader
            s.hardwareElementWithId("fader" + i).setBounds(1 + i * 10, 93, 10, 20);
            s.hardwareElementWithId("fader" + i).setLabel("Volume");
            s.hardwareElementWithId("fader" + i).setLabelColor(Color.whiteColor());
            s.hardwareElementWithId("fader" + i).setLabelPosition(RelativePosition.INSIDE);
            s.hardwareElementWithId("fader" + (i + 8)).setBounds(1 + i * 10, 113, 10, 20);
            s.hardwareElementWithId("fader" + (i + 8)).setLabel("Pan");
            s.hardwareElementWithId("fader" + (i + 16)).setBounds(1 + i * 10, 133, 10, 20);
            s.hardwareElementWithId("fader" + (i + 16)).setLabel("SendA");
            s.hardwareElementWithId("fader" + (i + 24)).setBounds(1 + i * 10, 153, 10, 20);
            s.hardwareElementWithId("fader" + (i + 24)).setLabel("SendB");
            for (int j = 0; j < 8; j++) {
                // Pad Matrix
                s.hardwareElementWithId("" + (i * 10 + (7 - j))).setBounds(10 * i + 1, 10 * (7 - j) + 12, 10.0, 10.0);
            }
        }
    }

    @Override
    public void exit() {
        mMidiOut.sendSysex(STANDALONE_MODE);
        mHost.showPopupNotification("Launchpad Pro Mk3 exited...");
    }

    @Override
    public void flush() {
        mHardwareSurface.updateHardware();
        midiCallback("flush");
    }


    private Boolean isNoteIntputActive = false;

    // API Objects
    private HardwareSurface mHardwareSurface;
    private Application mApplication;
    private ControllerHost mHost;
    private MidiIn mMidiIn0;
    private MidiIn mMidiIn1;
    private MidiOut mMidiOut;
    private NoteInput mNoteInput;
    private NoteInput mDrumInput;
    private PlayingNote[] mPlayingNotes;

    private Transport mTransport;
    private TrackBank mTrackBank;
    private SceneBank mSceneBank;
    private CursorTrack mCursorTrack;
    private DeviceBank mDeviceBank;
    private CursorDevice mCursorDevice;
    private CursorRemoteControlsPage mCursorRemoteControlsPage;
    private DrumPadBank mDrumPadBank;
    private Clip mCursorClip;

    // Layers
    private Layers mLayers = new Layers(this);
    private Layer mSessionLayer;
    private Layer mMixerLayer;
    private Layer[] mMixerLayers = new Layer[8];
    private Layer mLastLayer;

    // Hardware Proxy
    private HardwareButton[][] mButtons = new HardwareButton[8][8];
    private MultiStateHardwareLight[][] mPadLights = new MultiStateHardwareLight[8][8];
    private HardwareButton[] mRightButtons = new HardwareButton[8];
    private MultiStateHardwareLight[] mRightLights = new MultiStateHardwareLight[8];
    private HardwareButton mUpButton;
    private MultiStateHardwareLight mUpLight;
    private HardwareButton mDownButton;
    private MultiStateHardwareLight mDownLight;
    private HardwareButton mLeftButton;
    private MultiStateHardwareLight mLeftLight;
    private HardwareButton mRightButton;
    private MultiStateHardwareLight mRightLight;
    private HardwareButton mSessionButton;
    private HardwareButton mNoteButton;
    private HardwareButton mCustomButton;
    private HardwareButton mRecButton;
    private MultiStateHardwareLight mRecLight;
    private HardwareSlider[] mFader = new HardwareSlider[32];
}
