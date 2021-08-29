package com.bitwig.extensions.controllers.mackie.section;

import java.util.function.BooleanSupplier;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extensions.controllers.mackie.value.BooleanValueObject;

public class ScaleNoteHandler {
	private final NoteInput noteInput;
	private final Integer[] notesTable = new Integer[128];
	private final int[] notesToButtonsTable = new int[128];
	private final BooleanValueObject[] playing = new BooleanValueObject[32];
	private final boolean tracker[] = new boolean[32];

	private boolean active;

	private final int drumScrollOffset = 0;
	private int noteOffset;
	private int padOffset;

	public ScaleNoteHandler(final NoteInput noteInput, final CursorTrack cursorTrack) {
		this.noteInput = noteInput;
		for (int i = 0; i < notesTable.length; i++) {
			notesTable[i] = Integer.valueOf(-1);
			notesToButtonsTable[i] = -1;
		}
		noteInput.setKeyTranslationTable(notesTable);
		for (int i = 0; i < 8; i++) {
			playing[i] = new BooleanValueObject();
			playing[i].set(false);
		}
		cursorTrack.playingNotes().addValueObserver(this::handleNotes);
	}

	private void handleNotes(final PlayingNote[] notes) {
		if (!active) {
			return;
		}
		for (int i = 0; i < 32; i++) {
			tracker[i] = false;
		}
		for (final PlayingNote playingNote : notes) {
			final int padIndex = notesToButtonsTable[playingNote.pitch()];
			if (padIndex != -1) {
				playing[padIndex].set(true);
				tracker[padIndex] = true;
			}
		}
		for (int i = 0; i < 32; i++) {
			if (!tracker[i]) {
				playing[i].set(false);
			}
		}
	}

	public void activate(final int inputOffset, final int padOffset) {
		this.noteOffset = inputOffset; // With MCU happens to be 0
		this.padOffset = padOffset;
		active = true;
		applyScale();
	}

	void applyScale() {
		if (!active) {
			return;
		}
		for (int i = 0; i < 128; i++) {
			notesToButtonsTable[i] = -1;
		}
		// TODO could go out of bounds be careful !!
		for (int i = 0; i < 32; i++) {
			final int noteToPadIndex = drumScrollOffset + i + noteOffset + padOffset;
			if (noteToPadIndex < 128) {
				notesTable[i] = noteToPadIndex;
				notesToButtonsTable[noteToPadIndex] = i;
			}
		}
		noteInput.setKeyTranslationTable(notesTable);
	}

	public void deactivate() {
		if (!active) {
			return;
		}
		for (int i = 0; i < 8; i++) {
			notesTable[i] = -1;
		}
		noteInput.setKeyTranslationTable(notesTable);
	}

	public BooleanSupplier isPlaying(final int index) {
		return playing[index];
	}

}
