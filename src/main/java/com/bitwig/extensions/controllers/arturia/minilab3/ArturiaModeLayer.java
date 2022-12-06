package com.bitwig.extensions.controllers.arturia.minilab3;

import java.util.Arrays;

import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.framework.Layer;

public class ArturiaModeLayer extends Layer {
    private final MiniLab3Extension driver;
    private final NoteInput noteInput;
    private final Integer[] noteTable = new Integer[128];

    public ArturiaModeLayer(final MiniLab3Extension driver) {
        super(driver.getLayers(), "ARTURIA_PAD");
        this.driver = driver;
        noteInput = driver.getMidiIn().createNoteInput("MIDI_", "89????", "99????", "A9????");
        noteInput.setShouldConsumeEvents(false);
        Arrays.fill(noteTable, -1);
    }

    private void applyNotes() {
        final RgbButton[] aButtons = driver.getPadBankAButtons();
        final RgbButton[] bButtons = driver.getPadBankBButtons();
        for (final RgbButton button : aButtons) {
            noteTable[button.getNoteValue()] = button.getNoteValue();
        }
        for (final RgbButton button : bButtons) {
            noteTable[button.getNoteValue()] = button.getNoteValue();
        }
        noteInput.setKeyTranslationTable(noteTable);
    }

    protected void resetNotes() {
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
