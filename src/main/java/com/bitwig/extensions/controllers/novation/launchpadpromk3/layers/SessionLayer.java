package com.bitwig.extensions.controllers.novation.launchpadpromk3.layers;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.commonsmk3.*;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.*;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.di.PostConstruct;

public class SessionLayer extends AbstractLpSessionLayer {
    public static final double MAX_LENGTH_FOR_DUPLICATE = 512 * 4.0;
    private Clip cursorClip;
    @Inject
    private ModifierStates modifiers;
    @Inject
    private ViewCursorControl viewCursorControl;
    @Inject
    private LppPreferences preferences;

    public SessionLayer(final Layers layers) {
        super(layers);
    }

    @PostConstruct
    protected void init(final Transport transport, final HwElements hwElements) {
        clipLauncherOverdub = transport.isClipLauncherOverdubEnabled();
        clipLauncherOverdub.markInterested();
        cursorClip = viewCursorControl.getCursorClip();
        cursorClip.getLoopLength().markInterested();

        final TrackBank trackBank = viewCursorControl.getTrackBank();
        trackBank.setShouldShowClipLauncherFeedback(true);

        final SceneBank sceneBank = trackBank.sceneBank();
        final Scene targetScene = trackBank.sceneBank().getScene(0);
        targetScene.clipCount().markInterested();
        initClipControl(hwElements, trackBank);
        initNavigation(hwElements, trackBank, sceneBank);
    }

    private void initNavigation(final HwElements hwElements, final TrackBank trackBank, final SceneBank sceneBank) {
        final LabeledButton upButton = hwElements.getLabeledButton(LabelCcAssignments.UP);
        final LabeledButton downButton = hwElements.getLabeledButton(LabelCcAssignments.DOWN);
        final LabeledButton leftButton = hwElements.getLabeledButton(LabelCcAssignments.LEFT);
        final LabeledButton rightButton = hwElements.getLabeledButton(LabelCcAssignments.RIGHT);
        sceneBank.canScrollForwards().markInterested();
        sceneBank.canScrollBackwards().markInterested();
        trackBank.canScrollForwards().markInterested();
        trackBank.canScrollBackwards().markInterested();
        final RgbState baseColor = RgbState.of(1);
        final RgbState pressedColor = RgbState.of(3);

        // TODO Shift function needed
        downButton.bindRepeatHold(this, () -> sceneBank.scrollBy(1));
        downButton.bindHighlightButton(this, sceneBank.canScrollForwards(), baseColor, pressedColor);

        upButton.bindRepeatHold(this, () -> sceneBank.scrollBy(-1));
        upButton.bindHighlightButton(this, sceneBank.canScrollBackwards(), baseColor, pressedColor);

        leftButton.bindRepeatHold(this, () -> trackBank.scrollBy(-1));
        leftButton.bindHighlightButton(this, trackBank.canScrollBackwards(), baseColor, pressedColor);

        rightButton.bindRepeatHold(this, () -> trackBank.scrollBy(1));
        rightButton.bindHighlightButton(this, trackBank.canScrollForwards(), baseColor, pressedColor);
    }

    private void initClipControl(final HwElements hwElements, final TrackBank trackBank) {
        for (int i = 0; i < 8; i++) {
            final int trackIndex = i;
            final Track track = trackBank.getItemAt(trackIndex);
            final BooleanValue equalsToCursorTrack = track.createEqualsValue(viewCursorControl.getCursorTrack());
            equalsToCursorTrack.markInterested();
            markTrack(track);
            for (int j = 0; j < 8; j++) {
                final int sceneIndex = j;
                final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
                slot.isSelected()
                   .addValueObserver(
                      selected -> handleSlotSelection(selected, track, sceneIndex, slot, equalsToCursorTrack));
                prepareSlot(slot, sceneIndex, trackIndex);

                final GridButton button = hwElements.getGridButton(sceneIndex, trackIndex);
                button.bindPressed(this, pressed -> handleSlot(pressed, track, slot, trackIndex));
                button.bindLight(this, () -> getState(track, slot, trackIndex, sceneIndex));
            }
        }
    }

    private void handleSlotSelection(final boolean wasSelected, final Track track, final int sceneIndex,
                                     final ClipLauncherSlot slot, BooleanValue equalsToCursorTrack) {
        if (wasSelected) {
            viewCursorControl.focusSlot(new FocusSlot(track, slot, sceneIndex, equalsToCursorTrack));
        }
    }

    private void markTrack(final Track track) {
        track.isStopped().markInterested();
        track.mute().markInterested();
        track.solo().markInterested();
        track.isQueuedForStop().markInterested();
        track.arm().markInterested();
    }

    private void prepareSlot(final ClipLauncherSlot slot, final int sceneIndex, final int trackIndex) {
        slot.hasContent().markInterested();
        slot.isPlaying().markInterested();
        slot.isStopQueued().markInterested();
        slot.isRecordingQueued().markInterested();
        slot.isRecording().markInterested();
        slot.isPlaybackQueued().markInterested();
        slot.color().addValueObserver((r, g, b) -> colorIndex[sceneIndex][trackIndex] = ColorLookup.toColor(r, g, b));
    }

    private void handleSlot(final boolean pressed, final Track track, final ClipLauncherSlot slot,
                            final int trackIndex) {
        if (pressed) {
            if (modifiers.isShift()) {
                if (modifiers.isQuantize()) {
                    doQuantize(slot);
                } else if (modifiers.isClear()) {
                    if (slot.hasContent().get()) {
                        selectClip(slot);
                        cursorClip.clearSteps();
                    }
                } else if (modifiers.isDuplicate()) {
                    if (slot.hasContent().get()) {
                        selectClip(slot);
                        if (cursorClip.getLoopLength().get() < 128.0) {
                            cursorClip.duplicateContent();
                            slot.showInEditor();
                        }
                    }
                } else {
                    if (preferences.getAltModeWithShift().get() && slot.hasContent().get()) {
                        slot.launchAlt();
                    } else {
                        slot.select();
                    }
                }
            } else {
                handleSlotPressNoShift(slot);
            }
        } else if (preferences.getAltModeWithShift().get()) {
            if (slot.hasContent().get()) {
                if (modifiers.isShift()) {
                    slot.launchReleaseAlt();
                } else if (!modifiers.anyModifierHeld()) {
                    slot.launchRelease();
                }
            }
        }
    }

    private void handleSlotPressNoShift(final ClipLauncherSlot slot) {
        if (modifiers.isQuantize()) {
            doQuantize(slot);
        } else if (modifiers.isClear()) {
            slot.deleteObject();
        } else if (modifiers.isDuplicate()) {
            slot.duplicateClip();
        } else {
            slot.launch();
            slot.select();
        }
    }

    private void doQuantize(final ClipLauncherSlot slot) {
        if (slot.hasContent().get()) {
            selectClip(slot);
            cursorClip.quantize(1.0);
            slot.showInEditor();
        }
    }

    private void selectClip(final ClipLauncherSlot slot) {
        slot.select();
    }

    @Override
    protected void onActivate() {
        super.onActivate();
    }

    @Override
    protected void onDeactivate() {
        super.onDeactivate();
    }


}
