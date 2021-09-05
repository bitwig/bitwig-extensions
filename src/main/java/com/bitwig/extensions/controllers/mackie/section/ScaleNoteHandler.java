package com.bitwig.extensions.controllers.mackie.section;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extensions.controllers.mackie.value.ValueObject;

public class ScaleNoteHandler {
	private static final int PLAYING_BUTTONS = 32;

	private final NoteInput noteInput;
	private final Integer[] notesTable = new Integer[128];
	private final Map<Integer, List<Integer>> notesToButtonsTable = new HashMap<>();

	private final List<ValueObject<NoteState>> playing = new ArrayList<>(PLAYING_BUTTONS);
	private final boolean[] isBaseNote = new boolean[PLAYING_BUTTONS];
	private final boolean tracker[] = new boolean[PLAYING_BUTTONS];

	private boolean active;

	private Scale scale = Scale.MINOR;
	private int baseNote = 0;
	private int octaveOffset = 3;
	private final int layoutOffset = 3; // 4ths

	public ScaleNoteHandler(final NoteInput noteInput, final CursorTrack cursorTrack) {
		this.noteInput = noteInput;
		for (int i = 0; i < notesTable.length; i++) {
			notesTable[i] = Integer.valueOf(-1);
			notesToButtonsTable.clear();
		}
		noteInput.setKeyTranslationTable(notesTable);
		for (int i = 0; i < PLAYING_BUTTONS; i++) {
			playing.add(new ValueObject<>(NoteState.OFF));
		}
		cursorTrack.playingNotes().addValueObserver(this::handleNotes);
	}

	public void setScale(final Scale scale) {
		this.scale = scale;
	}

	public void setBaseNote(final int baseNote) {
		this.baseNote = baseNote;
	}

	public Scale getScale() {
		return scale;
	}

	public int getBaseNote() {
		return baseNote;
	}

	private void handleNotes(final PlayingNote[] notes) {
		if (!active) {
			return;
		}
		for (int i = 0; i < PLAYING_BUTTONS; i++) {
			tracker[i] = false;
		}
		for (final PlayingNote playingNote : notes) {
			final List<Integer> pads = notesToButtonsTable.get(playingNote.pitch());
			if (pads != null) {
				pads.forEach(index -> {
					playing.get(index).set(NoteState.PLAYING);
					tracker[index] = true;
				});
			}
		}
		for (int i = 0; i < PLAYING_BUTTONS; i++) {
			if (!tracker[i]) {
				if (isBaseNote[i]) {
					playing.get(i).set(NoteState.BASENOTE);
				} else {
					playing.get(i).set(NoteState.OFF);
				}
			}
		}
	}

	public void navigateHorizontal(final int direction, final boolean pressed) {
		if (!pressed) {
			return;
		}
		if (direction > 0) {
		} else {
		}
	}

	public void navigateVertical(final int direction, final boolean pressed) {
		if (!pressed) {
			return;
		}
		if (direction > 0) {
			if (octaveOffset < 6) {
				octaveOffset++;
			}
			applyScale();
		} else {
			if (octaveOffset > 0) {
				octaveOffset--;
			}
			applyScale();
		}
	}

	public void activate() {
		active = true;
		applyScale();
	}

	void applyScale() {
		if (!active) {
			return;
		}
		notesToButtonsTable.clear();

		final int[] notes = scale.getNotes();
		for (int padMidiNoteNr = 0; padMidiNoteNr < PLAYING_BUTTONS; padMidiNoteNr++) {
			final int row = 3 - padMidiNoteNr / 8;
			final int col = padMidiNoteNr % 8;

			final int rowOffset = col + row * layoutOffset;
			final int oct = rowOffset / notes.length + octaveOffset;
			final int noteIndex = rowOffset % notes.length;
			final int note = notes[noteIndex];
			isBaseNote[padMidiNoteNr] = noteIndex == 0;
			// Chromatic
			// final int noteToPadIndex = baseNote + octaveOffset * 12 + rowOffset;
			final int noteToPadIndex = baseNote + oct * 12 + note;

			if (noteToPadIndex < 128) {
				notesTable[padMidiNoteNr] = noteToPadIndex;
				List<Integer> l = notesToButtonsTable.get(noteToPadIndex);
				if (l == null) {
					l = new ArrayList<>();
					notesToButtonsTable.put(noteToPadIndex, l);
				}
				l.add(padMidiNoteNr);
			}
		}
		noteInput.setKeyTranslationTable(notesTable);
	}

	public void deactivate() {
		if (!active) {
			return;
		}
		for (int i = 0; i < PLAYING_BUTTONS; i++) {
			notesTable[i] = -1;
		}
		noteInput.setKeyTranslationTable(notesTable);
	}

	public ValueObject<NoteState> isPlaying(final int index) {
		return playing.get(index);
	}

}
