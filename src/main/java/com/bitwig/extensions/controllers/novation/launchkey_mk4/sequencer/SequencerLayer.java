package com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer;

import java.util.Arrays;

import com.bitwig.extension.controller.api.BeatTimeFormatter;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.CcAssignments;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.GlobalStates;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.LaunchkeyHwElements;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.MidiProcessor;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.ViewControl;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.RgbButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.DisplayControl;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.values.IntValue;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.values.NoteHandler;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.values.ValueSet;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.time.TimedDelayEvent;

@Component
public class SequencerLayer extends Layer implements NoteHandler {
    private static final int STEPS = 16;
    private final int copyIndex = -1;
    private final long[] stepDownTimes = new long[STEPS];
    private final boolean[] justEntered = new boolean[STEPS];
    private final TimedDelayEvent holdDelayEvent = null;
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
    private ClipSeqMode seqMode = ClipSeqMode.KEYS;
    
    public SequencerLayer(final Layers layers, final ViewControl viewControl, final LaunchkeyHwElements hwElements,
        final GlobalStates globalStates, final ControllerHost host, final DisplayControl displayControl,
        final MidiProcessor midiProcessor) {
        super(layers, "SEQUENCER");
        Arrays.fill(stepDownTimes, -1);
        midiProcessor.addNoteHandler(this);
        this.midiProcessor = midiProcessor;
        this.formatter = host.createBeatTimeFormatter(":", 2, 1, 1, 0);
        this.globalStates = globalStates;
        gridValue = new ValueSet().add("1/2", 2.0) //
            .add("1/4", 1.0) //
            .add("1/8", 0.5) //
            .add("1/16", 0.25) //
            .add("1/32", 0.125);
        gridValue.setSelectedIndex(3);
        singlePadBank = viewControl.getPrimaryDevice().createDrumPadBank(1);
        focusDrumPad = singlePadBank.getItemAt(0);
        initPadKeyUpdate(viewControl, globalStates);
        
        final RgbButton[] buttons = hwElements.getSessionButtons();
        for (int i = 0; i < STEPS; i++) {
            final int index = i;
            final RgbButton button = buttons[i];
        }
        initNavigation(hwElements);
    }
    
    private void initPadKeyUpdate(final ViewControl viewControl, final GlobalStates globalStates) {
        viewControl.getPrimaryDevice().hasDrumPads().addValueObserver(hasDrumPads -> {
            this.hasDrumPads = hasDrumPads;
            seqMode = hasDrumPads ? ClipSeqMode.DRUM : ClipSeqMode.KEYS;
            updatePadName();
        });
        monoKeyNoteFocus.addValueObserver(focusKey -> updateFocusKey(focusKey));
        singlePadBank.scrollPosition().addValueObserver(pos -> updatePadName());
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
        
    }
    
    private void updateFocusKey(final int focusKey) {
        if (hasDrumPads) {
            singlePadBank.scrollPosition().set(focusKey);
            //globalStates.getPadSelectionInfo().set(focusDrumPad.name().get());
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
    }
    
    @Override
    public void handleNoteAction(final int note, final int velocity) {
        
    }
}
