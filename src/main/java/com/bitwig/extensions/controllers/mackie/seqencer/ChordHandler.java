package com.bitwig.extensions.controllers.mackie.seqencer;

import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.controllers.mackie.Midi;
import com.bitwig.extensions.controllers.mackie.value.ChordType;
import com.bitwig.extensions.controllers.mackie.value.IntValueObject;
import com.bitwig.extensions.controllers.mackie.value.ValueObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChordHandler {
   public final static String[] NOTES = {"  C", " C#", "  D", " D#", "  E", "  F", " F#", "  G", " G#", "  A", " A#", "  B"};

   private NoteInput noteInput;

   private final ValueObject<ChordType> chordType = new ValueObject<>(ChordType.MAJ, ChordType::increment,
      ChordType::convert);
   private final IntValueObject chordBaseNote = new IntValueObject(0, 0, 11, v -> NOTES[v]);
   private final IntValueObject octaveOffset = new IntValueObject(4, 0, 8);
   private final IntValueObject inversion = new IntValueObject(0, 0, 3);
   private final IntValueObject expansion = new IntValueObject(0, -1, 4);
   private final int[] heldNotes = new int[128];

   public ChordHandler() {
      Arrays.fill(heldNotes, 0);
      expansion.addValueObserver(v -> {
         final int notes = chordType.get().getNotes().length + v - 1;
         inversion.setMax((1 << notes) - 1);
      });
      getChordType().addValueObserver((old, newValue) -> {
         final int notes = newValue.getNotes().length + expansion.get() - 1;
         inversion.setMax((1 << notes) - 1);
      });
   }

   public void init(final NoteInput noteInput) {
      this.noteInput = noteInput;
   }

   public IntValueObject getChordBaseNote() {
      return chordBaseNote;
   }

   public IntValueObject getInversion() {
      return inversion;
   }

   public IntValueObject getOctaveOffset() {
      return octaveOffset;
   }

   public IntValueObject getExpansion() {
      return expansion;
   }

   public ValueObject<ChordType> getChordType() {
      return chordType;
   }

   public void apply(final Chord chord) {
      chordType.set(chord.getChordType());
      octaveOffset.set(chord.getOctaveOffset());
      chordBaseNote.set(chord.getChordBaseNote());
      expansion.set(chord.getExpansion());
      inversion.set(chord.getInversion());
   }

   public List<Integer> getNotes() {
      final int[] notes = chordType.get().getNotes();
      final int limit = notes.length + expansion.get();
      final List<Integer> noteList = new ArrayList<>(limit);
      for (int i = 0; i < limit; i++) {
         int note = notes[i % notes.length];
         final int expandOct = i / notes.length;

         final int msk = limit - i - 1;
         if (msk < limit - 1 && (inversion.get() & (1 << msk)) != 0) {
            note = note - 12 * (expandOct + 1);
         }
         final int val = note + octaveOffset.get() * 12 + chordBaseNote.get() + expandOct * 12;
         if (val >= 0 && val < 127) {
            noteList.add(val);
         }
      }
      return noteList;
   }

   public void play(final Chord chord) {
      final List<Integer> notes = chord.getNotes();
      for (final Integer noteNr : notes) {
         heldNotes[noteNr]++;
         noteInput.sendRawMidiEvent(Midi.NOTE_ON, noteNr, chord.getVelocity());
      }
   }

   public void release(final Chord chord) {
      final List<Integer> notes = chord.getNotes();
      for (final Integer noteNr : notes) {
         heldNotes[noteNr]--;
         if (heldNotes[noteNr] <= 0) {
            noteInput.sendRawMidiEvent(Midi.NOTE_OFF, noteNr, 0);
         }
      }
      release();
   }

   public void play(final int velocity) {
      final List<Integer> notes = getNotes();
      for (final Integer noteNr : notes) {
         heldNotes[noteNr]++;
         noteInput.sendRawMidiEvent(Midi.NOTE_ON, noteNr, velocity);
      }
   }

   public void release() {
      for (int noteNr = 0; noteNr < heldNotes.length; noteNr++) {
         if (heldNotes[noteNr] > 0) {
            noteInput.sendRawMidiEvent(Midi.NOTE_OFF, noteNr, 0);
         }
         heldNotes[noteNr] = 0;
      }
   }

   public void playNotes(final NoteStepSlot slot) {
      for (final NoteStep step : slot.steps()) {
         noteInput.sendRawMidiEvent(Midi.NOTE_ON, step.y(), (int) Math.round(step.velocity() * 127));
         heldNotes[step.y()]++;
      }
   }


}
