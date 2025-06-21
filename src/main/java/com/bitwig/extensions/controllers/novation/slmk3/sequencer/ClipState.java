package com.bitwig.extensions.controllers.novation.slmk3.sequencer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.SettableBeatTimeValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlotUtil;
import com.bitwig.extensions.controllers.novation.slmk3.seqcommons.ClipSeqMode;
import com.bitwig.extensions.controllers.novation.slmk3.seqcommons.DrumNoteStepSlot;
import com.bitwig.extensions.controllers.novation.slmk3.seqcommons.INoteStepSlot;
import com.bitwig.extensions.controllers.novation.slmk3.seqcommons.NoteStepSlot;
import com.bitwig.extensions.controllers.novation.slmk3.seqcommons.SequencerSource;
import com.bitwig.extensions.controllers.novation.slmk3.seqcommons.StepViewPosition;
import com.bitwig.extensions.controllers.novation.slmk3.value.IObservableValue;
import com.bitwig.extensions.controllers.novation.slmk3.value.IntValue;
import com.bitwig.extensions.controllers.novation.slmk3.value.ObservableValue;
import com.bitwig.extensions.framework.values.BasicStringValue;

public class ClipState {
    private static final int STEPS = 16;
    
    private final double gatePercent = 1.0;
    private boolean exists;
    private final Clip notesCursorClip;
    private final Clip drumCursorClip;
    private final StepViewPosition positionHandler;
    protected final INoteStepSlot[] assignments = new NoteStepSlot[STEPS];
    protected final INoteStepSlot[] drumAssignments = new DrumNoteStepSlot[STEPS];
    
    protected final Set<Integer> heldSteps = new HashSet<>();
    private final HashMap<Integer, NoteStep> expectedNoteChange = new HashMap<>();
    private final List<NoteStep> copyNotes = new ArrayList<>();
    private final IntValue pageIndex;
    private int playingStep;
    private final ObservableValue<SlRgbState> clipColor = new ObservableValue<>(SlRgbState.OFF);
    private final BasicStringValue clipName = new BasicStringValue();
    private final BasicStringValue clipTrackName = new BasicStringValue();
    private ClipSeqMode mode;
    private final Set<NoteValue> currentPlayedNotes = new HashSet<>();
    private final Set<NoteValue> lastEnteredNotes = new HashSet<>();
    
    private record NoteOverlap(int newPos, NoteStep copyNote) {
        //
    }
    
    private record NoteValue(int key, int velocity) {
        //
        
        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final NoteValue noteValue = (NoteValue) o;
            return key == noteValue.key;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }
    
    public ClipState(final SequencerSource sequencerSource, final Clip cursorClip, final Clip drumCursorClip,
        final IObservableValue<ClipSeqMode> clipSeqState) {
        this.notesCursorClip = cursorClip;
        this.drumCursorClip = drumCursorClip;
        for (int i = 0; i < assignments.length; i++) {
            assignments[i] = new NoteStepSlot();
            drumAssignments[i] = new DrumNoteStepSlot();
        }
        notesCursorClip.addNoteStepObserver(this::handleNoteStep);
        notesCursorClip.exists().markInterested();
        notesCursorClip.playingStep().addValueObserver(this::handlePlayingStep);
        mode = clipSeqState.get();
        clipSeqState.addValueObserver(this::setMode);
        lastEnteredNotes.add(new NoteValue(60, 100));
        
        drumCursorClip.addNoteStepObserver(this::handleDrumNoteStep);
        drumCursorClip.scrollToKey(sequencerSource.getMonoKeyNoteFocus().get());
        sequencerSource.getMonoKeyNoteFocus().addValueObserver(this::handleDrumIndexChanged);
        
        SlotUtil.prepareSlot(this.notesCursorClip.clipLauncherSlot());
        positionHandler =
            new StepViewPosition(List.of(notesCursorClip, drumCursorClip), sequencerSource.getGridResolution(), STEPS);
        this.pageIndex = new IntValue(0, 0, 1); //
        pageIndex.setMax(positionHandler.getPages() - 1);
        positionHandler.addPagesChangedCallback((index, pages) -> {
            pageIndex.setMax(pages - 1);
            pageIndex.set(index);
        });
        pageIndex.addValueObserver(positionHandler::setPage);
        
        prepareTrack(cursorClip.getTrack());
        
        cursorClip.exists().addValueObserver(this::updateValueUponExistence);
        cursorClip.clipLauncherSlot().name().addValueObserver(this::updateClipName);
        cursorClip.getTrack().name().addValueObserver(clipTrackName::set);
        
        cursorClip.color().addValueObserver((r, g, b) -> clipColor.set(SlRgbState.get(r, g, b)));
    }
    
    private void handleDrumIndexChanged(final int monoKeyIndex) {
        drumCursorClip.scrollToKey(monoKeyIndex);
    }
    
    private void setMode(final ClipSeqMode mode) {
        this.mode = mode;
    }
    
    public StepViewPosition getPositionHandler() {
        return positionHandler;
    }
    
    private static void prepareTrack(final Track track) {
        track.arm().markInterested();
        track.solo().markInterested();
        track.mute().markInterested();
    }
    
    private void updateValueUponExistence(final boolean exists) {
        this.exists = exists;
        updateClipName(notesCursorClip.clipLauncherSlot().name().get());
    }
    
    private void updateClipName(final String name) {
        if (!exists) {
            clipName.set("<NO CLIP>");
        } else {
            clipName.set(name);
        }
    }
    
    public SettableBeatTimeValue getLoopLength() {
        return notesCursorClip.getLoopLength();
    }
    
    private void handleDrumNoteStep(final NoteStep noteStep) {
        final int newStep = noteStep.x();
        final int xyIndex = (noteStep.x() << 8) | (mode == ClipSeqMode.DRUM ? 0 : noteStep.y());
        drumAssignments[newStep].updateNote(noteStep);
        
        if (mode != ClipSeqMode.DRUM) {
            return;
        }
        
        final NoteStep previousStep = expectedNoteChange.get(xyIndex);
        if (previousStep != null) {
            expectedNoteChange.remove(xyIndex);
            applyValues(noteStep, previousStep);
        }
    }
    
    private void handleNoteStep(final NoteStep noteStep) {
        final int newStep = noteStep.x();
        final int xyIndex = noteStep.x() << 8 | noteStep.y();
        assignments[newStep].updateNote(noteStep);
        
        if (mode != ClipSeqMode.KEYS) {
            return;
        }
        
        final NoteStep previousStep = expectedNoteChange.get(xyIndex);
        if (previousStep != null) {
            expectedNoteChange.remove(xyIndex);
            applyValues(noteStep, previousStep);
        }
    }
    
    public List<NoteStep> getHeldNotes() {
        final List<NoteStep> result = new ArrayList<>();
        
        final INoteStepSlot[] modAssignments = mode == ClipSeqMode.DRUM ? drumAssignments : assignments;
        for (final Integer heldIndex : heldSteps) {
            final INoteStepSlot slot = modAssignments[heldIndex];
            result.addAll(slot.steps());
        }
        return result;
    }
    
    public void applySelection(final int index) {
        heldSteps.add(index);
    }
    
    public void removeSelection(final int index) {
        heldSteps.remove(index);
    }
    
    public boolean hasHeldSteps() {
        return !heldSteps.isEmpty();
    }
    
    public boolean hasHeldStepsAndNotes() {
        if (heldSteps.isEmpty()) {
            return false;
        }
        return heldSteps.stream().anyMatch(slot -> assignments[slot].hasNotes());
    }
    
    public int heldIndex() {
        if (heldSteps.size() == 1) {
            return heldSteps.stream().findFirst().orElse(-1);
        }
        return -1;
    }
    
    public Clip getNotesCursorClip() {
        return notesCursorClip;
    }
    
    public void handleNoteAction(final int note, final int velocity) {
        final NoteValue value = new NoteValue(note, velocity);
        if (velocity > 0) {
            currentPlayedNotes.add(value);
        } else {
            currentPlayedNotes.remove(value);
        }
        if (heldSteps.isEmpty()) {
            return;
        }
        if (velocity > 0) {
            for (final Integer heldIndex : heldSteps) {
                final INoteStepSlot slot = getAssignment(heldIndex);
                if (slot.containsNote(note)) {
                    notesCursorClip.clearStep(heldIndex, note);
                } else {
                    notesCursorClip.setStep(heldIndex, note, velocity, getNoteResolutionLength());
                }
            }
        }
    }
    
    public void addNote(final int step, final int note, final int velocity, final double length) {
        notesCursorClip.setStep(step, note, velocity, getNoteResolutionLength() * length);
    }
    
    public void toggleValues(final int index) {
        final INoteStepSlot noteStep = getAssignment(index);
        if (noteStep.hasNotes()) {
            clearSteps(index);
        } else {
            if (currentPlayedNotes.isEmpty()) {
                setNotes(lastEnteredNotes, index);
            } else {
                setNotes(currentPlayedNotes, index);
                lastEnteredNotes.clear();
                lastEnteredNotes.addAll(currentPlayedNotes);
            }
        }
    }
    
    private void setNotes(final Set<NoteValue> values, final int index) {
        for (final NoteValue value : values) {
            notesCursorClip.setStep(index, value.key, value.velocity(), getNoteResolutionLength());
        }
    }
    
    void handlePlayingStep(final int playingStep) {
        if (playingStep == -1) {
            this.playingStep = -1;
        }
        this.playingStep = playingStep - positionHandler.getStepOffset();
    }
    
    public double getNoteResolutionLength() {
        return positionHandler.getGridResolution() * gatePercent;
    }
    
    public boolean stepIndexInLoop(final int index) {
        return positionHandler.stepIndexInLoop(index);
    }
    
    public int getLastStep() {
        return positionHandler.getPageLastStep();
    }
    
    public INoteStepSlot getAssignment(final int index) {
        if (mode == ClipSeqMode.KEYS) {
            return assignments[index];
        } else {
            return drumAssignments[index];
        }
    }
    
    public int getPlayingStep() {
        return playingStep;
    }
    
    public void moveToBuffer(final int index) {
        if (mode == ClipSeqMode.KEYS) {
            assignments[index].steps().forEach(step -> this.copyNotes.add(new NoteStepStore(step)));
        } else {
            drumAssignments[index].steps().forEach(step -> this.copyNotes.add(new NoteStepStore(step)));
        }
    }
    
    private Clip getClipByMode() {
        return mode == ClipSeqMode.KEYS ? notesCursorClip : drumCursorClip;
    }
    
    public void doPaste(final int index) {
        if (this.copyNotes.isEmpty()) {
            return;
        }
        final List<NoteOverlap> copyOverlaps = new ArrayList<>();
        final int minOffset = this.copyNotes.stream().mapToInt(NoteStep::x).min().orElse(0);
        for (final NoteStep copyNote : this.copyNotes) {
            if (copyNote.state() == NoteStep.State.Empty) {
                getClipByMode().clearStep(copyNote.x(), copyNote.y());
            } else {
                final int newPos = index + (copyNote.x() - minOffset);
                if (newPos < 16) {
                    pasteNote(newPos, copyNote);
                } else {
                    copyOverlaps.add(new NoteOverlap(newPos % 16, copyNote));
                }
            }
        }
        if (!copyOverlaps.isEmpty()) {
            positionHandler.moveRight();
            copyOverlaps.forEach(v -> pasteNote(v.newPos, v.copyNote));
            positionHandler.moveLeft();
        }
    }
    
    private void pasteNote(final int newPos, final NoteStep copyNote) {
        final int vel = (int) Math.round(copyNote.velocity() * 127);
        final double duration = copyNote.duration();
        // Problem with clip step size, need to be double actually
        final int xyIndex = newPos << 8 | copyNote.y();
        expectedNoteChange.put(xyIndex, copyNote);
        getClipByMode().setStep(newPos, copyNote.y(), vel, duration);
    }
    
    public void addDrumStep(final int index, final int velocity) {
        drumCursorClip.setStep(index, 0, velocity, getNoteResolutionLength());
    }
    
    public ObservableValue<SlRgbState> getClipColor() {
        return clipColor;
    }
    
    public void removeDrumStep(final int index) {
        drumCursorClip.clearStep(index, 0);
    }
    
    public void clearSteps(final int index) {
        getAssignment(index).steps().forEach(step -> notesCursorClip.clearStep(index, step.y()));
        getAssignment(index).clear();
    }
    
    public void clearCopyNotes() {
        copyNotes.clear();
    }
    
    public void clearSteps(final boolean all) {
        if (mode == ClipSeqMode.KEYS || all) {
            notesCursorClip.clearSteps();
        } else {
            drumCursorClip.clearStepsAtY(0, 0);
        }
    }
    
    public void removeNote(final int note) {
        notesCursorClip.clearStepsAtY(0, note);
    }
    
    public void duplicateContent() {
        notesCursorClip.duplicateContent();
    }
    
    
    public boolean canScrollPositionLeft() {
        return positionHandler.canScrollLeft().get();
    }
    
    public boolean canScrollPositionRight() {
        return positionHandler.canScrollRight().get();
    }
    
    
    public void scrollPosition(final int byValue) {
        if (byValue > 0) {
            positionHandler.scrollRight();
        } else {
            positionHandler.scrollLeft();
        }
    }
    
    public void expandToPosition() {
        if (this.positionHandler.getGridResolution() <= 0.25) {
            this.positionHandler.expandClipToPagePosition(pageIndex.get());
        } else {
            this.positionHandler.expandClipByBar();
        }
    }
    
    public void advancePage(final boolean expand) {
        if (positionHandler.canScrollRight().get()) {
            positionHandler.scrollRight();
        } else {
            if (expand) {
                positionHandler.scrollRight();
            } else {
                positionHandler.setPage(0);
            }
        }
    }
    
    public void goToStart() {
        positionHandler.setPage(0);
    }
    
    public static void applyValues(final NoteStep dest, final NoteStep src) {
        if (src.chance() != dest.chance()) {
            dest.setChance(src.chance());
        }
        if (src.timbre() != dest.timbre()) {
            dest.setTimbre(src.timbre());
        }
        if (src.pressure() != dest.pressure()) {
            dest.setPressure(src.pressure());
        }
        if (src.velocitySpread() == dest.velocitySpread()) {
            dest.setVelocitySpread(src.velocitySpread());
        }
        if (src.repeatCount() != dest.repeatCount()) {
            dest.setRepeatCount(src.repeatCount());
        }
        if (src.repeatVelocityCurve() != dest.repeatVelocityCurve()) {
            dest.setRepeatVelocityCurve(src.repeatVelocityCurve());
        }
        if (src.repeatVelocityEnd() != dest.repeatVelocityEnd()) {
            dest.setRepeatVelocityEnd(src.repeatVelocityEnd());
        }
        if (src.repeatCurve() != dest.repeatCurve()) {
            dest.setRepeatCurve(src.repeatCurve());
        }
        if (src.pan() != dest.pan()) {
            dest.setPan(src.pan());
        }
        if (dest.recurrenceLength() != src.recurrenceLength() && dest.recurrenceMask() != src.recurrenceMask()) {
            dest.setRecurrence(src.recurrenceLength(), src.recurrenceMask());
        }
        if (src.occurrence() != dest.occurrence()) {
            dest.setOccurrence(src.occurrence());
        }
        if (src.transpose() != dest.transpose()) {
            dest.setTranspose(src.transpose());
        }
    }
    
    
}
