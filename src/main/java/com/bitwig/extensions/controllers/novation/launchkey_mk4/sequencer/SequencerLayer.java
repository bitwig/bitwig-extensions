package com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.bitwig.extension.controller.api.BeatTimeFormatter;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
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
import com.bitwig.extensions.controllers.novation.launchkey_mk4.LayerPool;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.MidiProcessor;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.SessionLayer;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.ViewControl;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.RgbButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.DisplayControl;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.DisplayValueTracker;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.StringUtil;
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

@Component
public class SequencerLayer extends Layer implements NoteHandler, SequencerSource {
    private static final int STEPS = 16;
    private final CursorTrack cursorTrack;
    private RgbState focusDrumPadColor;
    private RgbState clipColor = RgbState.WHITE;
    private final DisplayControl displayControl;
    private final LayerPool layerPool;
    private final long[] stepDownTimes = new long[STEPS];
    private final boolean[] justEntered = new boolean[STEPS];
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
    private final BasicStringValue focusValue = new BasicStringValue();
    private final OverlayEncoderLayer encoderLayer;
    private final Layer functionLayer;
    private TimedDelayEvent holdDelayEvent = null;
    private final Set<Integer> copiedHeld = new HashSet<>();
    private final Set<Integer> copySet = new HashSet<>();
    private CopyState copyState = CopyState.COPY;
    
    private boolean functionJustPressed;
    private final DisplayValueTracker padDisplay;
    private DisplayValueTracker pageDisplay;
    private final DisplayValueTracker gridDisplay;
    
    @Inject
    private ControlHandler controlHandler;
    @Inject
    private SessionLayer sessionLayer;
    
    private enum CopyState {
        COPY,
        PASTE
    }
    
    public SequencerLayer(final Layers layers, final ViewControl viewControl, final LaunchkeyHwElements hwElements,
        final GlobalStates globalStates, final ControllerHost host, final DisplayControl displayControl,
        final MidiProcessor midiProcessor, final LayerPool layerPool) {
        super(layers, "SEQUENCER");
        Arrays.fill(stepDownTimes, -1);
        midiProcessor.addNoteHandler(this);
        this.midiProcessor = midiProcessor;
        this.layerPool = layerPool;
        this.functionLayer = new Layer(layers, "FUNCTION_LAYER");
        this.formatter = host.createBeatTimeFormatter(":", 2, 1, 1, 0);
        this.globalStates = globalStates;
        this.displayControl = displayControl;
        this.padDisplay = new DisplayValueTracker(displayControl, new BasicStringValue("Focus Pad"), focusValue);
        gridValue = new ValueSet().add("1/2", 2.0) //
            .add("1/4", 1.0) //
            .add("1/8", 0.5) //
            .add("1/16", 0.25) //
            .add("1/32", 0.125);
        gridValue.setSelectedIndex(3);
        singlePadBank = viewControl.getPrimaryDevice().createDrumPadBank(1);
        focusDrumPad = singlePadBank.getItemAt(0);
        
        gridDisplay =
            new DisplayValueTracker(displayControl, new BasicStringValue("Grid"), gridValue.getDisplayValue());
        
        cursorTrack = viewControl.getCursorTrack();
        cursorTrack.addNoteSource(midiProcessor.getNoteInput());
        cursorTrack.position().addValueObserver(this::updateFocusClips);
        
        clipState = createLauncherClipState();
        clipState.getNotesCursorClip().color().addValueObserver((r, g, b) -> clipColor = RgbState.get(r, g, b));
        this.encoderLayer =
            new OverlayEncoderLayer(layers, hwElements.getIncEncoders(), clipState, displayControl, gridValue);
        
        initPadKeyUpdate(viewControl);
        
        final RgbButton[] buttons = hwElements.getSessionButtons();
        for (int i = 0; i < STEPS; i++) {
            final int index = i;
            final RgbButton button = buttons[i];
            button.bindLight(this, () -> getStepLight(index));
            button.bindPressed(this, () -> handleSequencerPressed(index));
            button.bindReleased(this, () -> handleSequencerReleased(index));
            button.bindPressed(functionLayer, () -> handleCopyPressed(index));
            button.bindReleased(functionLayer, () -> handleCopyReleased(index));
        }
        initNavigation(hwElements);
        
        final Layer sceneLayer = layerPool.getSceneControlSequencer();
        final RgbButton functionButton = hwElements.getButton(CcAssignments.LAUNCH_MODE);
        functionButton.bindIsPressed(sceneLayer, this::handleFunctionPressed);
        functionButton.bindLightPressed(sceneLayer, pressedState -> {
            if (layerPool.getSequencerControl().get()) {
                return RgbState.OFF;
            }
            return functionLayer.isActive() ? RgbState.WHITE : RgbState.DIM_WHITE;
        });
        
        final RgbButton sceneLaunchButton = hwElements.getButton(CcAssignments.SCENE_LAUNCH);
        sceneLaunchButton.bindIsPressed(sceneLayer, this::handleSelectMode);
        sceneLaunchButton.bindLight(
            sceneLayer, () -> layerPool.getSequencerControl().get() ? RgbState.WHITE : RgbState.DIM_WHITE);
        
        layerPool.getSequencerControl().addValueObserver(v -> {
            sessionLayer.setIsActive(v);
            this.setIsActive(!v);
            if (v) {
                displayControl.show2Line("Sequencer", "Select Clip");
            } else {
                displayControl.revertToFixed();
            }
        });
    }
    
    private void handleFunctionPressed(final boolean pressed) {
        if (!layerPool.getSequencerControl().get()) {
            functionLayer.setIsActive(pressed);
            if (pressed) {
                displayControl.show2Line("Function", "Duplicate");
                initCopyState();
            } else {
                displayControl.revertToFixed();
            }
        }
        this.functionJustPressed = pressed;
    }
    
    private void handleSelectMode(final boolean pressed) {
        if (pressed) {
            layerPool.getSequencerControl().toggle();
        }
    }
    
    private ClipState createLauncherClipState() {
        final PinnableCursorClip cursorClip = cursorTrack.createLauncherCursorClip(STEPS, 128);
        final PinnableCursorClip drumCursorClip = cursorTrack.createLauncherCursorClip(STEPS, 1);
        return new ClipState(this, cursorClip, drumCursorClip, seqMode);
    }
    
    private void updateFocusClips(final int trackIndex) {
        if (isActive()) {
            ViewControl.filterSlot(cursorTrack, slot -> slot.isPlaying().get()).ifPresent(ClipLauncherSlot::select);
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
    
    private void initPadKeyUpdate(final ViewControl viewControl) {
        viewControl.getPrimaryDevice().hasDrumPads().addValueObserver(hasDrumPads -> {
            this.hasDrumPads = hasDrumPads;
            seqMode.set(hasDrumPads ? ClipSeqMode.DRUM : ClipSeqMode.KEYS);
        });
        monoKeyNoteFocus.addValueObserver(this::updateFocusKey);
        singlePadBank.scrollPosition()
            .addValueObserver(pos -> this.updateFocusName(focusPadName, focusPadExists, monoKeyNoteFocus.get()));
        focusDrumPad.color().addValueObserver((r, g, b) -> this.focusDrumPadColor = RgbState.get(r, g, b));
        
        focusDrumPad.name()
            .addValueObserver(name -> this.updateFocusName(name, focusPadExists, monoKeyNoteFocus.get()));
        focusDrumPad.exists()
            .addValueObserver(exists -> this.updateFocusName(focusPadName, exists, monoKeyNoteFocus.get()));
    }
    
    private void initCopyState() {
        this.copyState = CopyState.COPY;
        copiedHeld.clear();
        copySet.clear();
        clipState.clearCopyNotes();
    }
    
    private void handleCopyPressed(final int index) {
        copiedHeld.add(index);
        final INoteStepSlot notes = clipState.getAssignment(index);
        
        if (copyState == CopyState.COPY) {
            if (notes.hasNotes()) {
                copySet.add(index);
                clipState.moveToBuffer(index);
                displayControl.show2Line("Notes", "copied");
            }
        } else if (!copySet.isEmpty()) {
            clipState.doPaste(index);
            displayControl.show2Line("Notes", "pasted");
            initCopyState();
        }
    }
    
    private void handleCopyReleased(final int index) {
        final int outGoingCopy = copiedHeld.size();
        copiedHeld.remove(index);
        if (outGoingCopy > 0 && copiedHeld.isEmpty() && !copySet.isEmpty()) {
            if (copyState == CopyState.COPY) {
                copyState = CopyState.PASTE;
            }
        }
    }
    
    private void handleSequencerPressed(final int index) {
        if (!clipState.getNotesCursorClip().exists().get()) {
            ViewControl.filterSlot(cursorTrack, slot -> !slot.hasContent().get()).ifPresent(slot -> {
                slot.createEmptyClip(4);
                slot.select();
                slot.launch();
            });
            return;
        }
        if (!clipState.getPositionHandler().stepIndexInLoop(index)) {
            clipState.getPositionHandler().expandClipToPagePosition(index);
        }
        final int previousHeldSteps = clipState.heldSteps();
        if (seqMode.get() == ClipSeqMode.KEYS) {
            stepDownTimes[index] = System.currentTimeMillis();
            clipState.applySelection(index);
        } else {
            stepDownTimes[index] = System.currentTimeMillis();
            handleSelectionEnterDrum(index);
        }
        
        if (previousHeldSteps > 0 && clipState.heldSteps() == 0) {
            displayControl.revertToFixed();
        }
        
        if (clipState.hasHeldSteps() && !encoderLayer.isActive()) {
            launchTimedEvent(() -> {
                displayControl.getTemporaryDisplay()
                    .showParamInfo("Note Edit", "Vel", "Len", "L.Fine", "RND", "Rpt", "Rcr", "Tim", "AT");
                controlHandler.activateLayer(encoderLayer);
            }, 400);
        }
    }
    
    private void handleSequencerReleased(final int index) {
        final long holdTime = System.currentTimeMillis() - stepDownTimes[index];
        if (seqMode.get() == ClipSeqMode.KEYS) {
            if (holdTime < 400) {
                clipState.toggleValues(index);
            }
            clipState.removeSelection(index);
        } else {
            if (holdTime < 400 && !justEntered[index]) {
                clipState.removeDrumStep(index);
            }
            clipState.removeSelection(index);
        }
        justEntered[index] = false;
        if (!clipState.hasHeldSteps()) {
            cancelHoldEvent();
        }
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
    }
    
    private void cancelHoldEvent() {
        if (holdDelayEvent != null) {
            holdDelayEvent.cancel();
        }
    }
    
    private void launchTimedEvent(final Runnable action, final int delay) {
        cancelHoldEvent();
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
            if (seqMode.get() == ClipSeqMode.KEYS || focusDrumPadColor == RgbState.OFF) {
                return clipColor;
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
        final BasicStringValue pagePosition =
            new BasicStringValue(getPositionValue(clipState.getPositionHandler().getPagePosition().get()));
        final BasicStringValue loopLength = new BasicStringValue();
        clipState.getPositionHandler().getPagePosition().addValueObserver(v -> pagePosition.set(getPositionValue(v)));
        clipState.getNotesCursorClip().getLoopLength()
            .addValueObserver(v -> loopLength.set("%3.1f Bars".formatted(v / 4)));
        pageDisplay = new DisplayValueTracker(displayControl, cursorTrack.name(), pagePosition, loopLength);
        
        final RgbButton navUpButton = hwElements.getButton(CcAssignments.NAV_UP);
        final RgbButton navDownButton = hwElements.getButton(CcAssignments.NAV_DOWN);
        navUpButton.bindRepeatHold(this, () -> navigate(-1), 400, 50);
        navUpButton.bindLightPressed(this, this::canScrollUp);
        navDownButton.bindRepeatHold(this, () -> navigate(1), 400, 50);
        navDownButton.bindLightPressed(this, this::canScrollDown);
        
        navUpButton.bindPressed(functionLayer, this::duplicateClip);
        navUpButton.bindLightPressed(functionLayer, () -> false);
        navDownButton.bindPressed(functionLayer, this::changeGrid);
        navDownButton.bindLightPressed(functionLayer, () -> false);
    }
    
    private String getPositionValue(final int position) {
        final double bar = (position * (gridValue.getValue() * 4)) + 1.0;
        return "Pg %d Bar %3.1f".formatted(position + 1, bar);
    }
    
    private void duplicateClip() {
        if (clipState.getPositionHandler().getLoopLength() < 260) {
            clipState.duplicateContent();
            pageDisplay.show();
            pageDisplay.notifyUpdate();
        }
    }
    
    private void changeGrid() {
        if (functionJustPressed) {
            functionJustPressed = false;
            gridDisplay.show();
        } else {
            gridValue.incRoundRobin();
            gridDisplay.show();
            gridDisplay.notifyUpdate();
        }
    }
    
    private boolean canScrollDown() {
        return true;
    }
    
    private boolean canScrollUp() {
        return clipState.getPositionHandler().canScrollLeft().get();
    }
    
    private void navigate(final int dir) {
        final StepViewPosition positionHandler = clipState.getPositionHandler();
        if (dir < 0) {
            positionHandler.scrollLeft();
        } else {
            positionHandler.scrollRight();
        }
        pageDisplay.notifyUpdate();
        pageDisplay.show();
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
        layerPool.getSceneControlSequencer().setIsActive(true);
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        controlHandler.releaseLayer(encoderLayer);
        if (!layerPool.getSequencerControl().get()) {
            layerPool.getSceneControlSequencer().setIsActive(false);
        }
        functionLayer.setIsActive(false);
        copySet.clear();
        copiedHeld.clear();
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
