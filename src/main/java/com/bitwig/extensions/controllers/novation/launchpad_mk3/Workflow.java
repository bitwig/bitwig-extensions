package com.bitwig.extensions.controllers.novation.launchpad_mk3;

import java.util.List;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.*;

public class Workflow extends Hardware {

    public Workflow(ControllerExtension driver, String model) {
        super(driver);
        mHost = driver.getHost();
        mLayers = new Layers(driver);
        modelName = model;
        initSysexMessages(model);
        initWorkflow();
    }

    private void initSysexMessages(String model) {
        switch (model) {
            case "Mini":
                formatSysexStrings("D");
                break;

            case "X":
                formatSysexStrings("C");
                break;

            default:
                break;
        }
    }

    private void formatSysexStrings(String s) {
        DAW_MODE = String.format(DAW_MODE, s);

        SESSION_LAYOUT = String.format(SESSION_LAYOUT, s);
        SESSION_MODE_PREFIX = String.format(SESSION_MODE_PREFIX, s.toLowerCase());

        SESSION_MODE_LED = String.format(SESSION_MODE_LED, s);
        MIXER_MODE_LED = String.format(MIXER_MODE_LED, s);

        DRUM_MODE = String.format(DRUM_MODE, s);
        NOTE_MODE = String.format(NOTE_MODE, s);

        DAW_VOLUME_FADER = String.format(DAW_VOLUME_FADER, s);
        DAW_PAN_FADER = String.format(DAW_PAN_FADER, s);
        DAW_SEND_A_FADER = String.format(DAW_SEND_A_FADER, s);
        DAW_SEND_B_FADER = String.format(DAW_SEND_B_FADER, s);
        DAW_FADER_ON = String.format(DAW_FADER_ON, s);
        DAW_FADER_OFF = String.format(DAW_FADER_OFF, s);

        String[] DAW_FADER_MODES_TEMP = {
                DAW_VOLUME_FADER,
                DAW_PAN_FADER,
                DAW_SEND_A_FADER,
                DAW_SEND_B_FADER
        };

        DAW_FADER_MODES = DAW_FADER_MODES_TEMP;
    }

    private void initWorkflow() {

        initNoteTable();
        initMIDI();

        mNoteInput = mMidiIn1.createNoteInput("Note", "90????", "D0??", "80????");
        mDrumInput = mMidiIn0.createNoteInput("Drum", "98????");
        mDrumInput.setKeyTranslationTable(noteTable);

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

        mSessionLayer.activate();

    }

    private void initNoteTable() {
        for (int i = 0; i < 128; i++) {
            noteTable[i] = i;
            baseNoteTable[i] = i;
        }
    }

    // WORK!!!
    private Boolean updateNoteTable(int direction) {
        mHost.println(String.valueOf(mDrumPadBank.scrollPosition().get()));

        if (mDrumPadBank.scrollPosition().get() <= 0 && direction == 0
                || mDrumPadBank.scrollPosition().get() >= 64 && direction == 1)
            return false;

        globalOffset = mDrumPadBank.scrollPosition().get() - 36;
        if (direction == 1)
            globalOffset += 4;
        else
            globalOffset -= 4;

        for (int i = 36; i < 100; i++)
            noteTable[i] = i + globalOffset;

        mDrumInput.setKeyTranslationTable(noteTable);
        return true;
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

    public void midiCallback(String s) {
        mHost.println(s);
        mHost.println("midi");
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

        if (mMixerLayer.isActive() && isLayerSwitched) {
            sendFaderValuesToDevice();
            sendTrackControlsToDevice();
        }
    }

    private void sendPlayingNotesToDevice(boolean drums, int midi, RGBState onColor) {
        if (drums) {
            for (int i = 0; i < 64; i++) {
                if (mDrumPadBank.getItemAt(i).exists().get())
                    mMidiOut.sendMidi(0x98, 36 + i, new RGBState(mDrumPadBank.getItemAt(i).color().get()).getMessage());
                else
                    mMidiOut.sendMidi(0x98, 36 + i, RGBState.OFF.getMessage());

            }
        }
        int NoteOn = 0x9f;
        int NoteOff = 0x8f;
        int DrumNoteOn = 0x98;
        if (mLastPlayingNotes != null && mLastPlayingNotes.length != 0 && (mLastPlayingNotes != mPlayingNotes)) {
            for (PlayingNote l : mLastPlayingNotes) {
                mMidiOut.sendMidi(NoteOff, l.pitch(), onColor.getMessage());
            }
        }
        if (mPlayingNotes != null && mPlayingNotes.length != 0 && mLastPlayingNotes != mPlayingNotes) {
            for (PlayingNote n : mPlayingNotes) {
                mMidiOut.sendMidi(DrumNoteOn, n.pitch() - globalOffset, onColor.getMessage());
                mMidiOut.sendMidi(NoteOn, n.pitch(), onColor.getMessage());
            }
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

    private void initCursorElements() {
        mCursorTrack = mHost.createCursorTrack(8, 8);
        mCursorClip = mHost.createLauncherCursorClip(192, 128);
        mCursorClip.getLoopLength().markInterested();
        mCursorTrack.canHoldNoteData().markInterested();
        mCursorTrack.playingNotes().addValueObserver(notes -> {
            mLastPlayingNotes = mPlayingNotes;
            mPlayingNotes = notes;
        });

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
        mDrumPadBank.canScrollBackwards().markInterested();
        mDrumPadBank.canScrollForwards().markInterested();
        mDrumPadBank.scrollPosition().markInterested();
        mDrumPadBank.scrollPosition().addValueObserver(position -> {
            globalOffset = position - 36;
            if (globalOffset == 0)
                mDrumInput.setKeyTranslationTable(baseNoteTable);
        });

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

    private void initLayers() {
        mSessionLayer = new Layer(mLayers, "Session");
        mMixerLayer = new Layer(mLayers, "Mixer");
        for (int i = 0; i < 8; i++) {
            mMixerLayers[i] = new Layer(mLayers, "MixerLayer" + i);
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
                final int ix = i;
                final int jx = j;
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

                ClipTimeout[ix][jx] = true;
                // To Work On !!!
                mSessionLayer.bindLightState(() -> {
                    if (slot.isRecordingQueued().getAsBoolean())
                        return RGBState.RED_BLINK;
                    if (slot.isPlaybackQueued().getAsBoolean() && ClipTimeout[ix][jx]) {
                        mHost.scheduleTask(() -> ClipTimeout[ix][jx] = false, 50);
                        return new RGBState(slot.color().get());
                    }
                    if (slot.isPlaybackQueued().getAsBoolean() && !slot.isPlaying().get())
                        return RGBState.GREEN_BLINK;
                    if ((slot.isStopQueued().get() || track.isQueuedForStop().get()) && slot.isPlaying().get())
                        return RGBState.YELLOW_BLINK;
                    if (slot.isRecording().getAsBoolean())
                        return new RGBState(120, 2);
                    if (slot.isPlaying().getAsBoolean()) {
                        ClipTimeout[ix][jx] = true;
                        return RGBState.GREEN_PULS;
                    }
                    if (slot.hasContent().get())
                        return new RGBState(slot.color().get());
                    if (track.arm().getAsBoolean())
                        return RGBState.TRACK_ARM;
                    else
                        return RGBState.OFF;
                }, mPadLights[i][j]);
            }
        }
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
            if (i != 0 || modelName == "X")
                mSessionLayer.bindPressed(mRightButtons[i], mSceneBank.getScene(7 - ix).launchAction());
        }

        for (int i = 0; i < 8; i++) {
            int ix = i;
            mMixerLayer.bindPressed(mRightButtons[7 - ix], () -> {
                if (mMixerLayers[ix].isActive()) {
                    mMidiOut.sendSysex(DAW_FADER_OFF);
                    mMixerLayers[ix].deactivate();
                    lastLayerIndex = 8;
                    return;
                }

                mHost.scheduleTask(() -> {
                    if (mRightButtons[7 - ix].isPressed().get())
                        isTemporarySwitch = true;
                }, 400);

                if (ix < 4) {
                    mMidiOut.sendSysex(DAW_FADER_MODES[ix]);
                    switchLayer(mMixerLayers[ix]);
                    mMidiOut.sendSysex(DAW_FADER_ON);
                } else {
                    switchLayer(mMixerLayers[ix]);
                    mMidiOut.sendSysex(DAW_FADER_OFF);

                }
            });
            mMixerLayer.bindReleased(mRightButtons[7 - ix], () -> {
                if (isTemporarySwitch && lastLayerIndex == 8) {
                    mMidiOut.sendSysex(DAW_FADER_OFF);
                    mMixerLayers[ix].deactivate();
                    isTemporarySwitch = false;

                    return;
                }
                if (isTemporarySwitch) {
                    switchLayer(mMixerLayers[lastLayerIndex]);
                    if (!(lastLayerIndex < 4))
                        mMidiOut.sendSysex(DAW_FADER_OFF);
                    else {
                        mMidiOut.sendSysex(DAW_FADER_MODES[lastLayerIndex]);
                        mMidiOut.sendSysex(DAW_FADER_ON);
                    }
                    isTemporarySwitch = false;
                } else {
                    if (mMixerLayers[ix].isActive())
                        lastLayerIndex = ix;
                    else
                        lastLayerIndex = 8;
                }

            });

            mMixerLayer.bindLightState(() -> {
                if ((ix == 4 || ix == 7) && mMixerLayers[ix].isActive())
                    return RGBState.RED;
                else if (ix == 4 || ix == 7)
                    return RGBState.DARKRED;
                if (ix == 5 && mMixerLayers[ix].isActive())
                    return RGBState.ORANGE;
                else if (ix == 5)
                    return RGBState.DARKORANGE;
                if (ix == 6 && mMixerLayers[ix].isActive())
                    return RGBState.YELLOW;
                else if (ix == 6)
                    return RGBState.DARKYELLOW;
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

        // Stop/Solo/Mute

        if (modelName == "Mini") {
            mSessionLayer.bindPressed(mRightButtons[0], () -> {
                incrementSsmIndex();
                if (ssmIndex == 1) {
                    switchLayer(mMixerLayers[4]);
                    mMixerLayer.deactivate();
                } else if (ssmIndex == 2) {
                    switchLayer(mMixerLayers[6]);
                    mMixerLayer.deactivate();
                } else if (ssmIndex == 3) {
                    switchLayer(mMixerLayers[5]);
                    mMixerLayer.deactivate();
                } else {
                    switchLayer(mSessionLayer);
                    mMixerLayer.deactivate();
                }
            });

            mSessionLayer.bindLightState(() -> {
                switch (ssmIndex) {
                    case 0:
                        return RGBState.GREY;
                    case 1:
                        return RGBState.RED;
                    case 2:
                        return RGBState.YELLOW;
                    case 3:
                        return RGBState.ORANGE;
                    default:
                        return RGBState.OFF;
                }
            }, mRightLights[0]);
        }
    }

    private void incrementSsmIndex() {
        ssmIndex += 1;
        if (ssmIndex >= 4)
            ssmIndex = 0;
    }

    private void resetSsmIndex() {
        ssmIndex = 0;
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

            /// DELETE
            HardwareActionBindable[] parameter = new HardwareActionBindable[4];
            parameter[0] = t.stopAction();
            parameter[1] = t.mute().toggleAction();
            // parameter[2] = t.solo().toggleAction();
            parameter[3] = t.arm().toggleAction();
            ////

            l.bindPressed(mButtons[ix][0], () -> {
                switch (p) {
                    case 0:
                        t.stop();
                    case 1:
                        t.mute().toggle();
                    case 2:
                        if (t.solo().get())
                            t.solo().set(false);
                        else
                            t.solo().set(true);
                    case 3:
                        t.arm().toggle();
                }
            });
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

    private void initNavigation() {
        mSessionLayer.bindPressed(mSessionButton, () -> {
            if (!mMixerLayer.isActive() && !isNoteIntputActive) {
                // switchLayer(mMixerLayers[0]);
                switchLayer(mMixerLayer);
                resetSsmIndex();
                // mMixerLayer.activate();
                // mMixerLayers[0].activate();
                mMidiOut.sendSysex(DAW_VOLUME_FADER);
                // mMidiOut.sendSysex(DAW_FADER_ON);
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

        // mSessionLayer.bindPressed(mUpButton, mSceneBank.scrollBackwardsAction());
        // mSessionLayer.bindPressed(mDownButton, mSceneBank.scrollForwardsAction());
        // mSessionLayer.bindPressed(mLeftButton, mTrackBank.scrollBackwardsAction());
        // mSessionLayer.bindPressed(mRightButton, mTrackBank.scrollForwardsAction());
        mSessionLayer.bindPressed(mRightButton, () -> pressedAction(mRightButton, () -> mTrackBank.scrollForwards()));
        mSessionLayer.bindPressed(mLeftButton, () -> pressedAction(mLeftButton, () -> mTrackBank.scrollBackwards()));
        mSessionLayer.bindPressed(mUpButton, () -> {
            if (isNoteIntputActive)
                pressedAction(mUpButton, () -> {
                    if (updateNoteTable(1))
                        mDrumPadBank.scrollBy(4);
                });
            else
                pressedAction(mUpButton, () -> mSceneBank.scrollBackwards());
        });
        mSessionLayer.bindPressed(mDownButton, () -> {
            if (isNoteIntputActive)
                pressedAction(mDownButton, () -> {
                    if (updateNoteTable(0))
                        mDrumPadBank.scrollBy(-4);
                });
            else
                pressedAction(mDownButton, () -> mSceneBank.scrollForwards());
        });
        // mSessionLayer.bindPressed(mDownButton, () -> pressedAction(mDownButton, () ->
        // mSceneBank.scrollForwards()));

        mSessionLayer.bindLightState(
                () -> {
                    if (isNoteIntputActive && mDrumPadBank.exists().get())
                        return RGBState.OFF;
                    if (mTrackBank.canScrollBackwards().get())
                        return RGBState.WHITE;
                    else
                        return RGBState.DARKGREY;
                }, mLeftLight);
        mSessionLayer.bindLightState(
                () -> {
                    if (isNoteIntputActive && mDrumPadBank.exists().get())
                        return RGBState.OFF;
                    if (mTrackBank.canScrollForwards().get())
                        return RGBState.WHITE;
                    else
                        return RGBState.DARKGREY;
                }, mRightLight);
        mSessionLayer.bindLightState(
                () -> {
                    if (isNoteIntputActive && mDrumPadBank.exists().get()) {
                        if (mDrumPadBank.canScrollForwards().get())
                            return RGBState.WHITE;
                        else
                            return RGBState.DARKGREY;
                    }

                    if (mSceneBank.canScrollBackwards().get())
                        return RGBState.WHITE;
                    else
                        return RGBState.DARKGREY;
                }, mUpLight);
        mSessionLayer.bindLightState(
                () -> {
                    if (isNoteIntputActive && mDrumPadBank.exists().get()) {
                        if (mDrumPadBank.canScrollBackwards().get())
                            return RGBState.WHITE;
                        else
                            return RGBState.DARKGREY;
                    }

                    if (mSceneBank.canScrollForwards().get())
                        return RGBState.WHITE;
                    else
                        return RGBState.DARKGREY;
                }, mDownLight);
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

        isLayerSwitched = true;
        mHost.scheduleTask(() -> isLayerSwitched = false, 10);
    }

    private void resetLastLayer() {
        mLastLayer = null;
    }

    private void pressedAction(HardwareButton button, Runnable action) {
        action.run();
        longPressed(button, action);
    }

    private void longPressed(HardwareButton button, Runnable action) {
        longPressed(button, action, (long) 300.0);
    }

    private void longPressed(HardwareButton button, Runnable action, long timeout) {
        mHost.scheduleTask(() -> {
            if (button.isPressed().get()) {
                action.run();
                longPressed(button, action, (long) 100.0);
            }
        }, timeout);
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

    public HardwareSurface getHardwareSurface() {
        return mHardwareSurface;
    }

    public MidiOut getMidiOut() {
        return mMidiOut;
    }

    // VARIABLES
    private Boolean isNoteIntputActive = false;
    protected final Integer[] noteTable = new Integer[128];
    protected final Integer[] baseNoteTable = new Integer[128];
    private int globalOffset = 0;

    private Boolean isLayerSwitched = false;
    private Boolean isTemporarySwitch = false;
    private int lastLayerIndex = 0;
    private Boolean ClipTimeout[][] = new Boolean[8][8];
    private int ssmIndex = 0;
    private String modelName;

    // MIDI
    private static final int NOTES_MIDI_CHANNEL = 0x9f;
    private static final int DRUM_MIDI_CHANNEL = 0x98;
    // SYSEX

    private String DAW_MODE = "F0 00 20 29 02 0%s 10 01 F7";

    private String SESSION_LAYOUT = "F0 00 20 29 02 0%s 00 00 00 00 F7";
    private String SESSION_MODE_PREFIX = "f0002029020%s00000000";

    private String SESSION_MODE_LED = "F0 00 20 29 02 0%s 14 14 01 F7";
    private String MIXER_MODE_LED = "F0 00 20 29 02 0%s 14 24 01 F7";

    private String DRUM_MODE = "F0 00 20 29 02 0%s 0F 01 F7";
    private String NOTE_MODE = "F0 00 20 29 02 0%s 0F 00 F7";

    private String DAW_VOLUME_FADER = "F0 00 20 29 02 0%s 01 00 00 00 00 00 00 01 00 01 00 02 00 02 00 03 00 03 00 04 00 04 00 05 00 05 00 06 00 06 00 07 00 07 00 F7";
    private String DAW_PAN_FADER = "F0 00 20 29 02 0%s 01 01 01 00 01 08 00 01 01 09 00 02 01 0A 00 03 01 0B 00 04 01 0C 00 05 01 0D 00 06 01 0E 00 07 01 0F 00 F7";
    private String DAW_SEND_A_FADER = "F0 00 20 29 02 0%s 01 02 00 00 00 10 00 01 00 11 00 02 00 12 00 03 00 13 00 04 00 14 00 05 00 15 00 06 00 16 00 07 00 17 00 F7";
    private String DAW_SEND_B_FADER = "F0 00 20 29 02 0%s 01 03 00 00 00 18 00 01 00 19 00 02 00 1A 00 03 00 1B 00 04 00 1C 00 05 00 1D 00 06 00 1E 00 07 00 1F 00 F7";

    private String[] DAW_FADER_MODES;

    private String DAW_FADER_ON = "F0 00 20 29 02 0%s 00 0D F7";
    private String DAW_FADER_OFF = "F0 00 20 29 02 0%s 00 00 F7";

    // API Objects
    private Application mApplication;
    private NoteInput mNoteInput;
    private NoteInput mDrumInput;
    private PlayingNote[] mPlayingNotes;
    private PlayingNote[] mLastPlayingNotes;

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
    private Layers mLayers;
    private Layer mSessionLayer;
    private Layer mMixerLayer;
    private Layer[] mMixerLayers = new Layer[8];
    private Layer mLastLayer;

}
