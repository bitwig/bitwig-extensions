package com.bitwig.extensions.controllers.mackie;

public class OverrideNoteAssignment implements NoteAssignment {

   private final int noteNr;

   public OverrideNoteAssignment(final int noteNr) {
      super();
      this.noteNr = noteNr;
   }

   @Override
   public int getNoteNo() {
      return noteNr;
   }

   @Override
   public int getType() {
      return Midi.NOTE_ON;
   }

   @Override
   public int getChannel() {
      return 0;
   }

   @Override
   public String toString() {
      return "OverrideNoteAssignment{" +
         "noteNr=" + noteNr +
         '}';
   }
}
