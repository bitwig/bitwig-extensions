package com.bitwig.extensions.controllers.akai.apc64;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.akai.apc.common.MidiProcessor;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Component
public class FocusClip {
    private static final int SINGLE_SLOT_RANGE = 8;

    private final CursorTrack cursorTrack;
    private final Application application;
    private final Transport transport;
    private final Clip mainCursorClip;
    private final Project project;
    private final ControllerHost host;
    private final OverviewGrid overviewGrid;

    private int selectedSlotIndex = -1;
    private int scrollOffset = 0;

    private String currentTrackName = "";

    private final Map<String, Integer> indexMemory = new HashMap<>();
    private final ClipLauncherSlotBank slotBank;
    private ClipLauncherSlot focusSlot;
    private Runnable scrollTask = null;

    @Inject
    private MidiProcessor midiProcessor;

    public FocusClip(ControllerHost host, Application application, Transport transport, ViewControl viewControl,
                     Project project) {
        this.cursorTrack = viewControl.getCursorTrack();
        this.project = project;
        this.host = host;
        this.overviewGrid = viewControl.getOverviewGrid();
        slotBank = cursorTrack.clipLauncherSlotBank();
        for (int i = 0; i < slotBank.getSizeOfBank(); i++) {
            final ClipLauncherSlot slot = slotBank.getItemAt(i);
            slot.exists().markInterested();
            slot.isRecording().markInterested();
            slot.isPlaying().markInterested();
            slot.hasContent().markInterested();
        }

        this.application = application;
        this.transport = transport;

        slotBank.addPlaybackStateObserver((slotIndex, playbackState, isQueued) -> {
            if (playbackState != 0 && !isQueued) {
                slotBank.select(slotIndex);
            }
        });
        slotBank.addIsSelectedObserver((index, selected) -> {
            if (selected) {
                selectedSlotIndex = index;
                indexMemory.put(currentTrackName, selectedSlotIndex);
                focusSlot = slotBank.getItemAt(selectedSlotIndex);
            }
        });
        slotBank.scrollPosition().addValueObserver(scrollPos -> {
            //Apc64Extension.println(" SB %d %d", scrollPos, overviewGrid.getNumberOfScenes());
            scrollOffset = scrollPos;
            if (scrollTask != null) {
                scrollTask.run();
                scrollTask = null;
            }
        });

        this.cursorTrack.name().addValueObserver(name -> {
            selectedSlotIndex = -1;
            currentTrackName = name;
            final Integer index = indexMemory.get(name);
            if (index != null) {
                selectedSlotIndex = index.intValue();
            }
        });
        mainCursorClip = viewControl.getCursorClip();
    }

    public void invokeRecord() {
        if (selectedSlotIndex != -1) {
            final ClipLauncherSlot slot = slotBank.getItemAt(selectedSlotIndex);
            if (slot.isRecording().get()) {
                slot.launch();
                transport.isClipLauncherOverdubEnabled().set(false);
            } else {
                Optional<ClipLauncherSlot> emptySlot = getFirstEmptySlot(selectedSlotIndex);
                if (emptySlot.isPresent()) {
                    recordAction(emptySlot.get());
                } else {
                    project.createScene();
                    host.scheduleTask(
                            () -> getFirstEmptySlot(selectedSlotIndex).ifPresent(newSlot -> recordAction(newSlot)), 50);
                }
            }
        } else {
            getFirstEmptySlot(selectedSlotIndex).ifPresent(slot -> recordAction(slot));
        }
    }

    private void recordAction(ClipLauncherSlot emptySlot) {
        emptySlot.launch();
        transport.isClipLauncherOverdubEnabled().set(true);
    }

    public void duplicateContent() {
        mainCursorClip.duplicateContent();
    }

    public void quantize(final double amount) {
        mainCursorClip.quantize(amount);
    }

    public void clearSteps() {
        mainCursorClip.clearSteps();
    }

    public void transpose(final int semitones) {
        mainCursorClip.transpose(semitones);
    }

    public void focusOnNextEmpty(Consumer<ClipLauncherSlot> postCreation) {
        if (focusSlotIsEmpty()) {
            postCreation.accept(focusSlot);
        } else {
            getFirstEmptySlot(selectedSlotIndex) //
                    .ifPresentOrElse(slot -> postCreation.accept(slot),  //
                            () -> ensureEmptySlot(postCreation));
        }
    }

    private void ensureEmptySlot(Consumer<ClipLauncherSlot> postCreation) {
        project.createScene();
        host.scheduleTask(() -> getFirstEmptySlot(selectedSlotIndex).ifPresent(newSlot -> postCreation.accept(newSlot)),
                50);
    }

    private boolean focusSlotIsEmpty() {
        return focusSlot != null && !focusSlot.hasContent().get() && focusSlot.exists().get();
    }

    private Optional<ClipLauncherSlot> getFirstEmptySlot(int startIndex) {
        int start = startIndex < 0 ? 0 : startIndex;
        for (int i = start; i < slotBank.getSizeOfBank(); i++) {
            final ClipLauncherSlot slot = slotBank.getItemAt(i);
            if (!slot.hasContent().get() && slot.exists().get()) {
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }

    public void clearNotes(int noteToClear) {
        mainCursorClip.clearStepsAtY(0, noteToClear);
    }
}
