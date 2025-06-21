package com.bitwig.extensions.controllers.novation.slmk3.sequencer;

import java.util.List;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.Groove;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.SettableBeatTimeValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.novation.slmk3.GlobalStates;
import com.bitwig.extensions.controllers.novation.slmk3.ViewControl;
import com.bitwig.extensions.controllers.novation.slmk3.control.SlEncoder;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;
import com.bitwig.extensions.controllers.novation.slmk3.display.panel.BoxPanel;
import com.bitwig.extensions.controllers.novation.slmk3.display.panel.ScreenSetup;
import com.bitwig.extensions.controllers.novation.slmk3.seqcommons.ClipSeqMode;
import com.bitwig.extensions.controllers.novation.slmk3.seqcommons.StepViewPosition;
import com.bitwig.extensions.controllers.novation.slmk3.sequencer.value.BooleanControlValue;
import com.bitwig.extensions.controllers.novation.slmk3.sequencer.value.ControlValue;
import com.bitwig.extensions.controllers.novation.slmk3.sequencer.value.DoubleValueControlValue;
import com.bitwig.extensions.controllers.novation.slmk3.sequencer.value.IntegerControlValue;
import com.bitwig.extensions.controllers.novation.slmk3.sequencer.value.NoteDoubleControlValue;
import com.bitwig.extensions.controllers.novation.slmk3.sequencer.value.NoteDurationControlValue;
import com.bitwig.extensions.controllers.novation.slmk3.sequencer.value.NoteIntControlValue;
import com.bitwig.extensions.controllers.novation.slmk3.sequencer.value.NoteOccurrenceValue;
import com.bitwig.extensions.controllers.novation.slmk3.sequencer.value.NoteRecurrencePattern;
import com.bitwig.extensions.controllers.novation.slmk3.sequencer.value.NoteRecurrenceValue;
import com.bitwig.extensions.controllers.novation.slmk3.sequencer.value.ObjectControlValue;
import com.bitwig.extensions.controllers.novation.slmk3.sequencer.value.RangeValueControlValue;
import com.bitwig.extensions.controllers.novation.slmk3.value.IObservableValue;
import com.bitwig.extensions.controllers.novation.slmk3.value.IntValue;
import com.bitwig.extensions.controllers.novation.slmk3.value.ObservableColor;
import com.bitwig.extensions.controllers.novation.slmk3.value.ValueSet;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.ValueObject;

public class SeqControlPages {
    
    public static final int CLIP_PAGE_INDEX = 2;
    public static final int STEP_PAGE_INDEX = 3;
    
    private final ObservableColor modeColor;
    private final StepViewPosition positionHandler;
    private final PanelPage[] pages = new PanelPage[4];
    private PanelPage activePage;
    private int pageIndex = 0;
    private final int stashedIndex = 1;
    private final IntValue stepSkip = new IntValue(1, 1, 16);
    private final IntValue stepDuration = new IntValue(1, 0, 0);
    private final IntValue fixedVelocity = new IntValue(0, 0, 127);
    private final BooleanValueObject overdubMode = new BooleanValueObject(false);
    private final ValueObject<StepInputAdvanceType> advanceMethod =
        new ValueObject<>(StepInputAdvanceType.SAME, StepInputAdvanceType::increment, StepInputAdvanceType::getLabel);
    private final NoteRecurrencePattern recurrencePattern;
    private final NoteRecurrenceValue recurrenceValue;
    
    public static class PanelContent {
        private final int index;
        private String title = "";
        private String value = "";
        private boolean selected;
        private SlRgbState color = SlRgbState.OFF;
        private ControlValue controlValue;
        private final PanelPage parent;
        
        private PanelContent(final int index, final PanelPage parentPage) {
            this.parent = parentPage;
            this.index = index;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getValue() {
            return value;
        }
        
        public void setTitle(final String title) {
            this.title = title;
        }
        
        public void setValue(final String value) {
            this.value = value;
        }
        
        public void setColor(final SlRgbState color) {
            this.color = color;
        }
        
        public void setSelected(final boolean selected) {
            this.selected = selected;
        }
        
        public SlRgbState getColor() {
            return color;
        }
        
        public boolean isSelected() {
            return selected;
        }
        
        private void applyValue(final String displayValue) {
            this.value = displayValue;
            if (parent.isActive()) {
                parent.screen.getPanel(index).setText(1, displayValue);
            }
        }
        
        public void setControlValue(final ControlValue controlValue) {
            this.controlValue = controlValue;
            setValue(controlValue.getDisplayValue().get());
            controlValue.getDisplayValue().addValueObserver(this::applyValue);
        }
        
        public void incrementBy(final int inc, final boolean modifier) {
            if (parent.isActive()) {
                controlValue.incrementBy(inc, modifier);
            }
        }
    }
    
    private static class PanelPage {
        private final PanelContent[] panels = new PanelContent[8];
        private boolean active = false;
        private final ScreenSetup<BoxPanel> screen;
        
        public PanelPage(final ScreenSetup<BoxPanel> screen) {
            for (int i = 0; i < panels.length; i++) {
                panels[i] = new PanelContent(i, this);
            }
            this.screen = screen;
        }
        
        public PanelContent getPanel(final int index) {
            return panels[index];
        }
        
        public boolean isActive() {
            return active;
        }
        
        public void setActive(final boolean active) {
            this.active = active;
        }
        
        public void applyPageToScreen() {
            for (int i = 0; i < 8; i++) {
                final PanelContent pagePane = panels[i];
                if (panels[i] != null) {
                    final BoxPanel screenPanel = screen.getPanel(i);
                    screenPanel.setText(0, pagePane.getTitle());
                    screenPanel.setColor(0, pagePane.getColor());
                    screenPanel.setText(1, pagePane.getValue());
                }
            }
        }
        
        public void updateValues() {
            for (int i = 0; i < panels.length; i++) {
                if (panels[i].controlValue != null) {
                    panels[i].controlValue.update();
                }
            }
        }
    }
    
    public SeqControlPages(final Layer encoderLayer, final ScreenSetup<BoxPanel> screen, final ClipState clipState,
        final List<SlEncoder> encoders, final ValueSet gridResolution, final GlobalStates globalStates,
        final ViewControl viewControl) {
        for (int i = 0; i < pages.length; i++) {
            pages[i] = new PanelPage(screen);
        }
        
        modeColor = globalStates.getModeColor();
        recurrencePattern = new NoteRecurrencePattern(clipState, 3);
        recurrenceValue = new NoteRecurrenceValue(clipState, 5, recurrencePattern);
        
        modeColor.set(globalStates.getClipSeqMode().get() == ClipSeqMode.KEYS ? SlRgbState.CYAN : SlRgbState.DEEP_BLUE);
        globalStates.getClipSeqMode().addValueObserver(
            newMode -> modeColor.set(newMode == ClipSeqMode.KEYS ? SlRgbState.CYAN : SlRgbState.DEEP_BLUE));
        
        final Clip cursorClip = clipState.getNotesCursorClip();
        cursorClip.clipLauncherSlot().name().markInterested();
        // TO DO INFO does note Exists
        final SettableBeatTimeValue loopLenValue = clipState.getNotesCursorClip().getLoopLength();
        positionHandler = clipState.getPositionHandler();
        final BasicStringValue pagePosition = new BasicStringValue(getPositionValue());
        final BasicStringValue loopLength = new BasicStringValue();
        positionHandler.getPagePosition().addValueObserver(v -> pagePosition.set(getPositionValue()));
        loopLenValue.addValueObserver(v -> loopLength.set("%3.1f Bars".formatted(v / 4)));
        loopLenValue.addValueObserver(v -> pagePosition.set(getPositionValue()));
        gridResolution.addValueObserver(v -> pagePosition.set(getPositionValue()));
        final DoubleValueControlValue loopValue =
            new DoubleValueControlValue(loopLenValue, loopLength, globalStates.getShiftState(), 4.0, 0.25, 5);
        
        //assignMainPage(encoderLayer, pages[0], encoders, viewControl, cursorClip);
        //assignSeqParam1(encoderLayer, pages[0], clipState, encoders, gridResolution);
        assignSeqParam1(encoderLayer, pages[0], clipState, encoders, gridResolution);
        assignSeqParam2(encoderLayer, pages[1], clipState, encoders);
        assignClipPage(
            encoderLayer, pages[CLIP_PAGE_INDEX], encoders, viewControl, cursorClip, gridResolution, loopValue);
        assignStepRecParams(encoderLayer, pages[STEP_PAGE_INDEX], encoders, gridResolution, loopValue);
        
        assignInfoDisplay(screen, clipState, globalStates, viewControl, cursorClip, pagePosition, loopLength);
        
        activePage = pages[pageIndex];
        activePage.setActive(true);
        pages[0].applyPageToScreen();
    }
    
    public ObservableColor getModeColor() {
        return modeColor;
    }
    
    private void assignClipPage(final Layer encoderLayer, final PanelPage page, final List<SlEncoder> encoders,
        final ViewControl viewControl, final Clip cursorClip, final ValueSet gridResolution,
        final DoubleValueControlValue loopValue) {
        final Groove groove = viewControl.getGroove();
        final SettableRangedValue accentValue = cursorClip.getAccent();
        final RangeValueControlValue accentControlValue = new RangeValueControlValue(accentValue, 0.02, 0);
        final RangeValueControlValue shuffleAmount = new RangeValueControlValue(groove.getShuffleAmount(), 0.01, 0);
        final RangeValueControlValue shuffleRate = new RangeValueControlValue(groove.getShuffleRate(), 1, 10);
        final RangeValueControlValue accentAmount = new RangeValueControlValue(groove.getAccentAmount(), 0.01, 1);
        final RangeValueControlValue accentRate = new RangeValueControlValue(groove.getAccentRate(), 0.5, 10);
        int index = 0;
        final SlRgbState frameColor = SlRgbState.BITWIG_ORANGE;
        bindValue(encoderLayer, "Grid Res.", gridResolution, page.getPanel(index), encoders.get(index++), frameColor);
        bindValue(encoderLayer, "Length", loopValue, page.getPanel(index), encoders.get(index++), frameColor);
        bindValue(encoderLayer, "Accent", accentControlValue, page.getPanel(index), encoders.get(index++), frameColor);
        bindValue(encoderLayer, "Shuffle Amt", shuffleAmount, page.getPanel(index), encoders.get(index++), frameColor);
        bindValue(encoderLayer, "Shuffle Rt", shuffleRate, page.getPanel(index), encoders.get(index++), frameColor);
        bindValue(encoderLayer, "Accent Amt", accentAmount, page.getPanel(index), encoders.get(index++), frameColor);
        bindValue(encoderLayer, "Accent Rt", accentRate, page.getPanel(index), encoders.get(index), frameColor);
    }
    
    private void assignSeqParam1(final Layer encoderLayer, final PanelPage page, final ClipState clipState,
        final List<SlEncoder> encoders, final ValueSet gridResolution) {
        int index = 0;
        final NoteDoubleControlValue chanceValue =
            new NoteDoubleControlValue(clipState, NoteStep::chance, NoteStep::setChance);
        final NoteDoubleControlValue velocityValue =
            new NoteDoubleControlValue(clipState, NoteStep::velocity, NoteStep::setVelocity,
                this::convertVelocityRange);
        final NoteIntControlValue repeats =
            new NoteIntControlValue(clipState, NoteStep::repeatCount, NoteStep::setRepeatCount, 0, 128);
        final NoteDurationControlValue durationValue = new NoteDurationControlValue(clipState, gridResolution);
        final NoteDoubleControlValue repeatCurveValue =
            new NoteDoubleControlValue(clipState, NoteStep::repeatCurve, NoteStep::setRepeatCurve, -1);
        final NoteDoubleControlValue repeatVelCurveValue =
            new NoteDoubleControlValue(clipState, NoteStep::repeatVelocityCurve, NoteStep::setRepeatVelocityCurve, -1);
        final NoteDoubleControlValue repeatVelEndValue =
            new NoteDoubleControlValue(clipState, NoteStep::repeatVelocityEnd, NoteStep::setRepeatVelocityEnd, -1);
        final SlRgbState frameColor = SlRgbState.OFF;
        
        bindValue(encoderLayer, "Velocity", velocityValue, page.getPanel(index), encoders.get(index++), frameColor);
        bindValue(encoderLayer, "Duration", durationValue, page.getPanel(index), encoders.get(index++), frameColor);
        bindValueMod(encoderLayer, "Dur. Fine", durationValue, page.getPanel(index), encoders.get(index++), frameColor);
        bindValue(encoderLayer, "Chance", chanceValue, page.getPanel(index), encoders.get(index++), frameColor);
        bindValue(encoderLayer, "Repeats", repeats, page.getPanel(index), encoders.get(index++), frameColor);
        bindValue(encoderLayer, "Rep.Curve", repeatCurveValue, page.getPanel(index), encoders.get(index++), frameColor);
        bindValue(encoderLayer, "R.Vel.Curve", repeatVelCurveValue, page.getPanel(index), encoders.get(index++),
            frameColor);
        bindValue(encoderLayer, "R.Vel.End", repeatVelEndValue, page.getPanel(index), encoders.get(index++),
            frameColor);
    }
    
    private void assignSeqParam2(final Layer encoderLayer, final PanelPage page, final ClipState clipState,
        final List<SlEncoder> encoders) {
        int index = 0;
        final NoteDoubleControlValue timbreValue =
            new NoteDoubleControlValue(clipState, NoteStep::timbre, NoteStep::setTimbre, -1);
        final NoteDoubleControlValue panValue =
            new NoteDoubleControlValue(clipState, NoteStep::pan, NoteStep::setPan, -1);
        final NoteDoubleControlValue pressureValue =
            new NoteDoubleControlValue(clipState, NoteStep::pressure, NoteStep::setPressure);
        //final NoteGainValueControl gainValue = new NoteGainValueControl(clipState);
        final NoteDoubleControlValue velocitySpreadValue =
            new NoteDoubleControlValue(clipState, NoteStep::velocitySpread, NoteStep::setVelocitySpread);
        final NoteOccurrenceValue occurrenceValue = new NoteOccurrenceValue(clipState, 5);
        final SlRgbState frameColor = SlRgbState.OFF;
        
        bindValue(encoderLayer, "Occurrence", occurrenceValue, page.getPanel(index), encoders.get(index++), frameColor);
        bindValue(encoderLayer, "Recurrence", recurrenceValue, page.getPanel(index), encoders.get(index++), frameColor);
        bindValue(encoderLayer, "Rec.Patt", recurrencePattern, page.getPanel(index), encoders.get(index++), frameColor);
        bindValue(encoderLayer, "Timbre", timbreValue, page.getPanel(index), encoders.get(index++), frameColor);
        bindValue(encoderLayer, "Aftertch", pressureValue, page.getPanel(index), encoders.get(index++), frameColor);
        bindValue(encoderLayer, "Vel.Spread", velocitySpreadValue, page.getPanel(index), encoders.get(index++),
            frameColor);
        bindValue(encoderLayer, "Pan", panValue, page.getPanel(index), encoders.get(index), frameColor);
        //bindValue(encoderLayer, "Gain", gainValue, pages[2].getPanel(index), encoders.get(index++));
    }
    
    private void assignStepRecParams(final Layer encoderLayer, final PanelPage page, final List<SlEncoder> encoders,
        final ValueSet gridResolution, final DoubleValueControlValue loopValue) {
        int index = 0;
        final BooleanControlValue stepOverdubControlValue = new BooleanControlValue(overdubMode, "Overdub", "Replace");
        final ObjectControlValue<StepInputAdvanceType> advanceTypeValue = new ObjectControlValue<>(advanceMethod, 10);
        final IntegerControlValue stepSkipControlValue = new IntegerControlValue(stepSkip, 1, 16, 5);
        final IntegerControlValue fixedVelocityValue =
            new IntegerControlValue(fixedVelocity, 0, 127, 2, val -> val == 0 ? "Input" : Integer.toString(val));
        final IntegerControlValue stepDurationValue = new IntegerControlValue(stepDuration, -5, 8, 5, this::toDuration);
        final SlRgbState frameColor = SlRgbState.RED;
        
        bindValue(encoderLayer, "Grid Res.", gridResolution, page.getPanel(index), encoders.get(index++), frameColor);
        bindValue(encoderLayer, "Length", loopValue, page.getPanel(index), encoders.get(index++), frameColor);
        bindValue(encoderLayer, "Step Rec.", stepOverdubControlValue, page.getPanel(index), encoders.get(index++),
            frameColor);
        bindValue(encoderLayer, "Step.Adv", advanceTypeValue, page.getPanel(index), encoders.get(index++), frameColor);
        bindValue(
            encoderLayer, "Step.Skip", stepSkipControlValue, page.getPanel(index), encoders.get(index++), frameColor);
        bindValue(encoderLayer, "Step.Len", stepDurationValue, page.getPanel(index), encoders.get(index++), frameColor);
        bindValue(encoderLayer, "Fixed Vel", fixedVelocityValue, page.getPanel(index), encoders.get(index), frameColor);
    }
    
    private void assignInfoDisplay(final ScreenSetup<BoxPanel> screen, final ClipState clipState,
        final GlobalStates globalStates, final ViewControl viewControl, final Clip cursorClip,
        final BasicStringValue pagePosition, final BasicStringValue loopLength) {
        int index = 0;
        bindInfo(screen.getPanel(index), "Position", pagePosition);
        bindModeColor(screen.getPanel(index++));
        
        //bindInfo(screen.getPanel(index), "Length", loopLength);
        //bindColor(screen.getPanel(index++), SlRgbState.WHITE);
        
        bindSeqInfo(screen.getPanel(index++), "SEQ Mode", globalStates.getClipSeqMode(), globalStates.getClipSeqMode());
        
        bindSeqInfo(
            screen.getPanel(index++), "Note", globalStates.getPadSelectionInfo(), globalStates.getClipSeqMode());
        
        bindInfo(screen.getPanel(7), viewControl.getCursorTrack().name(), cursorClip.clipLauncherSlot().name());
        bindColor(screen.getPanel(7), clipState.getClipColor());
    }
    
    private String getPositionValue() {
        return "%d/%d".formatted(positionHandler.getPagePosition().get() + 1, positionHandler.getPages());
    }
    
    private String convertVelocityRange(final double min, final double max) {
        if (min == max) {
            return "%d".formatted(Math.round(min * 127));
        }
        return "%d-%d".formatted(Math.round(min * 127), Math.round(max * 127));
    }
    
    private String convertGain(final double min, final double max) {
        if (min == max) {
            return "%f".formatted(min);
        }
        return "%1.2f-%1.2f".formatted(min, max);
    }
    
    public boolean canScrollDown() {
        return pageIndex == 0;
    }
    
    public boolean canScrollUp() {
        return pageIndex == 1;
    }
    
    public void scrollBy(final int direction) {
        if (pageIndex == 0 && direction > 0) {
            setToPageIndex(1);
        } else if (pageIndex == 1 && direction < 0) {
            setToPageIndex(0);
        }
    }
    
    
    public BooleanValueObject getOverdubMode() {
        return overdubMode;
    }
    
    public IntValue getStepSkip() {
        return stepSkip;
    }
    
    public ValueObject<StepInputAdvanceType> getAdvanceMethod() {
        return advanceMethod;
    }
    
    public IntValue getFixedVelocity() {
        return fixedVelocity;
    }
    
    public NoteRecurrenceValue getRecurrenceValue() {
        return recurrenceValue;
    }
    
    public double getStepDuration() {
        return switch (stepDuration.get()) {
            case 0 -> stepSkip.get();
            case -1 -> 0.9;
            case -2 -> 0.75;
            case -3 -> 0.50;
            case -4 -> 0.25;
            case -5 -> 0.10;
            default -> stepDuration.get();
        };
    }
    
    public String toDuration(final int value) {
        return switch (value) {
            case -1 -> "90% Step";
            case -2 -> "75% Step";
            case -3 -> "50% Step";
            case -4 -> "25% Step";
            case -5 -> "10% Step";
            case 0 -> "Skip Len";
            default -> "%d Steps".formatted(value);
        };
    }
    
    public NoteRecurrencePattern getRecurrencePattern() {
        return recurrencePattern;
    }
    
    private void bindSeqInfo(final BoxPanel boxPanel, final String title, final IObservableValue<?> value,
        final IObservableValue<ClipSeqMode> mode) {
        boxPanel.setText(2, title);
        boxPanel.setText(3, value.getDisplayString());
        value.addDisplayObserver(v -> boxPanel.setText(3, v));
        bindModeColor(boxPanel);
    }
    
    private void bindSeqInfo(final BoxPanel boxPanel, final String title, final StringValue value,
        final IObservableValue<ClipSeqMode> mode) {
        boxPanel.setText(2, mode.get() == ClipSeqMode.DRUM ? title : "");
        boxPanel.setText(3, mode.get() == ClipSeqMode.DRUM ? value.get() : "");
        mode.addValueObserver(newValue -> {
            boxPanel.setText(2, newValue == ClipSeqMode.DRUM ? title : "");
            boxPanel.setText(3, mode.get() == ClipSeqMode.DRUM ? value.get() : "");
        });
        value.addValueObserver(v -> boxPanel.setText(3, mode.get() == ClipSeqMode.DRUM ? v : ""));
        boxPanel.setColor(1, mode.get() == ClipSeqMode.DRUM ? SlRgbState.DEEP_BLUE : SlRgbState.OFF);
        mode.addValueObserver(m -> boxPanel.setColor(1, m == ClipSeqMode.DRUM ? SlRgbState.DEEP_BLUE : SlRgbState.OFF));
        boxPanel.setCenterSelected(true);
    }
    
    private void bindInfo(final BoxPanel boxPanel, final StringValue title, final StringValue value) {
        title.addValueObserver(v -> boxPanel.setText(2, v));
        value.addValueObserver(v -> boxPanel.setText(3, v));
        boxPanel.setText(2, title.get()); // title.get()
        boxPanel.setText(3, value.get());
    }
    
    private void bindInfo(final BoxPanel boxPanel, final String title, final StringValue value) {
        bindInfo(boxPanel, new BasicStringValue(title), value);
    }
    
    private static void bindColor(final BoxPanel boxPanel, final IObservableValue<SlRgbState> color) {
        boxPanel.setColor(1, color.get());
        color.addValueObserver(colorState -> boxPanel.setColor(1, colorState));
        boxPanel.setCenterSelected(true);
    }
    
    private static void bindColor(final BoxPanel boxPanel, final SlRgbState color) {
        boxPanel.setColor(1, color);
        boxPanel.setCenterSelected(true);
    }
    
    private void bindModeColor(final BoxPanel boxPanel) {
        boxPanel.setColor(1, modeColor.get());
        modeColor.addValueObserver(color -> boxPanel.setColor(1, color));
        boxPanel.setCenterSelected(true);
    }
    
    private void bindValue(final Layer layer, final String title, final ControlValue controlValue,
        final PanelContent pagePane, final SlEncoder encoder, final SlRgbState frameColor) {
        pagePane.setTitle(title);
        pagePane.setColor(frameColor);
        pagePane.setControlValue(controlValue);
        encoder.bindIncrementAction(layer, inc -> pagePane.incrementBy(inc, false));
    }
    
    private void bindValueMod(final Layer layer, final String title, final ControlValue controlValue,
        final PanelContent pagePane, final SlEncoder encoder, final SlRgbState frameColor) {
        pagePane.setTitle(title);
        pagePane.setColor(frameColor);
        pagePane.setControlValue(controlValue);
        encoder.bindIncrementAction(layer, inc -> pagePane.incrementBy(inc, true));
    }
    
    public void setToStepInput() {
        setToPageIndex(STEP_PAGE_INDEX);
    }
    
    public void setToMain() {
        setToPageIndex(0);
    }
    
    public void setToClip() {
        setToPageIndex(CLIP_PAGE_INDEX);
    }
    
    public boolean onParamPage() {
        return pageIndex == 0 || pageIndex == 1;
    }
    
    public void setToPageIndex(final int newIndex) {
        if (newIndex != pageIndex) {
            this.pageIndex = newIndex;
            activePage.setActive(false);
            activePage = pages[pageIndex];
            activePage.updateValues();
            activePage.applyPageToScreen();
            activePage.setActive(true);
        }
    }
    
    
    public void updateNotes() {
        if (onParamPage()) {
            activePage.updateValues();
        }
    }
}
