package com.bitwig.extensions.controllers.mackie.section;

import java.util.function.BooleanSupplier;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extensions.controllers.mackie.value.BooleanValueObject;

public class DrumNoteHandler {
	private final NoteInput noteInput;
	private final Integer[] notesToDrumTable = new Integer[128];
	private final int[] notesToPadsTable = new int[128];
	private final BooleanValueObject[] playing = new BooleanValueObject[8];
	private final boolean drumTracker[] = new boolean[8];

	private boolean active;

	private int drumScrollOffset = 0;
	private int noteOffset;
	private int padOffset;

	public DrumNoteHandler(final NoteInput noteInput, final DrumPadBank drumPadBank, final CursorTrack cursorTrack) {
		this.noteInput = noteInput;
		for (int i = 0; i < notesToDrumTable.length; i++) {
			notesToDrumTable[i] = Integer.valueOf(-1);
			notesToPadsTable[i] = -1;
		}
		noteInput.setKeyTranslationTable(notesToDrumTable);
		drumPadBank.scrollPosition().addValueObserver(offset -> {
			drumScrollOffset = offset;
			applyScale();
		});
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
		for (int i = 0; i < 8; i++) {
			drumTracker[i] = false;
		}
		for (final PlayingNote playingNote : notes) {
			final int padIndex = notesToPadsTable[playingNote.pitch()];
			if (padIndex != -1) {
				playing[padIndex].set(true);
				drumTracker[padIndex] = true;
			}
		}
		for (int i = 0; i < 8; i++) {
			if (!drumTracker[i]) {
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
			notesToPadsTable[i] = -1;
		}
		// TODO could go out of bounds be careful !!
		for (int i = 0; i < 8; i++) {
			final int noteToPadIndex = drumScrollOffset + i + noteOffset + padOffset;
			if (noteToPadIndex < 128) {
				notesToDrumTable[i] = noteToPadIndex;
				notesToPadsTable[noteToPadIndex] = i;
			}
		}
		noteInput.setKeyTranslationTable(notesToDrumTable);
	}

	public void deactivate() {
		if (!active) {
			return;
		}
		for (int i = 0; i < 8; i++) {
			notesToDrumTable[i] = -1;
		}
		noteInput.setKeyTranslationTable(notesToDrumTable);
	}

	public BooleanSupplier isPlaying(final int index) {
		return playing[index];
	}

}
