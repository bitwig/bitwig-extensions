package com.bitwig.extensions.controllers.novation.launchpadpromk3.layers;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.commonsmk3.LabeledButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.HwElements;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.LppPreferences;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.ModifierStates;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.ViewCursorControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.di.PostConstruct;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class TrackControlLayer extends Layer {

    private final boolean[] selectionField = new boolean[8];
    private final HashSet<Integer> heldTracksButtons = new HashSet<>();
    private final HashMap<TrackModeButtonMode, Layer> modeLayers = new HashMap<>();

    @Inject
    private ModifierStates modifiers;
    @Inject
    private TrackState trackState;
    @Inject
    private LppPreferences preferences;

    private TrackModeLayer trackModes;
    private SettableBeatTimeValue postRecordingTimeOffset;

    public TrackControlLayer(final Layers layers) {
        super(layers, "TRACK_CONTROL_LAYER");
    }

    @PostConstruct
    public void init(final HwElements hwElements, final ViewCursorControl viewCursorControl) {

        final TrackBank trackBank = viewCursorControl.getTrackBank();
        final List<LabeledButton> trackButtons = hwElements.getTrackSelectButtons();
        for (int i = 0; i < 8; i++) {
            final int trackIndex = i;
            final Track track = trackBank.getItemAt(trackIndex);
            final LabeledButton button = trackButtons.get(i);
            prepareTrack(track);
            track.addIsSelectedInMixerObserver(selectedInMixer -> selectionField[trackIndex] = selectedInMixer);

            button.bindLight(this, () -> getTrackColor(trackIndex, track));
            button.bindPressed(this, pressed -> handleTrack(pressed, trackIndex, track));
        }
    }

    @Inject
    public void setTrackModes(final TrackModeLayer trackModes) {
        this.trackModes = trackModes;
    }

    @Inject
    public void setTransport(final Transport transport) {
        postRecordingTimeOffset = transport.getClipLauncherPostRecordingTimeOffset();
        final SettableEnumValue postRecordingAction = transport.clipLauncherPostRecordingAction();
        postRecordingAction.markInterested();
        postRecordingTimeOffset.markInterested();
    }

    private void prepareTrack(final Track track) {
        track.exists().markInterested();
        track.arm().markInterested();
        track.solo().markInterested();
        track.mute().markInterested();
        track.isQueuedForStop().markInterested();
        track.isStopped().markInterested();
    }

    private void handleTrack(final boolean pressed, final int index, final Track track) {
        if (pressed) {
            heldTracksButtons.add(index);
        } else {
            heldTracksButtons.remove(index);
        }
        switch (trackModes.getButtonsMode()) {
            case SELECT:
                handleTrackSelected(pressed, index, track);
                break;
            case ARM:
                handleTrackArm(pressed, index, track);
                break;
            case MUTE:
                handleTrackMute(pressed, index, track);
                break;
            case SOLO:
                handleTrackSolo(pressed, index, track);
                break;
            case STOP:
                handleTrackStop(pressed, index, track);
                break;
            case FIXED_LENGTH:
                handleFixedLength(pressed, index + 1);
                break;
        }
    }

    private void handleTrackSelected(final boolean pressed, final int index, final Track track) {
        if (!pressed) {
            return;
        }
        if (track.exists().get()) {
            if (modifiers.isClear()) {
                track.deleteObject();
            } else if (modifiers.isDuplicate()) {
                track.duplicate();
            } else if (modifiers.isShift()) {
                track.selectInEditor();
            } else {
                track.selectInMixer();
            }
        }
    }

    private void handleFixedLength(final boolean pressed, final int index) {
        postRecordingTimeOffset.set(index * 4.0);
    }

    private void handleTrackArm(final boolean pressed, final int index, final Track track) {
        if (!pressed) {
            return;
        }
        if (track.exists().get()) {
            track.arm().toggle();
        }
    }

    private void handleTrackMute(final boolean pressed, final int index, final Track track) {
        if (!pressed) {
            return;
        }
        if (track.exists().get()) {
            track.mute().toggle();
        }
    }

    private void handleTrackSolo(final boolean pressed, final int index, final Track track) {
        if (!pressed) {
            return;
        }
        if (track.exists().get()) {
            track.solo().toggle(heldTracksButtons.size() < 2);
        }
    }

    private void handleTrackStop(final boolean pressed, final int index, final Track track) {
        if (!pressed) {
            return;
        }
        if (track.exists().get()) {
            if (preferences.getAltModeWithShift().get() && modifiers.onlyShift()) {
                // Ready for stop alt
            }
            track.stop();
        }
    }

    private RgbState getTrackColor(final int index, final Track track) {
        if (trackModes.getButtonsMode() == TrackModeButtonMode.FIXED_LENGTH) {
            final double len = postRecordingTimeOffset.get() / 4;
            if (index < len) {
                return RgbState.ORANGE_PULSE;
            }
            return RgbState.OFF;
        }
        if (!track.exists().get()) {
            return RgbState.OFF;
        }
        switch (trackModes.getButtonsMode()) {
            case SELECT:
                return getTrackColorSelect(index, track);
            case ARM:
                return track.arm().get() ? RgbState.RED : RgbState.RED_LO;
            case MUTE:
                return track.mute().get() ? RgbState.ORANGE : RgbState.ORANGE_LO;
            case SOLO:
                return track.solo().get() ? RgbState.YELLOW : RgbState.YELLOW_LO;
            case STOP: {
                if (track.isStopped().get()) {
                    return RgbState.RED_LO;
                }
                if (track.isQueuedForStop().get()) {
                    return RgbState.flash(5, 0);
                }
                return RgbState.RED;
            }
        }
        return RgbState.OFF;
    }

    private RgbState getTrackColorSelect(final int index, final Track track) {
        if (track.exists().get()) {
            if (selectionField[index]) {
                return RgbState.of(trackState.getColorOfTrack(index));
            }
            return RgbState.DIM_WHITE;
        }
        return RgbState.OFF;
    }

}
