package com.bitwig.extensions.controllers.arturia.beatsteppro;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.Midi;

import java.util.Arrays;

public class PadLayer extends Layer {
    public static final int PAD_CHANNEL = 0xC;
    public final static String[] NOTES = {" C", "C#", " D", "D#", " E", " F", "F#", " G", "G#", " A", "A#", " B"};
    private final BeatStepProExtension driver;
    private final NoteInput noteInput;
    private final Integer[] noteTable = new Integer[128];
    private final int[] hangingNotes = new int[16];
    private Scale basicScale = Scale.MINOR;
    private Scale currentScale = Scale.CHROMATIC;
    private int baseNote = 0;

    private int octave = 4;
    private boolean focusOnDrums;

    public PadLayer(final BeatStepProExtension driver, final PinnableCursorDevice primaryDevice) {
        super(driver.getLayers(), "ARTURIA_PAD");
        this.driver = driver;
        driver.getMidiIn().setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);
        primaryDevice.hasDrumPads().addValueObserver(this::handleHasDrumsChanged);

        noteInput = driver.getMidiIn().createNoteInput("MIDI_PAD", "8C????", "9C????", "AC????");
        noteInput.setShouldConsumeEvents(false);

        Arrays.fill(noteTable, -1);
    }

    private void handleHasDrumsChanged(final boolean hasDrums) {
        focusOnDrums = hasDrums;
        if (focusOnDrums) {
            currentScale = Scale.DRUM;
        } else {
            currentScale = basicScale;
        }
        applyNotes();
    }

    private void onMidi0(final ShortMidiMessage msg) {
        if (!isActive()) {
            return;
        }
        final int channel = msg.getChannel();
        final int sb = msg.getStatusByte() & (byte) 0xF0;
        if (channel == PAD_CHANNEL && (sb == Midi.NOTE_ON || sb == Midi.NOTE_OFF)) {
            notifyNote(sb, msg.getData1());
        }
    }

    public void notifyRelease(final int noteValue) {
        final int index = noteValue - 36;
        final int offset = currentScale.getOffset(index);
        if (offset == -1) {
            return;
        }
        if (hangingNotes[index] != -1) {
            noteInput.sendRawMidiEvent(Midi.NOTE_OFF | PAD_CHANNEL, hangingNotes[index], 0);
        }
        hangingNotes[index] = -1;
    }

    private void notifyNote(final int sb, final int noteValue) {
        final int index =  noteValue - 36;
        if(index < 0 || index > 15) {
            return;
        }
        final int offset = currentScale.getOffset(index);
        if (offset == -1) {
            return;
        }
        final int notePlayed = baseNote + octave * 12 + offset;
        if (sb == Midi.NOTE_ON) {
            hangingNotes[index] = notePlayed;
        } else if (sb == Midi.NOTE_OFF) {
            if (hangingNotes[index] != -1 && hangingNotes[index] != notePlayed) {
                noteInput.sendRawMidiEvent(Midi.NOTE_OFF | PAD_CHANNEL, hangingNotes[index], 0);
            }
            hangingNotes[index] = -1;
        }
    }
    
    private void applyNotes() {
        Arrays.fill(noteTable, -1);
        final int base = currentScale.isDrum() ? 36 : baseNote + octave * 12;
        for (int i = 0; i < 16; i++) {
            final int offset = currentScale.getOffset(i);
            if (offset != -1) {
                final int value = base + offset;
                noteTable[36 + i] = value < 128 ? value : -1;
            }
        }
        noteInput.setKeyTranslationTable(noteTable);
    }

    public String getCurrentScale() {
        return NOTES[baseNote] + " " + currentScale.getName();
    }


    public void setBaseNote(final int baseNote) {
        this.baseNote = baseNote;
        if (!focusOnDrums) {
            final String message = String.format("Beatstep Base Note : %s %d %s", NOTES[baseNote], getOctave(),
                    basicScale.getName());
            driver.getHost().showPopupNotification(message);
            if (isActive()) {
                applyNotes();
            }
        }
    }

    public void setScale(final Scale scale) {
        basicScale = scale;
        if (!focusOnDrums) {
            currentScale = basicScale;
            if (currentScale.isDrum()) {
                driver.getHost().showPopupNotification("Beatstep Scale : Drum Layout");
            } else {
                final String message = String.format("Beatstep Scale : %s %d %s", NOTES[baseNote], getOctave(),
                        basicScale.getName());
                driver.getHost().showPopupNotification(message);
            }
            if (isActive()) {
                applyNotes();
            }
        }
    }

    public void changeOctave(final int amount) {
        final int newValue = octave + amount;
        if (newValue > 0 && newValue < 10) {
            octave = newValue;
        }
        final String message = String.format("Beatstep Octave : %s %d %s", NOTES[baseNote], getOctave(),
                basicScale.getName());
        driver.getHost().showPopupNotification(message);
        if (isActive()) {
            applyNotes();
        }
    }

    private int getOctave() {
        return (currentScale.isDrum() ? 36 : baseNote + octave * 12) / 12 - 3;
    }

    private void resetNotes() {
        Arrays.fill(noteTable, -1);
        noteInput.setKeyTranslationTable(noteTable);
    }

    @Override
    protected void onActivate() {
        super.onActivate();
        applyNotes();
    }


    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        resetNotes();
    }

}
