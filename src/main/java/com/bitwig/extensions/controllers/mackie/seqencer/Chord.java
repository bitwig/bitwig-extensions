package com.bitwig.extensions.controllers.mackie.seqencer;

import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.controllers.mackie.Midi;
import com.bitwig.extensions.controllers.mackie.value.ChordType;

import java.util.ArrayList;
import java.util.List;

public class Chord {
   private ChordType chordType;
   private int chordBaseNote;
   private int octaveOffset;
   private int inversion;
   private int expansion;

   public Chord(final ChordType chordType, final int chordBaseNote, final int octaveOffset) {
      this.chordType = chordType;
      this.chordBaseNote = chordBaseNote;
      this.octaveOffset = octaveOffset;
   }

   public ChordType getChordType() {
      return chordType;
   }

   public void setChordType(final ChordType chordType) {
      this.chordType = chordType;
   }

   public void setChordBaseNote(final int chordBaseNote) {
      this.chordBaseNote = chordBaseNote;
   }

   public void setOctaveOffset(final int octaveOffset) {
      this.octaveOffset = octaveOffset;
   }

   public int getChordBaseNote() {
      return chordBaseNote;
   }

   public int getOctaveOffset() {
      return octaveOffset;
   }

   public int getInversion() {
      return inversion;
   }

   public void setInversion(final int inversion) {
      this.inversion = inversion;
   }

   public int getExpansion() {
      return expansion;
   }

   public void setExpansion(final int expansion) {
      this.expansion = expansion;
   }

   public List<Integer> getNotes() {
      final int[] notes = chordType.getNotes();
      final int limit = notes.length + expansion;
      final List<Integer> noteList = new ArrayList<>(limit);
      for (int i = 0; i < limit; i++) {
         int note = notes[i % notes.length];
         final int expandOct = i / notes.length;

         final int msk = limit - i - 1;
         if (msk < limit - 1 && (inversion & (1 << msk)) != 0) {
            note = note - 12 * (expandOct + 1);
         }
         final int val = note + octaveOffset * 12 + chordBaseNote + expandOct * 12;
         if (val >= 0 && val < 127) {
            noteList.add(val);
         }
      }
      return noteList;
   }

   public void play(final List<Integer> heldNotes, final NoteInput noteInput, final int velocity) {
      release(heldNotes, noteInput);
      heldNotes.addAll(getNotes());
      heldNotes.forEach(noteNr -> noteInput.sendRawMidiEvent(Midi.NOTE_ON, noteNr, velocity));
   }

   public void release(final List<Integer> heldNotes, final NoteInput noteInput) {
      heldNotes.forEach(noteNr -> noteInput.sendRawMidiEvent(Midi.NOTE_OFF, noteNr, 0));
      heldNotes.clear();
   }


}
