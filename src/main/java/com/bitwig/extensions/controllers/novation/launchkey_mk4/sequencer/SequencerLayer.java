package com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer;

import java.util.Arrays;

import com.bitwig.extension.controller.api.BeatTimeFormatter;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.CcAssignments;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.ControlHandler;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.GlobalStates;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.LaunchkeyHwElements;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.MidiProcessor;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.ViewControl;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.RgbButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.DisplayControl;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.StringUtil;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.TempDisplayValue;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.values.IntValue;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.values.NoteHandler;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.values.ObservableValue;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.values.ValueSet;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.time.TimedDelayEvent;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.BooleanValueObject;

@Component
public class SequencerLayer extends Layer implements NoteHandler, SequencerSource {
    private static final int STEPS = 16;
    private final CursorTrack cursorTrack;
    private RgbState focusDrumPadColor;
    private final DisplayControl displayControl;
    public static final RgbState STEP_ACT_COLOR = RgbState.BLUE.dim();
    public static final RgbState STEP_CPY_COLOR = RgbState.flash(13, 3);
    private final int copyIndex = -1;
    private final long[] stepDownTimes = new long[STEPS];
    private final boolean[] justEntered = new boolean[STEPS];
    private TimedDelayEvent holdDelayEvent = null;
    private final IntValue monoKeyNoteFocus = new IntValue(36, 0, 127);
    private final ValueSet gridValue;
    private final MidiProcessor midiProcessor;
    private final BeatTimeFormatter formatter;
    private final GlobalStates globalStates;
    private final DrumPadBank singlePadBank;
    private final DrumPad focusDrumPad;
    private String focusPadName;
    private boolean focusPadExists;
    private boolean hasDrumPads;
    private final ClipState clipState;
    private final ObservableValue<ClipSeqMode> seqMode = new ObservableValue<>(ClipSeqMode.KEYS);
    private final BooleanValueObject copyState = new BooleanValueObject();
    private final BasicStringValue focusValue = new BasicStringValue();
    private final TempDisplayValue padDisplay;
    private final OverlayEncoderLayer encoderLayer;
    
    @Inject
    private ControlHandler controlHandler;
    
    public SequencerLayer(final Layers layers, final ViewControl viewControl, final LaunchkeyHwElements hwElements,
        final GlobalStates globalStates, final ControllerHost host, final DisplayControl displayControl,
        final MidiProcessor midiProcessor) {
        super(layers, "SEQUENCER");
        Arrays.fill(stepDownTimes, -1);
        midiProcessor.addNoteHandler(this);
        this.midiProcessor = midiProcessor;
        this.formatter = host.createBeatTimeFormatter(":", 2, 1, 1, 0);
        this.globalStates = globalStates;
        this.displayControl = displayControl;
        this.padDisplay = new TempDisplayValue(displayControl, "Focus Pad", focusValue);
        gridValue = new ValueSet().add("1/2", 2.0) //
            .add("1/4", 1.0) //
            .add("1/8", 0.5) //
            .add("1/16", 0.25) //
            .add("1/32", 0.125);
        gridValue.setSelectedIndex(3);
        singlePadBank = viewControl.getPrimaryDevice().createDrumPadBank(1);
        focusDrumPad = singlePadBank.getItemAt(0);
        
        cursorTrack = viewControl.getCursorTrack();
        cursorTrack.addNoteSource(midiProcessor.getNoteInput());
        cursorTrack.position().addValueObserver(this::updateFocusClips);
        
        clipState = createLauncherClipState();
        this.encoderLayer =
            new OverlayEncoderLayer(layers, hwElements.getIncEncoders(), clipState, displayControl, gridValue);
        
        initPadKeyUpdate(viewControl, globalStates);
        
        final RgbButton[] buttons = hwElements.getSessionButtons();
        for (int i = 0; i < STEPS; i++) {
            final int index = i;
            final RgbButton button = buttons[i];
            button.bindLight(this, () -> getStepLight(index));
            button.bindPressed(this, () -> handleSequencerPressed(index));
            button.bindReleased(this, () -> handleSequencerReleased(index));
        }
        initNavigation(hwElements);
    }
    
    private ClipState createLauncherClipState() {
        final PinnableCursorClip cursorClip = cursorTrack.createLauncherCursorClip(STEPS, 128);
        final PinnableCursorClip drumCursorClip = cursorTrack.createLauncherCursorClip(STEPS, 1);
        return new ClipState(this, cursorClip, drumCursorClip, seqMode);
    }
    
    private void updateFocusClips(final int trackIndex) {
        if (isActive()) {
            ViewControl.filterSlot(cursorTrack, slot -> slot.isPlaying().get()).ifPresent(slot -> slot.select());
        }
    }
    
    private void updateFocusName(final String padName, final boolean padExists, final int focusKey) {
        this.focusPadName = padName;
        this.focusPadExists = padExists;
        if (focusPadExists) {
            focusValue.set("%s (%s)".formatted(padName, StringUtil.toNoteValue(focusKey)));
        } else {
            focusValue.set(StringUtil.toNoteValue(focusKey));
        }
    }
    
    private void initPadKeyUpdate(final ViewControl viewControl, final GlobalStates globalStates) {
        viewControl.getPrimaryDevice().hasDrumPads().addValueObserver(hasDrumPads -> {
            this.hasDrumPads = hasDrumPads;
            seqMode.set(hasDrumPads ? ClipSeqMode.DRUM : ClipSeqMode.KEYS);
        });
        monoKeyNoteFocus.addValueObserver(focusKey -> updateFocusKey(focusKey));
        singlePadBank.scrollPosition()
            .addValueObserver(pos -> this.updateFocusName(focusPadName, focusPadExists, monoKeyNoteFocus.get()));
        focusDrumPad.color().addValueObserver((r, g, b) -> this.focusDrumPadColor = RgbState.get(r, g, b));
        
        focusDrumPad.name()
            .addValueObserver(name -> this.updateFocusName(name, focusPadExists, monoKeyNoteFocus.get()));
        focusDrumPad.exists()
            .addValueObserver(exists -> this.updateFocusName(focusPadName, exists, monoKeyNoteFocus.get()));
    }
    
    private void handleSequencerPressed(final int index) {
        if (!clipState.getNotesCursorClip().exists().get()) {
            ViewControl.filterSlot(cursorTrack, slot -> !slot.hasContent().get()).ifPresent(slot -> {
                slot.createEmptyClip(4);
                slot.select();
                slot.launch();
            });
        } else if (seqMode.get() == ClipSeqMode.KEYS) {
            stepDownTimes[index] = System.currentTimeMillis();
            handleSelectionEnter(index);
        } else {
            stepDownTimes[index] = System.currentTimeMillis();
            handleSelectionEnterDrum(index);
        }
        if (clipState.hasHeldSteps() && !encoderLayer.isActive()) {
            controlHandler.activateLayer(encoderLayer);
        }
    }
    
    private void handleSelectionEnter(final int index) {
        clipState.applySelection(index);
        displayControl.getTemporaryDisplay()
            .showParamInfo("Note Edit", "Vel", "Len", "L.Fine", "RND", "Rpt", "Rcr", "Tim", "AT");
    }
    
    private void handleSequencerReleased(final int index) {
        final long holdTime = System.currentTimeMillis() - stepDownTimes[index];
        if (seqMode.get() == ClipSeqMode.KEYS) {
            if (holdTime < 400) {
                clipState.toggleValues(index);
            }
            clipState.removeSelection(index);
        } else {
            if (holdDelayEvent != null) {
                holdDelayEvent.cancel();
            }
            if (!copyState.get()) {
                if (holdTime < 400 && !justEntered[index]) {
                    clipState.removeDrumStep(index);
                }
                clipState.removeSelection(index);
            }
        }
        justEntered[index] = false;
        if (!clipState.hasHeldSteps() && encoderLayer.isActive()) {
            controlHandler.releaseLayer(encoderLayer);
        }
    }
    
    private void handleSelectionEnterDrum(final int index) {
        final INoteStepSlot noteSlot = clipState.getAssignment(index);
        clipState.applySelection(index);
        if (!noteSlot.hasNotes()) {
            //clipState.addDrumStep(index, (int) (controlPages.getVelocityValue().getSelectValue() * 127));
            clipState.addDrumStep(index, 100);
            justEntered[index] = true;
        }
        launchTimedEvent(() -> {
            clipState.applySelection(index);
            //controlPages.applySelection(noteSlot);
        }, 400);
    }
    
    
    private void launchTimedEvent(final Runnable action, final int delay) {
        if (holdDelayEvent != null) {
            holdDelayEvent.cancel();
        }
        holdDelayEvent = new TimedDelayEvent(action, delay);
        midiProcessor.queueTimedEvent(holdDelayEvent);
    }
    
    
    private RgbState getStepLight(final int index) {
        if (!clipState.stepIndexInLoop(index)) {
            return RgbState.OFF;
        }
        final INoteStepSlot assignment = clipState.getAssignment(index);
        final int playingStep = clipState.getPlayingStep();
        if (index == playingStep) {
            return RgbState.WHITE;
        }
        if (assignment.hasNotes()) {
            // TODO Copy index  copyIndex == index  STEP_CPY_COLOR
            if (seqMode.get() == ClipSeqMode.KEYS || focusDrumPadColor == RgbState.OFF) {
                return globalStates.getTrackColor();
            } else {
                return focusDrumPadColor;
            }
        }
        return RgbState.OFF;
    }
    
    private void updateFocusKey(final int focusKey) {
        if (hasDrumPads) {
            this.updateFocusName(focusPadName, focusPadExists, focusKey);
            singlePadBank.scrollPosition().set(focusKey);
            if (isActive()) {
                padDisplay.notifyUpdate();
            }
        }
    }
    
    private void initNavigation(final LaunchkeyHwElements hwElements) {
        final RgbButton navUpButton = hwElements.getButton(CcAssignments.NAV_UP);
        final RgbButton navDownButton = hwElements.getButton(CcAssignments.NAV_DOWN);
        navUpButton.bindRepeatHold(this, () -> navigate(1), 400, 50);
        navUpButton.bindLightPressed(this, this::canScrollUp);
        navDownButton.bindRepeatHold(this, () -> navigate(-1), 400, 50);
        navDownButton.bindLightPressed(this, this::canScrollDown);
    }
    
    private boolean canScrollDown() {
        return false;
    }
    
    private boolean canScrollUp() {
        return false;
    }
    
    private void navigate(final int dir) {
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        controlHandler.releaseLayer(encoderLayer);
    }
    
    @Override
    public void handleNoteAction(final int note, final int velocity) {
        if (!isActive()) {
            return;
        }
        if (seqMode.get() == ClipSeqMode.KEYS) {
            clipState.handleNoteAction(note, velocity);
        } else if (velocity > 0) {
            monoKeyNoteFocus.set(note);
        }
    }
    
    @Override
    public ValueSet getGridResolution() {
        return gridValue;
    }
    
    @Override
    public BeatTimeFormatter getBeatTimeFormatter() {
        return this.formatter;
    }
    
    @Override
    public IntValue getMonoKeyNoteFocus() {
        return monoKeyNoteFocus;
    }
    
}
