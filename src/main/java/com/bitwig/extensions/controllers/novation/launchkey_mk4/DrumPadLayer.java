package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extensions.controllers.novation.commonsmk3.ColorLookup;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.RgbButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.DisplayControl;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.StringUtil;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

@Component
public class DrumPadLayer extends Layer {
    private final PinnableCursorDevice primaryDevice;
    private final DrumPadBank padBank;
    private final CursorTrack cursorTrack;
    private final NoteInput noteInput;
    
    private static final int[] IDX_TO_PAD_MAPPING = {8, 9, 10, 11, 0, 1, 2, 3, 12, 13, 14, 15, 4, 5, 6, 7};
    private static final int[] KEY_MAPPING = {0, 1, 2, 3, 8, 9, 10, 11, 4, 5, 6, 7, 12, 13, 14, 15};
    private int padsNoteOffset = 32;
    private int keyNoteOffset = 36;
    private boolean focusOnDrums = true;
    private final int[] hangingNotes = new int[16];
    private final Integer[] noteTable = new Integer[128];
    private final boolean[] isPlaying = new boolean[128];
    private final int[] padsToNotes = new int[16];
    private final Set<Integer> padNotes = new HashSet<>();
    
    @Inject
    private DisplayControl display;
    private final GlobalStates globalStates;
    
    public DrumPadLayer(final Layers layers, final LaunchkeyHwElements hwElements, final MidiProcessor midiProcessor,
        final ViewControl viewControl, final GlobalStates globalStates) {
        super(layers, "DRUM_LAYER");
        Arrays.fill(noteTable, -1);
        Arrays.fill(hangingNotes, -1);
        Arrays.fill(padsToNotes, -1);
        this.globalStates = globalStates;
        cursorTrack = viewControl.getCursorTrack();
        cursorTrack.playingNotes().addValueObserver(this::handleNotes);
        noteInput = midiProcessor.getPadNoteInput();
        noteInput.setKeyTranslationTable(noteTable);
        primaryDevice = viewControl.getPrimaryDevice();
        padBank = primaryDevice.createDrumPadBank(16);
        padBank.scrollPosition().addValueObserver(index -> {
            padsNoteOffset = index;
            if (isActive() && focusOnDrums) {
                applyNotes(padsNoteOffset);
            }
        });
        padBank.exists().markInterested();
        padBank.scrollPosition().markInterested();
        padBank.scrollPosition().set(padsNoteOffset);
        primaryDevice.hasDrumPads().addValueObserver(hasDrumPads -> {
            focusOnDrums = hasDrumPads;
            applyNotes(focusOnDrums ? padsNoteOffset : keyNoteOffset);
        });
        
        final RgbButton[] drumButtons = hwElements.getDrumButtons();
        for (int i = 0; i < 16; i++) {
            final int index = i;
            final RgbButton button = drumButtons[IDX_TO_PAD_MAPPING[i]];
            final DrumPad pad = padBank.getItemAt(i);
            final PadSlot padSlot = new PadSlot(index, pad);
            globalStates.setPadSlot(index, padSlot);
            padNotes.add(button.getMidiId());
            pad.exists().markInterested();
            pad.name().markInterested();
            pad.color().addValueObserver((r, g, b) -> padSlot.setColor(RgbState.of(ColorLookup.toColor(r, g, b))));
            pad.addIsSelectedInEditorObserver(selected -> padSlot.setSelected(selected));
            button.bindLight(this, () -> getColor(index, pad));
            button.bindPressed(this, () -> handlePadSelection(index, pad));
        }
        
        final RgbButton navUpButton = hwElements.getButton(CcAssignments.NAV_UP);
        final RgbButton navDownButton = hwElements.getButton(CcAssignments.NAV_DOWN);
        navUpButton.bindRepeatHold(this, () -> navigate(1), 500, 100);
        navUpButton.bindLightPressed(this, this::canScrollUp);
        
        navDownButton.bindRepeatHold(this, () -> navigate(-1), 500, 100);
        navDownButton.bindLightPressed(this, this::canScrollDown);
        
    }
    
    private boolean canScrollUp() {
        if (focusOnDrums) {
            return padsNoteOffset + 4 < 128;
        }
        return keyNoteOffset + 4 < 128;
    }
    
    private boolean canScrollDown() {
        if (focusOnDrums) {
            return padsNoteOffset - 4 >= 0;
        }
        return keyNoteOffset - 4 >= 0;
    }
    
    private void navigate(final int dir) {
        if (focusOnDrums) {
            final int pos = padsNoteOffset + dir * 4;
            if (pos >= 0 && (pos + 16) < 128) {
                padBank.scrollBy(dir * 4);
                display.show2Line(
                    "Drum Pads", "%s - %s".formatted(StringUtil.toNoteValue(pos), StringUtil.toNoteValue(pos + 16)));
            }
        } else {
            final int pos = keyNoteOffset + dir * 12;
            if (pos >= 0 && (pos + 16) < 128) {
                keyNoteOffset = pos;
                applyNotes(keyNoteOffset);
                display.show2Line("Keyboard Pads",
                    "%s - %s".formatted(StringUtil.toNoteValue(pos), StringUtil.toNoteValue(pos + 16)));
            }
        }
    }
    
    private RgbState getColor(final int index, final DrumPad pad) {
        final int noteValue = (focusOnDrums ? padsNoteOffset + index : keyNoteOffset + KEY_MAPPING[index]);
        if (!focusOnDrums) {
            if (noteValue < 0 || noteValue > 127) {
                return RgbState.OFF;
            }
            if (noteValue % 12 == 0) {
                return isPlaying[noteValue] ? RgbState.WHITE : RgbState.DIM_WHITE;
            }
            return globalStates.getTrackColor(isPlaying[noteValue]);
        } else if (pad.exists().get()) {
            return globalStates.getPadColor(index, isPlaying[noteValue]);
        }
        return RgbState.OFF;
    }
    
    private void handlePadSelection(final int index, final DrumPad pad) {
        LaunchkeyMk4Extension.println(" HANDLE IN => %d", index);
    }
    
    private void handleNotes(final PlayingNote[] playingNotes) {
        if (!isActive()) {
            return;
        }
        Arrays.fill(isPlaying, false);
        for (final PlayingNote playingNote : playingNotes) {
            isPlaying[playingNote.pitch()] = true;
        }
    }
    
    public void applyNotes(final int noteOffset) {
        if (isActive()) {
            Arrays.fill(noteTable, -1);
            if (focusOnDrums) {
                for (int note = 0; note < 16; note++) {
                    final int value = noteOffset + note;
                    noteTable[0x24 + note] = value < 128 ? value : -1;
                    padsToNotes[note] = noteOffset + note;
                }
            } else {
                for (int note = 0; note < 16; note++) {
                    final int value = noteOffset + KEY_MAPPING[note];
                    noteTable[0x24 + note] = value < 128 ? value : -1;
                    padsToNotes[note] = noteOffset + note;
                }
            }
            noteInput.setKeyTranslationTable(noteTable);
        }
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
        applyNotes(focusOnDrums ? padsNoteOffset : keyNoteOffset);
    }
}
