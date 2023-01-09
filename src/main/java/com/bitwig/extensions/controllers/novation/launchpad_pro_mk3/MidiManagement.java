package com.bitwig.extensions.controllers.novation.launchpad_pro_mk3;

import java.util.List;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.framework.Layer;

public class MidiManagement {

    private LaunchpadProMK3Workflow driver;
    private Sysex sysex;

    public MidiManagement(LaunchpadProMK3Workflow d) {
        driver = d;
        sysex = new Sysex();
    }

    // Rework for Performance!!
    public void midiCallback(String s) {
        // mHost.println(s);

        if (driver.isNoteModeActive && driver.mPlayingNotes != null) {
            sendNotesToDevice();
        }

        if (s.startsWith(sysex.SESSION_MODE_PREFIX) && (driver.layerManagement.getLastLayer() == null
                || driver.layerManagement.getLastLayer() == driver.mSessionLayer)) {
            driver.isNoteModeActive = false;
            // In LayerManagement auslagern
            List<Layer> l = driver.mLayers.getLayers();
            for (int i = 0; i < l.size(); i++) {
                l.get(i).deactivate();
            }
            driver.mSessionLayer.activate();
        }

        if (s.startsWith(sysex.NOTE_MODE_PREFIX) || s.startsWith(sysex.CHORD_MODE_PREFIX)) {
            driver.isNoteModeActive = true;
            driver.mHost.println("Note or Chord Mode");
            List<Layer> l = driver.mLayers.getLayers();
            for (int i = 0; i < l.size(); i++) {
                l.get(i).deactivate();
            }
            driver.mSessionLayer.activate();

        }

        if (s.startsWith(sysex.PRINT_TO_CLIP_PREFIX)) {
            driver.printToClip.print(driver, s);
        }

        if ((driver.isTrackBankNavigated || driver.layerManagement.getLastLayer() != null)
                && (driver.mVolumeLayer.isActive() || driver.mPanLayer.isActive()
                        || driver.mSendsLayer.isActive() || driver.mDeviceLayer.isActive())) {
            for (int i = 0; i < 8; i++) {
                final Track track = driver.mTrackBank.getItemAt(i);

                if (driver.mVolumeLayer.isActive()) {
                    driver.mMidiOut.sendMidi(0xb5, i,
                            track.exists().get() ? new RGBState(track.color().get()).getMessage() : 0);
                    final Parameter volume = track.volume();
                    driver.mMidiOut.sendMidi(0xb4, i, (int) (volume.get() * 127));
                }
                if (driver.mPanLayer.isActive()) {
                    driver.mMidiOut.sendMidi(0xb5, i + 8,
                            track.exists().get() ? new RGBState(track.color().get()).getMessage() : 0);
                    final Parameter pan = track.pan();
                    driver.mMidiOut.sendMidi(0xb4, i + 8, (int) (pan.get() * 127));
                }
                if (driver.mSendsLayer.isActive()) {
                    RGBState r = new RGBState(track.sendBank().getItemAt(driver.mSendIndex).sendChannelColor().get());
                    r = r.getMessage() == 0 ? RGBState.WHITE : r;
                    driver.mMidiOut.sendMidi(0xb5, i + 16,
                            track.exists().get() && track.trackType().get() != "Master" ? r.getMessage() : 0);
                    final Parameter send = track.sendBank().getItemAt(driver.mSendIndex);
                    driver.mMidiOut.sendMidi(0xb4, i + 16, (int) (send.get() * 127));
                }
                if (driver.mDeviceLayer.isActive()) {
                    driver.mMidiOut.sendMidi(0xb5, i,
                            driver.mCursorRemoteControlsPage.getParameter(i).exists().get() ? 79 : 0);
                    final Parameter device = driver.mCursorRemoteControlsPage.getParameter(i);
                    driver.mMidiOut.sendMidi(0xb4, i + 24, (int) (device.get() * 127));
                }
            }
        }
    }

    private void sendNotesToDevice() {
        if (!driver.mDrumPadBank.exists().get()) {
            for (int i = 0; i < 88; i++) {
                driver.mMidiOut.sendMidi(0x8f, i, 21);
                if (driver.mPlayingNotes.length != 0 && driver.mPlayingNotes != null) {
                    driver.mHost.println("PLayingNotes");
                    for (PlayingNote n : driver.mPlayingNotes) {
                        driver.mMidiOut.sendMidi(0x9f, n.pitch(), 21);
                    }
                }
            }
        }
        if (driver.mDrumPadBank.exists().get()) {
            driver.mMidiOut.sendSysex(sysex.DAW_DRUM);
            for (int i = 0; i < 64; i++) {
                if (driver.mDrumPadBank.getItemAt(i).exists().get()) {
                    driver.mMidiOut.sendMidi(0x98, 36 + i,
                            new RGBState(driver.mDrumPadBank.getItemAt(i).color().get()).getMessage());
                } else
                    driver.mMidiOut.sendMidi(0x98, 36 + i, RGBState.OFF.getMessage());
                if (driver.mPlayingNotes.length != 0 && driver.mPlayingNotes != null) {
                    for (PlayingNote n : driver.mPlayingNotes) {
                        driver.mMidiOut.sendMidi(0x98, n.pitch() - driver.drumBank.getOffset(),
                                RGBState.WHITE.getMessage());
                    }
                }
            }

        } else
            driver.mMidiOut.sendSysex(sysex.DAW_NOTE);
    }

}
