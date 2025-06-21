package com.bitwig.extensions.controllers.novation.slmk3.layer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extensions.controllers.novation.slmk3.CcAssignment;
import com.bitwig.extensions.controllers.novation.slmk3.GlobalStates;
import com.bitwig.extensions.controllers.novation.slmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.slmk3.SlMk3HardwareElements;
import com.bitwig.extensions.controllers.novation.slmk3.StringUtil;
import com.bitwig.extensions.controllers.novation.slmk3.ViewControl;
import com.bitwig.extensions.controllers.novation.slmk3.control.RgbButton;
import com.bitwig.extensions.controllers.novation.slmk3.display.ScreenHandler;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;
import com.bitwig.extensions.controllers.novation.slmk3.sequencer.SequencerLayer;
import com.bitwig.extensions.controllers.novation.slmk3.value.Scale;
import com.bitwig.extensions.controllers.novation.slmk3.value.ScaleInterface;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.values.BooleanValueObject;

@Component
public class DrumPadLayer {
    
    private final ScreenHandler screenHandler;
    private final ViewControl viewControl;
    public static final int NOTE_ON = 0x90;
    private static final int[] IDX_TO_PAD_MAPPING = {8, 9, 10, 11, 0, 1, 2, 3, 12, 13, 14, 15, 4, 5, 6, 7};
    private static final int[] KEY_MAPPING = {0, 1, 2, 3, 8, 9, 10, 11, 4, 5, 6, 7, 12, 13, 14, 15};
    private static final int[] KEY_TO_PADS =
        {112, 113, 114, 115, 96, 97, 98, 99, 116, 117, 118, 119, 100, 101, 102, 103};
    
    private final PinnableCursorDevice primaryDevice;
    private final DrumPadBank padBank;
    private final NoteInput noteInput;
    private final Set<Integer> padNotes = new HashSet<>();
    private int padsNoteOffset = 32;
    private int keyNoteOffset = 36;
    private int baseNote = 0;
    private final int[] hangingNotes = new int[16];
    private final Integer[] noteTable = new Integer[128];
    private final boolean[] isPlaying = new boolean[128];
    private final int[] padsToNotes = new int[16];
    private final GlobalStates globalStates;
    private final Integer[] keyToPadLookup = new Integer[128];
    
    private final PadSlot[] padSlots = new PadSlot[16];
    private SlRgbState trackColor = SlRgbState.OFF;
    private SlRgbState trackColorOff = SlRgbState.OFF;
    
    private boolean focusOnDrums = true;
    private final BooleanValueObject selectModeActive = new BooleanValueObject(false);
    private final BooleanValueObject scaleSelectModeActive = new BooleanValueObject(false);
    private final BooleanValueObject noteSelectModeActive = new BooleanValueObject(false);
    
    private final Layer padLayer;
    private final CursorTrack cursorTrack;
    private int scaleIndex = 0;
    private final List<ScaleInterface> scaleSelection =
        List.of(Scale.CHROMATIC, Scale.MAJOR, Scale.MINOR, Scale.PENTATONIC, Scale.PENTATONIC_MINOR, Scale.DORIAN,
            Scale.DORIAN, Scale.MIXOLYDIAN, Scale.LYDIAN, Scale.LOCRIAN, Scale.WHOLE_TONE);
    
    @Inject
    private ControllerHost host;
    
    @Inject
    private SequencerLayer sequencerLayer;
    
    public DrumPadLayer(final SlMk3HardwareElements hwElements, final ViewControl viewControl,
        final MidiProcessor midiProcessor, final LayerRepo layerRepo, final GlobalStates globalStates,
        final ScreenHandler screenHandler) {
        Arrays.fill(noteTable, -1);
        Arrays.fill(hangingNotes, -1);
        Arrays.fill(padsToNotes, -1);
        Arrays.fill(keyToPadLookup, -1);
        for (int i = 0; i < KEY_TO_PADS.length; i++) {
            keyToPadLookup[KEY_TO_PADS[i]] = i;
        }
        padLayer = layerRepo.getPadDrumLayer();
        this.globalStates = globalStates;
        this.screenHandler = screenHandler;
        this.viewControl = viewControl;
        globalStates.getBaseMode().addValueObserver(this::handleGridMode);
        globalStates.getModifierActive().addValueObserver(this::handleModifierActive);
        midiProcessor.addPadNoteHandler(this::playNote);
        
        cursorTrack = viewControl.getCursorTrack();
        cursorTrack.color().addValueObserver((r, g, b) -> {
            trackColor = SlRgbState.get(r, g, b);
            trackColorOff = trackColor.reduced(40);
        });
        cursorTrack.playingNotes().addValueObserver(this::handleNotes);
        noteInput = midiProcessor.getDrumNoteInput();
        noteInput.setKeyTranslationTable(noteTable);
        primaryDevice = viewControl.getPrimaryDevice();
        padBank = viewControl.getPadBank();
        padBank.scrollPosition().addValueObserver(index -> {
            padsNoteOffset = index;
            if (padLayer.isActive() && focusOnDrums) {
                applyNotes(padsNoteOffset);
            }
        });
        padBank.scrollPosition().set(padsNoteOffset);
        primaryDevice.hasDrumPads().addValueObserver(hasDrumPads -> {
            focusOnDrums = hasDrumPads;
            applyNotes(focusOnDrums ? padsNoteOffset : keyNoteOffset);
        });
        final List<RgbButton> padButtons = hwElements.getPadButtons();
        for (int i = 0; i < padButtons.size(); i++) {
            final int index = i;
            final RgbButton button = padButtons.get(IDX_TO_PAD_MAPPING[i]);
            final DrumPad pad = padBank.getItemAt(i);
            padSlots[index] = new PadSlot(index, pad);
            padNotes.add(button.getMidiId());
            pad.exists().markInterested();
            pad.name().markInterested();
            pad.color().addValueObserver((r, g, b) -> {
                final SlRgbState color = SlRgbState.get(r, g, b);
                padSlots[index].setColor(color);
            });
            pad.addIsSelectedInEditorObserver(selected -> {
                padSlots[index].setSelected(selected);
            });
            button.bindLight(padLayer, () -> getColor(index, pad));
            button.bindPressed(padLayer, () -> handlePadSelection(index, pad));
        }
        final RgbButton padUpButton = hwElements.getButton(CcAssignment.PADS_UP);
        final RgbButton padDownButton = hwElements.getButton(CcAssignment.PADS_DOWN);
        
        padUpButton.bindLight(padLayer, () -> canScrollUp() ? SlRgbState.GREEN : SlRgbState.GREEN_DIM);
        padDownButton.bindLight(padLayer, () -> canScrollDown() ? SlRgbState.GREEN : SlRgbState.GREEN_DIM);
        padUpButton.bindRepeatHold(padLayer, () -> scrollPadBank(1), 200, 200);
        padDownButton.bindRepeatHold(padLayer, () -> scrollPadBank(-1), 200, 200);
        
        globalStates.getClearState().addValueObserver(clearHeld -> handleClearDown(clearHeld));
        
        final RgbButton scene1Button = hwElements.getButton(CcAssignment.SCENE_LAUNCH_1);
        scene1Button.bindLight(padLayer, this::scene1LightState);
        scene1Button.bindIsPressed(padLayer, this::handleScene1Button);
        final RgbButton scene2Button = hwElements.getButton(CcAssignment.SCENE_LAUNCH_2);
        
        scene2Button.bindLight(padLayer, this::scene2LightState);
        scene2Button.bindIsPressed(padLayer, this::handleScene2Button);
    }
    
    private Optional<PadSlot> getSelected() {
        for (int i = 0; i < padSlots.length; i++) {
            if (padSlots[i].isSelected()) {
                return Optional.of(padSlots[i]);
            }
        }
        return Optional.empty();
    }
    
    private void handleScene1Button(final boolean isPressed) {
        if (focusOnDrums) {
            if (isPressed) {
                selectModeActive.toggle();
                screenHandler.notifyMessage("Select Pads", selectModeActive.get() ? "active" : "inactive");
            }
        } else {
            scaleSelectModeActive.set(isPressed);
            if (isPressed) {
                screenHandler.notifyMessage("Select Scale", scaleSelection.get(scaleIndex).getName());
            }
        }
        
    }
    
    private void handleScene2Button(final boolean isPressed) {
        if (!focusOnDrums) {
            noteSelectModeActive.set(isPressed);
            if (isPressed) {
                screenHandler.notifyMessage("Scale Base Note", StringUtil.toSingleNoteValue(baseNote).trim());
            }
        }
    }
    
    private SlRgbState scene1LightState() {
        if (focusOnDrums) {
            return selectModeActive.get() ? SlRgbState.GREEN : SlRgbState.GREEN_DIM;
        } else {
            return scaleSelectModeActive.get() ? SlRgbState.GREEN : SlRgbState.GREEN_DIM;
        }
    }
    
    private SlRgbState scene2LightState() {
        if (focusOnDrums) {
            return SlRgbState.OFF;
        } else {
            return noteSelectModeActive.get() ? SlRgbState.GREEN : SlRgbState.GREEN_DIM;
        }
    }
    
    private void handleClearDown(final boolean clearHeld) {
        if (globalStates.getBaseMode().get() != GridMode.PAD) {
            return;
        }
        if (clearHeld) {
            resetNotes();
        } else {
            applyNotes(focusOnDrums ? padsNoteOffset : keyNoteOffset);
        }
    }
    
    private void handlePadSelection(final int index, final DrumPad pad) {
        // Deleting and Duplicating pads via API doesn't respond, so could not implement
        if (globalStates.getClearState().get()) {
            final int noteValue = (focusOnDrums ? padsNoteOffset + index : indexToNoteValue(index));
            sequencerLayer.removeNotes(noteValue);
            final String message = focusOnDrums ? "%s".formatted(pad.name().get()) : StringUtil.toNoteValue(noteValue);
            host.scheduleTask(() -> screenHandler.notifyMessage("Clear Notes", message), 200);
        } else if (globalStates.getShiftState().get()) {
            final int noteValue = (focusOnDrums ? padsNoteOffset + index : indexToNoteValue(index));
            pad.selectInEditor();
            if (focusOnDrums) {
                viewControl.selectDrumDevice(padsToNotes[index]);
            }
            final String message = focusOnDrums ? "%s".formatted(pad.name().get()) : StringUtil.toNoteValue(noteValue);
            host.scheduleTask(() -> screenHandler.notifyMessage("Select Pad", message), 200);
        }
    }
    
    private void handleModifierActive(final boolean active) {
        if (padLayer.isActive()) {
            if (active) {
                resetNotes();
            } else {
                applyNotes(focusOnDrums ? padsNoteOffset : keyNoteOffset);
            }
        }
    }
    
    private void handleGridMode(final GridMode mode) {
        if (mode == GridMode.PAD) {
            padLayer.setIsActive(true);
            applyNotes(focusOnDrums ? padsNoteOffset : keyNoteOffset);
        } else {
            padLayer.setIsActive(false);
            resetNotes();
        }
    }
    
    private SlRgbState getColor(final int index, final DrumPad pad) {
        final int noteValue = focusOnDrums ? padsNoteOffset + index : indexToNoteValue(index);
        if (!focusOnDrums) {
            if (noteValue < 0 || noteValue > 127) {
                return SlRgbState.OFF;
            }
            if (noteValue % 12 == (scaleIndex == 0 ? 0 : baseNote)) {
                return isPlaying[noteValue] ? SlRgbState.WHITE : SlRgbState.WHITE_DIM;
            }
            return isPlaying[noteValue] ? trackColor : trackColorOff;
        } else if (pad.exists().get()) {
            final SlRgbState state;
            if (padSlots[index].getColor().isOff()) {
                state = isPlaying[noteValue] ? trackColor : trackColorOff;
            } else {
                state = isPlaying[noteValue] ? padSlots[index].getColor() : padSlots[index].getColorOff();
            }
            return padSlots[index].isSelected() ? state.getPulse() : state;
        } else if (isPlaying[noteValue]) {
            return SlRgbState.WHITE;
        }
        return SlRgbState.OFF;
    }
    
    private int indexToNoteValue(final int padIndex) {
        if (scaleIndex == 0) {
            return keyNoteOffset + KEY_MAPPING[padIndex];
        }
        return keyNoteOffset + baseNote + scaleSelection.get(scaleIndex).toOffsetOct(KEY_MAPPING[padIndex]);
    }
    
    private void handleNotes(final PlayingNote[] playingNotes) {
        if (!padLayer.isActive()) {
            return;
        }
        Arrays.fill(isPlaying, false);
        for (final PlayingNote playingNote : playingNotes) {
            isPlaying[playingNote.pitch()] = true;
        }
    }
    
    public void playNote(final int noteValue, final int vel) {
        if (!padLayer.isActive() || !padNotes.contains(noteValue)) {
            return;
        }
        final Integer index = keyToPadLookup[noteValue];
        if (vel > 0 && index >= 0 && index < 16) {
            if (selectModeActive.get()) {
                final DrumPad pad = padBank.getItemAt(index);
                pad.selectInEditor();
                viewControl.selectDrumDevice(padsToNotes[index]);
            }
        }
    }
    
    private boolean canScrollUp() {
        if (focusOnDrums) {
            return padsNoteOffset + 16 < 128;
        }
        return keyNoteOffset + 12 < 128;
    }
    
    private boolean canScrollDown() {
        if (focusOnDrums) {
            return padsNoteOffset - 4 >= 0;
        }
        return keyNoteOffset - 12 >= 0;
    }
    
    private void scrollPadBank(final int dir) {
        if (!focusOnDrums && scaleSelectModeActive.get()) {
            final int newIndex = scaleIndex + dir;
            if (newIndex >= 0 && newIndex < scaleSelection.size()) {
                scaleIndex = newIndex;
                screenHandler.notifyMessage("Select Scale", scaleSelection.get(scaleIndex).getName());
                applyNotes(keyNoteOffset);
            }
        } else if (!focusOnDrums && noteSelectModeActive.get()) {
            final int newIndex = baseNote + dir;
            if (newIndex >= 0 && newIndex < 12) {
                baseNote = newIndex;
                screenHandler.notifyMessage("Select Note", StringUtil.toSingleNoteValue(newIndex));
                applyNotes(keyNoteOffset);
            }
        } else {
            final boolean shiftActive = globalStates.getShiftState().get();
            if (padBank.exists().get()) {
                if (dir < 0) {
                    final int amount = padsNoteOffset > 8 ? (shiftActive ? -16 : -4) : -4;
                    padBank.scrollBy(amount);
                } else {
                    final int amount = padsNoteOffset == 0 ? 4 : (shiftActive ? 16 : 4);
                    padBank.scrollBy(amount);
                }
            } else {
                final int newPos;
                final int baseOffset = scaleIndex == 0 ? 0 : baseNote;
                if (dir < 0) {
                    final int amount = -12;
                    newPos = Math.max(0, keyNoteOffset + amount);
                } else {
                    final int amount = 12;
                    newPos = Math.min(108, keyNoteOffset + amount);
                }
                if (newPos != keyNoteOffset) {
                    keyNoteOffset = newPos;
                    screenHandler.notifyMessage("Pad Base Note", StringUtil.toNoteValue(keyNoteOffset + baseOffset));
                    applyNotes(keyNoteOffset);
                }
            }
        }
    }
    
    public void applyNotes(final int noteOffset) {
        if (padLayer.isActive()) {
            Arrays.fill(noteTable, -1);
            if (focusOnDrums) {
                for (int note = 0; note < 16; note++) {
                    final int padNote = IDX_TO_PAD_MAPPING[note];
                    final int value = noteOffset + note;
                    final int noteIndex = padNote < 8 ? padNote : padNote + 8;
                    noteTable[0x60 + noteIndex] = value < 128 ? value : -1;
                    padsToNotes[note] = noteOffset + note;
                }
            } else {
                final ScaleInterface scale = scaleSelection.get(scaleIndex);
                final int baseOffset = scaleIndex == 0 ? 0 : baseNote;
                for (int note = 0; note < 16; note++) {
                    final int padNote = IDX_TO_PAD_MAPPING[note];
                    final int value = noteOffset + baseOffset + scale.toOffsetOct(KEY_MAPPING[note]);
                    final int noteIndex = padNote < 8 ? padNote : padNote + 8;
                    noteTable[0x60 + noteIndex] = value < 128 ? value : -1;
                    padsToNotes[note] = noteOffset + note;
                }
            }
            noteInput.setKeyTranslationTable(noteTable);
        }
    }
    
    private void resetNotes() {
        Arrays.fill(noteTable, -1);
        noteInput.setKeyTranslationTable(noteTable);
    }
    
}
