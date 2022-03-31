package com.bitwig.extensions.controllers.mackie.section;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extensions.controllers.mackie.NotePlayingSetup;
import com.bitwig.extensions.controllers.mackie.value.ValueObject;

import java.util.*;

public class ScaleNoteHandler {
   private static final int PLAYING_BUTTONS = 32;

   private final NoteInput noteInput;
   private final Integer[] notesTable = new Integer[128];
   protected final Integer[] velTable = new Integer[128];
   private final Map<Integer, List<Integer>> notesToButtonsTable = new HashMap<>();

   private final List<ValueObject<NoteState>> playing = new ArrayList<>(PLAYING_BUTTONS);
   private final boolean[] isBaseNote = new boolean[PLAYING_BUTTONS];
   private final boolean[] tracker = new boolean[PLAYING_BUTTONS];

   private boolean active;

   private final NotePlayingSetup notePlaying;

   public ScaleNoteHandler(final NoteInput noteInput, final NotePlayingSetup notePlaying,
                           final CursorTrack cursorTrack) {
      this.noteInput = noteInput;
      this.notePlaying = notePlaying;
      Arrays.fill(notesTable, -1);
      notesToButtonsTable.clear();
      noteInput.setKeyTranslationTable(notesTable);
      for (int i = 0; i < PLAYING_BUTTONS; i++) {
         playing.add(new ValueObject<>(NoteState.OFF));
      }
      cursorTrack.playingNotes().addValueObserver(this::handleNotes);
      notePlaying.getOctaveOffset().addValueObserver(val -> applyScale());
      notePlaying.getBaseNote().addValueObserver(val -> applyScale());
      notePlaying.getScale().addValueObserver((oldValue, newValue) -> applyScale());
      notePlaying.getLayoutOffset().addValueObserver(val -> applyScale());
      notePlaying.getVelocity().addValueObserver(val -> applyVelTable());
      for (int i = 0; i < velTable.length; i++) {
         velTable[i] = notePlaying.getVelocity().get();
      }
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
      noteInput.setVelocityTranslationTable(velTable);
   }

   private void applyVelTable() {
      for (int i = 0; i < velTable.length; i++) {
         velTable[i] = notePlaying.getVelocity().get();
      }
      if (active) {
         noteInput.setVelocityTranslationTable(velTable);
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

   public void activate() {
      active = true;
      applyScale();
   }

   void applyScale() {
      if (!active) {
         return;
      }
      notesToButtonsTable.clear();

      final int[] notes = notePlaying.getScale().get().getNotes();
      final int layoutOffset = notePlaying.getLayoutOffset().get();
      final int octaveOffset = notePlaying.getOctaveOffset().get();
      final int baseNote = notePlaying.getBaseNote().get();

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
      for (int i = 0; i < PLAYING_BUTTONS; i++) {
         if (isBaseNote[i]) {
            playing.get(i).set(NoteState.BASENOTE);
         } else {
            playing.get(i).set(NoteState.OFF);
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
