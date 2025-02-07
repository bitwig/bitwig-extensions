package com.bitwig.extensions.controllers.novation.slmk3;

import com.bitwig.extensions.controllers.novation.slmk3.display.ButtonMode;
import com.bitwig.extensions.controllers.novation.slmk3.display.SequencerButtonSubMode;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;
import com.bitwig.extensions.controllers.novation.slmk3.layer.GridMode;
import com.bitwig.extensions.controllers.novation.slmk3.seqcommons.ClipSeqMode;
import com.bitwig.extensions.controllers.novation.slmk3.value.BufferedObservableValue;
import com.bitwig.extensions.controllers.novation.slmk3.value.ClipFocus;
import com.bitwig.extensions.controllers.novation.slmk3.value.IObservableValue;
import com.bitwig.extensions.controllers.novation.slmk3.value.ObservableColor;
import com.bitwig.extensions.controllers.novation.slmk3.value.ObservableValue;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.BooleanValueObject;

@Component
public class GlobalStates {
    private final BooleanValueObject duplicateState = new BooleanValueObject();
    private final BooleanValueObject clearState = new BooleanValueObject();
    private final BooleanValueObject shiftState = new BooleanValueObject();
    private final BooleanValueObject modifierActive = new BooleanValueObject();
    private final BooleanValueObject gridModeHeld = new BooleanValueObject();
    private final BooleanValueObject hasDrumPads = new BooleanValueObject();
    
    private final BufferedObservableValue<GridMode> baseMode = new BufferedObservableValue<>(GridMode.LAUNCH);
    private final IObservableValue<ClipFocus> clipFocus =
        new ObservableValue<>(ClipFocus.LAUNCHER, ClipFocus::getDisplayName);
    private final IObservableValue<ClipSeqMode> clipSeqMode =
        new ObservableValue<>(ClipSeqMode.KEYS, ClipSeqMode::getDisplayName);
    
    private final BasicStringValue padSelectionInfo = new BasicStringValue("SomePad");
    private final ObservableValue<ButtonMode> buttonMode = new ObservableValue<>(ButtonMode.TRACK);
    private final ObservableValue<SequencerButtonSubMode> sequencerSubMode =
        new ObservableValue<>(SequencerButtonSubMode.MODE_1);
    private final ObservableColor trackColor = new ObservableColor();
    private final ObservableColor modeColor = new ObservableColor(SlRgbState.DEEP_BLUE);
    
    public GlobalStates() {
        shiftState.addValueObserver(active -> updateModifierActive());
        clearState.addValueObserver(active -> updateModifierActive());
        duplicateState.addValueObserver(active -> updateModifierActive());
    }
    
    public BooleanValueObject getGridModeHeld() {
        return gridModeHeld;
    }
    
    private void updateModifierActive() {
        this.modifierActive.set(shiftState.get() || clearState.get() || duplicateState.get());
    }
    
    public ObservableColor getModeColor() {
        return modeColor;
    }
    
    public ObservableColor getTrackColor() {
        return trackColor;
    }
    
    public ObservableValue<ButtonMode> getButtonMode() {
        return buttonMode;
    }
    
    public ObservableValue<SequencerButtonSubMode> getSequencerSubMode() {
        return sequencerSubMode;
    }
    
    public BasicStringValue getPadSelectionInfo() {
        return padSelectionInfo;
    }
    
    public BooleanValueObject getHasDrumPads() {
        return hasDrumPads;
    }
    
    public BooleanValueObject getModifierActive() {
        return modifierActive;
    }
    
    public BooleanValueObject getDuplicateState() {
        return duplicateState;
    }
    
    public BooleanValueObject getClearState() {
        return clearState;
    }
    
    public BooleanValueObject getShiftState() {
        return shiftState;
    }
    
    public BufferedObservableValue<GridMode> getBaseMode() {
        return baseMode;
    }
    
    public IObservableValue<ClipSeqMode> getClipSeqMode() {
        return clipSeqMode;
    }
}
