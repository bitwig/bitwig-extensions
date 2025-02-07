package com.bitwig.extensions.controllers.novation.slmk3.sequencer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.bitwig.extension.controller.api.BeatTimeFormatter;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.Groove;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.controllers.novation.slmk3.CcAssignment;
import com.bitwig.extensions.controllers.novation.slmk3.GlobalStates;
import com.bitwig.extensions.controllers.novation.slmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.slmk3.SlMk3HardwareElements;
import com.bitwig.extensions.controllers.novation.slmk3.StringUtil;
import com.bitwig.extensions.controllers.novation.slmk3.ViewControl;
import com.bitwig.extensions.controllers.novation.slmk3.control.RgbButton;
import com.bitwig.extensions.controllers.novation.slmk3.display.ButtonMode;
import com.bitwig.extensions.controllers.novation.slmk3.display.ButtonSubPanel;
import com.bitwig.extensions.controllers.novation.slmk3.display.KnobMode;
import com.bitwig.extensions.controllers.novation.slmk3.display.ScreenHandler;
import com.bitwig.extensions.controllers.novation.slmk3.display.SequencerButtonSubMode;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlotUtil;
import com.bitwig.extensions.controllers.novation.slmk3.display.panel.SelectionSubPanel;
import com.bitwig.extensions.controllers.novation.slmk3.layer.GridMode;
import com.bitwig.extensions.controllers.novation.slmk3.layer.LayerRepo;
import com.bitwig.extensions.controllers.novation.slmk3.seqcommons.ClipSeqMode;
import com.bitwig.extensions.controllers.novation.slmk3.seqcommons.INoteStepSlot;
import com.bitwig.extensions.controllers.novation.slmk3.seqcommons.SequencerLayers;
import com.bitwig.extensions.controllers.novation.slmk3.seqcommons.SequencerSource;
import com.bitwig.extensions.controllers.novation.slmk3.value.IObservableValue;
import com.bitwig.extensions.controllers.novation.slmk3.value.IntValue;
import com.bitwig.extensions.controllers.novation.slmk3.value.NoteHandler;
import com.bitwig.extensions.controllers.novation.slmk3.value.ObservableColor;
import com.bitwig.extensions.controllers.novation.slmk3.value.ValueSet;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.time.TimedDelayEvent;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.ValueObject;


@Component
public class SequencerLayer implements NoteHandler, SequencerSource {
    public static final SlRgbState RECUR_COLOR = SlRgbState.WHITE.reduced(30);
    public static final SlRgbState RECUR_COLOR_SEL = SlRgbState.ORANGE;
    
    private static final int STEPS = 16;
    private final long[] stepDownTimes = new long[STEPS];
    private final boolean[] justEntered = new boolean[STEPS];
    private TimedDelayEvent holdDelayEvent = null;
    private final IntValue monoKeyNoteFocus = new IntValue(36, 0, 127);
    private final MidiProcessor midiProcessor;
    private final BeatTimeFormatter formatter;
    private final GlobalStates globalState;
    private final ScreenHandler screenHandler;
    private boolean active;
    private final ValueSet gridValue;
    private final DrumPadBank singlePadBank;
    private final DrumPad focusDrumPad;
    private final CursorTrack cursorTrack;
    private final ClipState clipState;
    private boolean focusPadExists;
    private String focusPadName;
    private SlRgbState focusDrumPadColor;
    private SlRgbState clipColor = SlRgbState.WHITE;
    private final IObservableValue<ClipSeqMode> seqMode;
    private final SeqControlPages controlPages;
    private CopyState copyState = CopyState.OFF;
    private final Set<Integer> copiedHeld = new HashSet<>();
    private final Set<Integer> copySet = new HashSet<>();
    private final SequencerLayers padSequenceLayers;
    private final ValueObject<StepInputState> stepInputState = new ValueObject<>(StepInputState.OFF);
    private int inputStep = 0;
    private final HashSet<Integer> heldInput = new HashSet<>();
    private final BooleanValueObject clipControlActive = new BooleanValueObject();
    
    private enum CopyState {
        COPY,
        OFF,
        PASTE
    }
    
    private enum StepInputState {
        OFF,
        NOTE_HELD,
        NEXT_INPUT
    }
    
    public SequencerLayer(final LayerRepo layerRepo, final SlMk3HardwareElements hwElements,
        final MidiProcessor midiProcessor, final ViewControl viewControl, final ScreenHandler screenHandler,
        final GlobalStates globalStates, final ControllerHost host) {
        Arrays.fill(stepDownTimes, -1);
        midiProcessor.addNoteHandler(this);
        this.midiProcessor = midiProcessor;
        padSequenceLayers = layerRepo.getPadSequenceLayer();
        
        this.formatter = host.createBeatTimeFormatter(":", 2, 1, 1, 0);
        this.globalState = globalStates;
        this.screenHandler = screenHandler;
        this.globalState.getBaseMode().addValueObserver(mode -> active = mode == GridMode.SEQUENCER);
        singlePadBank = viewControl.getPrimaryDevice().createDrumPadBank(1);
        focusDrumPad = singlePadBank.getItemAt(0);
        
        initPadKeyUpdate(viewControl, globalStates);
        
        gridValue = new ValueSet().add("1/2", 2.0) //
            .add("1/4", 1.0) //
            .add("1/8", 0.5) //
            .add("1/16", 0.25) //
            .add("1/32", 0.125) //
            .add("1/4T", 2.0 / 3) //
            .add("1/8T", 1.0 / 3) //
            .add("1/16T", 1.0 / 6);
        gridValue.setSelectedIndex(3);
        seqMode = globalState.getClipSeqMode();
        
        cursorTrack = viewControl.getCursorTrack();
        cursorTrack.addNoteSource(midiProcessor.getNoteInput());
        
        clipState = createLauncherClipState();
        clipState.getNotesCursorClip().exists().markInterested();
        clipState.getNotesCursorClip().color().addValueObserver((r, g, b) -> clipColor = SlRgbState.get(r, g, b));
        controlPages =
            new SeqControlPages(layerRepo.getKnobLayer(KnobMode.SEQUENCER), screenHandler.getSequencerScreen(),
                clipState, hwElements.getEncoders(), gridValue, globalStates, viewControl);
        
        controlPages.getRecurrenceValue().addChangeListener(this::handleRecurrenceEditInvoke);
        
        final Layer stepLayer = padSequenceLayers.getStepLayer();
        final Layer copyLayer = padSequenceLayers.getCopyLayer();
        final Layer stepInputLayer = padSequenceLayers.getStepInputLayer();
        final List<RgbButton> buttons = hwElements.getPadButtons();
        for (int i = 0; i < STEPS; i++) {
            final int index = i;
            final RgbButton button = buttons.get(i);
            button.bindLight(stepLayer, () -> getStepLight(index));
            button.bindPressed(stepLayer, () -> handleSequencerPressed(index));
            button.bindReleased(stepLayer, () -> handleSequencerReleased(index));
            button.bindPressed(copyLayer, () -> handleCopyPressed(index));
            button.bindReleased(copyLayer, () -> handleCopyReleased(index));
            button.bindLight(stepInputLayer, () -> getStepInputLight(index));
            button.bindPressed(stepInputLayer, () -> handleStepInputStep(index));
            if (i < 8) {
                button.bindLight(padSequenceLayers.getTopRecurrenceLayer(), () -> getRecurrenceLight(index));
                button.bindPressed(padSequenceLayers.getTopRecurrenceLayer(), () -> editRecurrence(index));
            } else {
                button.bindLight(padSequenceLayers.getBottomRecurrenceLayer(), () -> getRecurrenceLight(index - 8));
                button.bindPressed(padSequenceLayers.getBottomRecurrenceLayer(), () -> editRecurrence(index - 8));
            }
        }
        initNavigation(layerRepo, hwElements);
        bindEncoderNavigation(layerRepo.getKnobLayer(KnobMode.SEQUENCER), hwElements);
        bindButtonRow(layerRepo.getButtonLayer(ButtonMode.SEQUENCER), hwElements, viewControl);
        bindButtonRowClip(layerRepo.getButtonLayer(ButtonMode.SEQUENCER2), hwElements, viewControl);
    }
    
    private void bindButtonRowClip(final Layer layer, final SlMk3HardwareElements hwElements,
        final ViewControl viewControl) {
        final ButtonSubPanel subPanels = screenHandler.getSubPanel(ButtonMode.SEQUENCER2);
        int index = 0;
        final Groove groove = viewControl.getGroove();
        final List<RgbButton> buttons = hwElements.getSelectButtons();
        final ObservableColor colorSource = new ObservableColor(SlRgbState.ORANGE);
        
        bindButtonHold(layer, buttons.get(index), subPanels.get(index++), "Settings", "Menu", clipControlActive);
        clipControlActive.addValueObserver(this::handleGrooveClipMenu);
        
        bindButtonBoolToggle(layer, buttons.get(index), subPanels.get(index++), "Global", "Groove", colorSource,
            groove.getEnabled().value());
        bindButtonBoolToggle(layer, buttons.get(index), subPanels.get(index++), "Clip", "Shuffle", colorSource,
            clipState.getNotesCursorClip().getShuffle());
        bindButtonAction(
            layer, buttons.get(index), subPanels.get(index++), "Clip", "Quantize", this::handleQuantize, colorSource);
        while (index < 8) {
            bindButtonEmpty(layer, buttons.get(index), subPanels.get(index++));
        }
    }
    
    private void bindButtonRow(final Layer layer, final SlMk3HardwareElements hwElements,
        final ViewControl viewControl) {
        final ButtonSubPanel subPanels = screenHandler.getSubPanel(ButtonMode.SEQUENCER);
        int index = 0;
        final Groove groove = viewControl.getGroove();
        final List<RgbButton> buttons = hwElements.getSelectButtons();
        
        bindButtonHold(layer, buttons.get(index), subPanels.get(index++), "Settings", "Menu", this.clipControlActive);
        final ObservableColor colorSource = controlPages.getModeColor();
        bindButtonAction(layer, buttons.get(index), subPanels.get(index++), "SEQ", "Mode", this::changeSeqMode,
            colorSource);
        bindButtonAction(layer, buttons.get(index), subPanels.get(index++), "Double", "Content",
            clipState::duplicateContent, colorSource);
        bindButtonAction(layer, buttons.get(index), subPanels.get(index++), "Clear", "All", this::clearNotes,
            colorSource);
        bindButtonPress(
            layer, buttons.get(index), subPanels.get(index++), "Copy /", "Paste", this::handleCopyButton, colorSource);
        
        bindButtonEmpty(layer, buttons.get(index), subPanels.get(index++));
        bindNextStep(layer, buttons.get(index), subPanels.get(index++));
        bindStepInputMode(layer, buttons.get(index), subPanels.get(index));
    }
    
    private void clearNotes() {
        if (globalState.getClearState().get()) {
            clipState.clearSteps(true);
            screenHandler.notifyMessage("Clear all", "Steps");
        } else {
            clipState.clearSteps(false);
            screenHandler.notifyMessage("Clear", "Steps");
        }
    }
    
    private void handleGrooveClipMenu(final boolean pressed) {
        if (pressed) {
            globalState.getSequencerSubMode().set(SequencerButtonSubMode.MODE_2);
            controlPages.setToClip();
        } else {
            globalState.getSequencerSubMode().set(SequencerButtonSubMode.MODE_1);
            controlPages.setToMain();
        }
    }
    
    private void bindEncoderNavigation(final Layer layer, final SlMk3HardwareElements hwElements) {
        final RgbButton prevButton = hwElements.getButton(CcAssignment.SCREEN_UP);
        final RgbButton nextButton = hwElements.getButton(CcAssignment.SCREEN_DOWN);
        final ObservableColor modeColor = controlPages.getModeColor();
        
        nextButton.bindLight(layer, () -> controlPages.canScrollDown() ? modeColor.get() : SlRgbState.OFF);
        prevButton.bindLight(layer, () -> controlPages.canScrollUp() ? modeColor.get() : SlRgbState.OFF);
        prevButton.bindPressed(layer, () -> controlPages.scrollBy(-1));
        nextButton.bindPressed(layer, () -> controlPages.scrollBy(1));
    }
    
    
    private void handleCopyButton(final boolean pressed) {
        padSequenceLayers.getCopyLayer().setIsActive(pressed);
        if (pressed) {
            initCopyState();
        } else {
            copyState = CopyState.OFF;
        }
    }
    
    private void handleQuantize() {
        if (clipState.getNotesCursorClip().exists().get()) {
            clipState.getNotesCursorClip().quantize(1);
            screenHandler.notifyMessage("Quantize", "Clip");
        } else {
            screenHandler.notifyMessage("Quantize", "No Clip");
        }
    }
    
    private void initCopyState() {
        this.copyState = CopyState.COPY;
        copiedHeld.clear();
        copySet.clear();
        clipState.clearCopyNotes();
    }
    
    private void handleRecurrenceEditInvoke() {
        final int heldIndex = clipState.heldIndex();
        if (heldIndex != -1) {
            final SequencerLayers.SeqLayerMode mode = padSequenceLayers.getMode();
            if (mode != SequencerLayers.SeqLayerMode.BOTTOM_RECURRENCE
                && mode != SequencerLayers.SeqLayerMode.TOP_RECURRENCE) {
                padSequenceLayers.setMode(heldIndex < 8
                    ? SequencerLayers.SeqLayerMode.BOTTOM_RECURRENCE
                    : SequencerLayers.SeqLayerMode.TOP_RECURRENCE);
            }
        }
    }
    
    
    private void handleCopyPressed(final int index) {
        copiedHeld.add(index);
        final INoteStepSlot notes = clipState.getAssignment(index);
        
        if (copyState == CopyState.COPY) {
            if (notes.hasNotes()) {
                copySet.add(index);
                clipState.moveToBuffer(index);
                screenHandler.notifyMessage("Notes", "copied");
                
            }
        } else if (!copySet.isEmpty()) {
            clipState.doPaste(index);
            screenHandler.notifyMessage("Notes", "pasted");
            initCopyState();
        }
    }
    
    private void handleStepInputStep(final int index) {
        if (globalState.getClearState().get()) {
            clipState.clearSteps(index);
        } else {
            inputStep = index;
            stepInputState.set(StepInputState.NEXT_INPUT);
            heldInput.clear();
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
    
    private void initPadKeyUpdate(final ViewControl viewControl, final GlobalStates globalStates) {
        viewControl.getPrimaryDevice().hasDrumPads().addValueObserver(hasDrumPads -> {
            this.globalState.getHasDrumPads().set(hasDrumPads);
            this.globalState.getClipSeqMode().set(hasDrumPads ? ClipSeqMode.DRUM : ClipSeqMode.KEYS);
            updatePadName();
        });
        monoKeyNoteFocus.addValueObserver(focusKey -> updateFocusKey(focusKey));
        singlePadBank.scrollPosition().addValueObserver(pos -> updatePadName());
        focusDrumPad.color().addValueObserver((r, g, b) -> this.focusDrumPadColor = SlRgbState.get(r, g, b));
        focusDrumPad.exists().addValueObserver(exists -> {
            focusPadExists = exists;
            updatePadName();
        });
        focusDrumPad.name().addValueObserver(name -> {
            focusPadName = name;
            updatePadName();
        });
    }
    
    private void updatePadName() {
        if (globalState.getHasDrumPads().get() && focusPadExists) {
            globalState.getPadSelectionInfo().set(focusPadName);
        } else {
            globalState.getPadSelectionInfo().set("%s".formatted(StringUtil.toNoteValue(monoKeyNoteFocus.get())));
        }
    }
    
    private void updateFocusKey(final int focusKey) {
        if (globalState.getHasDrumPads().get()) {
            singlePadBank.scrollPosition().set(focusKey);
            globalState.getPadSelectionInfo().set(focusDrumPad.name().get());
        } else if (seqMode.get() == ClipSeqMode.DRUM) {
            updatePadName();
        }
    }
    
    private ClipState createLauncherClipState() {
        final PinnableCursorClip cursorClip = cursorTrack.createLauncherCursorClip(STEPS, 128);
        final PinnableCursorClip drumCursorClip = cursorTrack.createLauncherCursorClip(STEPS, 1);
        return new ClipState(this, cursorClip, drumCursorClip, globalState.getClipSeqMode());
    }
    
    private ClipState createArrangerClipState(final ControllerHost host) {
        final Clip cursorClip = host.createLauncherCursorClip(STEPS, 128);
        final Clip drumCursorClip = host.createLauncherCursorClip(STEPS, 1);
        return new ClipState(this, cursorClip, drumCursorClip, globalState.getClipSeqMode());
    }
    
    @Override
    public IntValue getMonoKeyNoteFocus() {
        return monoKeyNoteFocus;
    }
    
    @Override
    public BeatTimeFormatter getBeatTimeFormatter() {
        return formatter;
    }
    
    private void initNavigation(final LayerRepo layerRepo, final SlMk3HardwareElements hwElements) {
        final RgbButton navUp = hwElements.getButton(CcAssignment.PADS_UP);
        final RgbButton navDown = hwElements.getButton(CcAssignment.PADS_DOWN);
        final RgbButton scene1Button = hwElements.getButton(CcAssignment.SCENE_LAUNCH_1);
        final RgbButton scene2Button = hwElements.getButton(CcAssignment.SCENE_LAUNCH_2);
        final SequencerLayers stepLayer = layerRepo.getPadSequenceLayer();
        final Clip cursorClip = clipState.getNotesCursorClip();
        final ClipLauncherSlot slot = cursorClip.clipLauncherSlot();
        cursorClip.getTrack().isQueuedForStop().markInterested();
        slot.isPlaying().markInterested();
        final ObservableColor modeColor = controlPages.getModeColor();
        
        scene1Button.bindPressed(stepLayer, cursorClip::launch);
        scene1Button.bindHoldDelay(stepLayer, () -> launchClipIfNotPlaying(cursorClip), () -> stopClip(cursorClip),
            500);
        scene1Button.bindLight(stepLayer,
            () -> SlotUtil.determineClipColor(slot, cursorClip.getTrack(), clipState.getClipColor().get(), false));
        scene2Button.bindIsPressed(stepLayer, this::handleSelectPressed);
        scene2Button.bindLightOnPressed(stepLayer, modeColor);
        
        final Layer selectLayer = layerRepo.getSelectClipLayer();
        
        scene1Button.bindPressed(selectLayer, () -> {});
        scene1Button.bindLight(selectLayer, () -> SlRgbState.OFF);
        scene2Button.bindIsPressed(selectLayer, this::handleSelectPressed);
        scene2Button.bindLightOnPressed(selectLayer, SlRgbState.WHITE_DIM, SlRgbState.OFF);
        
        navUp.bindLight(
            stepLayer, () -> clipState.canScrollPositionLeft() ? modeColor.get() : modeColor.getDimmedColor());
        navDown.bindLight(stepLayer, () -> modeColor.get());
        navUp.bindRepeatHold(stepLayer, () -> clipState.scrollPosition(-1), 400, 50);
        navDown.bindRepeatHold(stepLayer, () -> clipState.scrollPosition(1), 400, 50);
    }
    
    private void stopClip(final Clip cursorClip) {
        cursorClip.getTrack().stop();
    }
    
    private void launchClipIfNotPlaying(final Clip cursorClip) {
        if (!cursorClip.clipLauncherSlot().isPlaying().get()) {
            cursorClip.launch();
        }
    }
    
    private void handleSelectPressed(final boolean pressed) {
        if (pressed) {
            screenHandler.notifyMessage("Select", "Clip");
        }
        globalState.getBaseMode().set(pressed ? GridMode.SELECT : GridMode.SEQUENCER);
    }
    
    private void changeSeqMode() {
        final IObservableValue<ClipSeqMode> seqMode = globalState.getClipSeqMode();
        if (seqMode.get() == ClipSeqMode.DRUM) {
            seqMode.set(ClipSeqMode.KEYS);
        } else {
            seqMode.set(ClipSeqMode.DRUM);
        }
    }
    
    private void bindNextStep(final Layer layer, final RgbButton button, final SelectionSubPanel panel) {
        applyStep(panel);
        stepInputState.addValueObserver(state -> applyStep(panel));
        button.bindPressed(layer, this::advanceStep);
        button.bindLightOnPressed(layer, pressed -> {
            if (padSequenceLayers.getMode() == SequencerLayers.SeqLayerMode.STEP_INPUT) {
                return pressed ? SlRgbState.RED : SlRgbState.RED_DIM;
            }
            return SlRgbState.OFF;
        });
    }
    
    private void applyStep(final SelectionSubPanel panel) {
        if (padSequenceLayers.getMode() == SequencerLayers.SeqLayerMode.STEP_INPUT) {
            panel.setColor(SlRgbState.RED);
            panel.setRow1("Insert");
            panel.setRow2("Pause");
        } else {
            panel.setColor(SlRgbState.OFF);
            panel.setRow1("");
            panel.setRow2("");
        }
    }
    
    private void bindStepInputMode(final Layer layer, final RgbButton button, final SelectionSubPanel panel) {
        panel.setColor(SlRgbState.RED);
        panel.setRow1("Step");
        panel.setRow2("Input");
        panel.setSelected(false);
        final SlRgbState color = SlRgbState.RED;
        stepInputState.addValueObserver((newValue -> panel.setSelected(newValue != StepInputState.OFF)));
        button.bindPressed(layer, this::toggleStepInput);
        button.bindLight(layer, () -> stepInputState.get() == StepInputState.OFF ? color.reduced(10) : color);
    }
    
    private void toggleStepInput() {
        if (padSequenceLayers.getMode() == SequencerLayers.SeqLayerMode.STEP_INPUT) {
            padSequenceLayers.reset();
            stepInputState.set(StepInputState.OFF);
            controlPages.setToMain();
        } else {
            padSequenceLayers.setMode(SequencerLayers.SeqLayerMode.STEP_INPUT);
            stepInputState.set(StepInputState.NEXT_INPUT);
            controlPages.setToStepInput();
        }
        inputStep = 0;
        heldInput.clear();
    }
    
    private void editRecurrence(final int index) {
        controlPages.getRecurrencePattern().toggleMask(index);
    }
    
    private SlRgbState getRecurrenceLight(final int index) {
        if (index < controlPages.getRecurrencePattern().getLength()) {
            final int maskValue = (controlPages.getRecurrencePattern().getMask() >> index) & 0x1;
            return maskValue > 0 ? RECUR_COLOR_SEL : RECUR_COLOR;
        }
        return SlRgbState.OFF;
    }
    
    private void bindButtonHold(final Layer layer, final RgbButton button, final SelectionSubPanel panel,
        final String title1, final String title2, final BooleanValueObject value) {
        final ObservableColor modeColor = controlPages.getModeColor();
        panel.setColor(modeColor.get());
        modeColor.addValueObserver(color -> panel.setColor(color));
        panel.setRow1(title1);
        panel.setRow2(title2);
        value.addValueObserver(v -> panel.setSelected(v));
        button.bindPressed(layer, () -> value.set(true));
        button.bindReleased(layer, () -> value.set(false));
        button.bindLightOnPressed(layer, modeColor); // Questionable
    }
    
    private void bindButtonAction(final Layer layer, final RgbButton button, final SelectionSubPanel panel,
        final String title1, final String title2, final Runnable action, final ObservableColor colorSource) {
        panel.setColor(colorSource.get());
        colorSource.addValueObserver(color -> panel.setColor(color));
        panel.setRow1(title1);
        panel.setRow2(title2);
        button.bindPressed(layer, action);
        button.bindLightOnPressed(layer, colorSource);
    }
    
    private void bindButtonPress(final Layer layer, final RgbButton button, final SelectionSubPanel panel,
        final String title1, final String title2, final Consumer<Boolean> action, final ObservableColor colorSource) {
        panel.setColor(colorSource.get());
        colorSource.addValueObserver(color -> panel.setColor(color));
        panel.setRow1(title1);
        panel.setRow2(title2);
        panel.setSelected(false);
        button.bindIsPressed(layer, action);
        button.bindLightOnPressed(layer, colorSource);
    }
    
    private void bindButtonBoolToggle(final Layer layer, final RgbButton button, final SelectionSubPanel panel,
        final String title1, final String title2, final ObservableColor colorSource, final SettableRangedValue value) {
        panel.setColor(colorSource.get());
        colorSource.addValueObserver(color -> panel.setColor(color));
        panel.setRow1(title1);
        panel.setRow2(title2);
        value.addValueObserver(v -> panel.setSelected(v > 0));
        button.bindPressed(layer, () -> value.set(value.get() > 0 ? 0 : 1));
        button.bindLightOnPressed(layer, colorSource.get(), () -> value.get() > 0);
    }
    
    private void bindButtonBoolToggle(final Layer layer, final RgbButton button, final SelectionSubPanel panel,
        final String title1, final String title2, final ObservableColor colorSource, final SettableBooleanValue value) {
        panel.setColor(colorSource.get());
        colorSource.addValueObserver(color -> panel.setColor(color));
        panel.setRow1(title1);
        panel.setRow2(title2);
        value.addValueObserver(panel::setSelected);
        button.bindPressed(layer, value::toggle);
        button.bindLightOnPressed(layer, colorSource.get(), value);
    }
    
    private void bindButtonEmpty(final Layer layer, final RgbButton button, final SelectionSubPanel panel) {
        panel.setColor(SlRgbState.OFF);
        panel.setRow1("");
        panel.setRow2("");
        button.bindPressed(layer, () -> {});
        button.bindLight(layer, () -> SlRgbState.OFF);
    }
    
    private void launchTimedEvent(final Runnable action, final int delay) {
        if (holdDelayEvent != null) {
            holdDelayEvent.cancel();
        }
        holdDelayEvent = new TimedDelayEvent(action, delay);
        midiProcessor.queueTimedEvent(holdDelayEvent);
    }
    
    
    private void notifySelectedSteps(final int index) {
        final INoteStepSlot slot = clipState.getAssignment(index);
        if (slot.hasNotes()) {
            screenHandler.notifyMessage("Select Steps",
                slot.steps().stream().map(step -> StringUtil.toNoteValueTight(step.y())).sorted()
                    .collect(Collectors.joining(" ")));
        }
    }
    
    private SlRgbState getStepInputLight(final int index) {
        if (inputStep == index) {
            return SlRgbState.RED_PULSE;
        }
        if (!clipState.stepIndexInLoop(index)) {
            return SlRgbState.OFF;
        }
        final INoteStepSlot assignment = clipState.getAssignment(index);
        final int playingStep = clipState.getPlayingStep();
        if (index == playingStep) {
            return SlRgbState.WHITE;
        }
        if (assignment.hasNotes()) {
            if (globalState.getClipSeqMode().get() == ClipSeqMode.KEYS || focusDrumPadColor == null) {
                return clipColor;
            } else {
                return focusDrumPadColor;
            }
        }
        return SlRgbState.DARK_GRAY;
    }
    
    private SlRgbState getStepLight(final int index) {
        if (!clipState.stepIndexInLoop(index)) {
            return SlRgbState.OFF;
        }
        final INoteStepSlot assignment = clipState.getAssignment(index);
        final int playingStep = clipState.getPlayingStep();
        if (index == playingStep) {
            return SlRgbState.WHITE;
        }
        if (assignment.hasNotes()) {
            if (globalState.getClipSeqMode().get() == ClipSeqMode.KEYS || focusDrumPadColor == null) {
                return clipColor;
            } else {
                return focusDrumPadColor;
            }
        }
        return SlRgbState.OFF;
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
        if (seqMode.get() == ClipSeqMode.KEYS) {
            stepDownTimes[index] = System.currentTimeMillis();
            clipState.applySelection(index);
            notifySelectedSteps(index);
        } else {
            stepDownTimes[index] = System.currentTimeMillis();
            handleSelectionEnterDrum(index);
        }
        launchTimedEvent(() -> controlPages.updateNotes(), 100);
    }
    
    private boolean handleSelectionEnterDrum(final int index) {
        final INoteStepSlot noteSlot = clipState.getAssignment(index);
        clipState.applySelection(index);
        if (!noteSlot.hasNotes()) {
            clipState.addDrumStep(index, 100);
            justEntered[index] = true;
            return true;
        }
        return false;
    }
    
    private void cancelHoldEvent() {
        if (holdDelayEvent != null) {
            holdDelayEvent.cancel();
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
            padSequenceLayers.reset();
        }
        controlPages.updateNotes();
    }
    
    
    @Override
    public boolean isActive() {
        return active;
    }
    
    @Override
    public ValueSet getGridResolution() {
        return gridValue;
    }
    
    @Override
    public int getHeldIndex() {
        return 0;
    }
    
    @Override
    public void handleNoteAction(final int note, final int velocity) {
        if (!isActive()) {
            return;
        }
        if (stepInputState.get() == StepInputState.OFF) {
            if (globalState.getClipSeqMode().get() == ClipSeqMode.KEYS) {
                clipState.handleNoteAction(note, velocity);
                if (clipState.hasHeldSteps()) {
                    launchTimedEvent(() -> controlPages.updateNotes(), 100);
                }
            } else if (velocity > 0) {
                monoKeyNoteFocus.set(note);
            }
        } else {
            final int currentlyHeld = heldInput.size();
            final int loopStepEnd = clipState.getLastStep();
            final StepInputAdvanceType stepInputAdvanceType = controlPages.getAdvanceMethod().get();
            if (velocity > 0) {
                if (currentlyHeld == 0 && !controlPages.getOverdubMode().get()) {
                    clipState.clearSteps(inputStep);
                }
                if (!clipState.stepIndexInLoop(inputStep) && stepInputAdvanceType.isExpands()) {
                    clipState.expandToPosition();
                }
                final int fixedVelocity = controlPages.getFixedVelocity().get();
                clipState.addNote(inputStep, note, fixedVelocity == 0 ? velocity : fixedVelocity,
                    controlPages.getStepDuration());
                heldInput.add(note);
            } else {
                heldInput.remove(note);
            }
            if (heldInput.isEmpty() && currentlyHeld > 0) {
                advanceStep(loopStepEnd, stepInputAdvanceType, controlPages.getStepSkip().get());
            }
        }
    }
    
    private void advanceStep() {
        if (padSequenceLayers.getMode() != SequencerLayers.SeqLayerMode.STEP_INPUT) {
            return;
        }
        final StepInputAdvanceType stepInputAdvanceType = controlPages.getAdvanceMethod().get();
        final int loopStepEnd = clipState.getLastStep();
        advanceStep(loopStepEnd, stepInputAdvanceType, 1);
    }
    
    private void advanceStep(final int loopStepEnd, final StepInputAdvanceType stepInputAdvanceType, final int amount) {
        final int nextStep = inputStep + amount;
        if (gridValue.getValue() <= 0.25) {
            if (nextStep > 15 && stepInputAdvanceType.isNextPage()) {
                clipState.advancePage(stepInputAdvanceType.isExpands());
            }
            inputStep = nextStep % 16;
        } else {
            if (nextStep >= loopStepEnd && stepInputAdvanceType.isNextPage()) {
                if (stepInputAdvanceType.isExpands()) {
                    if (nextStep == 16) {
                        clipState.advancePage(true);
                    }
                    inputStep = nextStep % 16;
                } else {
                    clipState.advancePage(false);
                    inputStep = 0;
                }
            } else {
                if (loopStepEnd == 0) {
                    clipState.goToStart();
                    inputStep = 0;
                } else {
                    inputStep = nextStep % loopStepEnd;
                }
            }
        }
    }
    
    public void removeNotes(final int note) {
        clipState.removeNote(note);
    }
}
