package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.color.RgbColor;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.color.RgbLightState;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components.HwElements;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components.ViewControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.PostConstruct;
import com.bitwig.extensions.framework.values.BasicStringValue;

public class DrumPadLayer extends Layer {
    private static final String[] noteValues = {"C ", "C#", "D ", "D#", "E ", "F ", "F#", "G ", "G#", "A ", "A#", "B "};
    
    private final RgbColor[] colorSlots = new RgbColor[8];
    //protected final Integer[] velTable = new Integer[128];
    private final Integer[] noteTable = new Integer[128];
    private final boolean[] isPlaying = new boolean[128];
    private final Set<Integer> padNotes = new HashSet<>();
    private final NoteInput noteInput;
    private DrumPadBank padBank;
    private RgbColor trackColor = RgbColor.OFF;
    private boolean focusOnDrums = false;
    private int keysNoteOffset = 60;
    private int padsNoteOffset = 32;
    private final int[] hangingNotes = new int[8];
    private final BasicStringValue padLocationInfo = new BasicStringValue("");
    
    public DrumPadLayer(final Layers layers, final MidiIn midiIn) {
        super(layers, "DRUM_PAD");
        noteInput = midiIn.createNoteInput("MIDI", "8A????", "9A????", "AA????");
        noteInput.setShouldConsumeEvents(false);
    }
    
    @PostConstruct
    public void init(final ViewControl viewControl, final HwElements hwElements) {
        final PinnableCursorDevice primaryDevice = viewControl.getPrimaryDevice();
        primaryDevice.hasDrumPads().addValueObserver(this::handleHasDrumsChanged);
        padBank = primaryDevice.createDrumPadBank(8);
        padBank.scrollPosition().addValueObserver(position -> {
            padsNoteOffset = position;
            if (isActive()) {
                applyNotes(padsNoteOffset);
            }
            updateInfo();
        });
        padBank.scrollPosition().set(30);
        padBank.setIndication(true);
        primaryDevice.hasDrumPads().addValueObserver(hasDrumPads -> {
            if (isActive()) {
                applyNotes(hasDrumPads ? padsNoteOffset : keysNoteOffset);
            }
            updateInfo();
        });
        padsNoteOffset = padBank.scrollPosition().get();
        Arrays.fill(noteTable, -1);
        Arrays.fill(colorSlots, RgbColor.GREEN);
        Arrays.fill(hangingNotes, -1);
        
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        
        final RgbButton[] buttons = hwElements.getPadBankBButtons();
        for (int i = 0; i < buttons.length; i++) {
            final int index = i;
            final int buttonIndex = i % 4 + (1 - (i / 4)) * 4;
            final RgbButton button = buttons[buttonIndex];
            
            padNotes.add(button.getNoteValue());
            final DrumPad pad = padBank.getItemAt(i);
            pad.exists().markInterested();
            
            pad.color().addValueObserver((r, g, b) -> colorSlots[index] = RgbColor.getColor(r, g, b));
            button.bindLight(this, () -> getLightState(index, pad));
        }
        
        cursorTrack.playingNotes().addValueObserver(this::handleNotes);
        cursorTrack.color().addValueObserver((r, g, b) -> trackColor = RgbColor.getColor(r, g, b));
    }
    
    private void handleHasDrumsChanged(final boolean hasDrums) {
        focusOnDrums = hasDrums;
    }
    
    public BasicStringValue getPadLocationInfo() {
        return padLocationInfo;
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
    
    public void navigate(final int dir) {
        if (focusOnDrums) {
            //driver.getOled().enableValues(DisplayMode.SCENE);
            padBank.scrollBy(dir * 4);
        } else {
            final int newOffset = keysNoteOffset + dir * 4;
            if (newOffset >= 0 && newOffset <= 116) {
                //muteNoteFromOffset(keysNoteOffset);
                keysNoteOffset = newOffset;
                applyNotes(keysNoteOffset);
                updateInfo();
            }
        }
    }
    
    private void updateInfo() {
        if (focusOnDrums) {
            padLocationInfo.set(
                String.format(String.format("Pads %s - %s", toNote(padsNoteOffset), toNote(padsNoteOffset + 7))));
        } else {
            padLocationInfo.set(
                String.format(String.format("Pads %s - %s", toNote(keysNoteOffset), toNote(keysNoteOffset + 7))));
        }
    }
    
    private String toNote(final int midiValue) {
        final int noteValue = midiValue % 12;
        final int octave = (midiValue / 12) - 2;
        return noteValues[noteValue] + octave;
    }
    
    private RgbLightState getLightState(final int index, final DrumPad pad) {
        final int noteValue = (focusOnDrums ? padsNoteOffset : keysNoteOffset) + index;
        if (noteValue < 128) {
            final boolean notePlaying = isPlaying[noteValue];
            if (focusOnDrums) {
                if (pad.exists().get()) {
                    if (colorSlots[index] != null) {
                        return notePlaying ? RgbLightState.WHITE : colorSlots[index].getColorState();
                    }
                } else {
                    return RgbLightState.OFF;
                }
                return notePlaying ? RgbLightState.WHITE : trackColor.getColorState();
            }
            return notePlaying ? RgbLightState.WHITE : trackColor.getColorState();
        } else {
            return RgbLightState.OFF;
        }
    }
    
    public void applyNotes(final int noteOffset) {
        Arrays.fill(noteTable, -1);
        for (int note = 0; note < 8; note++) {
            final int value = noteOffset + note;
            noteTable[0x2c + note] = value < 128 ? value : -1;
        }
        noteInput.setKeyTranslationTable(noteTable);
    }
    
    private void resetNotes() {
        Arrays.fill(noteTable, -1);
        noteInput.setKeyTranslationTable(noteTable);
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
        applyNotes(focusOnDrums ? padsNoteOffset : keysNoteOffset);
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        resetNotes();
    }
    
}
