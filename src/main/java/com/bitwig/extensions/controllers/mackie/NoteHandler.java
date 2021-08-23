package com.bitwig.extensions.controllers.mackie;

import java.util.function.BooleanSupplier;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extensions.controllers.mackie.value.BooleanValueObject;

public class NoteHandler {
	private final NoteInput noteInput;
	private final Integer[] noteTable = new Integer[128];
	private final int[] noteToPad = new int[128];

	private final BooleanValueObject[] playing = new BooleanValueObject[8];
	private final boolean tracker[] = new boolean[8];

	private boolean active;

	private int drumScrollOffset = 0;
	private int noteOffset;
	private int padOffset;

	public NoteHandler(final MidiIn midiIn, final DrumPadBank drumPadBank, final CursorTrack cursorTrack) {
		noteInput = midiIn.createNoteInput("MIDI", "80????", "90????");
		noteInput.setShouldConsumeEvents(true);
		for (int i = 0; i < noteTable.length; i++) {
			noteTable[i] = Integer.valueOf(-1);
			noteToPad[i] = -1;
		}
		noteInput.setKeyTranslationTable(noteTable);
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
			tracker[i] = false;
		}
		for (final PlayingNote playingNote : notes) {
			final int padIndex = noteToPad[playingNote.pitch()];
			if (padIndex != -1) {
				playing[padIndex].set(true);
				tracker[padIndex] = true;
			}
		}
		for (int i = 0; i < 8; i++) {
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
			noteToPad[i] = -1;
		}
		// TODO could go out of bounds be careful !!
		for (int i = 0; i < 8; i++) {
			final int noteToPadIndex = drumScrollOffset + i + noteOffset + padOffset;
			if (noteToPadIndex < 128) {
				noteTable[i] = noteToPadIndex;
				noteToPad[noteToPadIndex] = i;
			}
		}
		noteInput.setKeyTranslationTable(noteTable);
	}

	public void deactivate() {
		if (!active) {
			return;
		}
		for (int i = 0; i < 8; i++) {
			noteTable[i] = -1;
		}
		noteInput.setKeyTranslationTable(noteTable);
	}

	public BooleanSupplier isPlaying(final int index) {
		return playing[index];
	}

}
