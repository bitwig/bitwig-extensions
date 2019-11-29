package com.bitwig.extensions.framework;

public final class MusicalScale
{
   public MusicalScale(final String name, final int[] notes)
   {
      mName = name;
      mNotes = notes;
   }

   public final int[] getNotes()
   {
      return mNotes;
   }

   public final String getName()
   {
      return mName;
   }

   public final boolean isRootMidiNote(final int midiRootKey, final int midiNote)
   {
      return (midiNote - midiRootKey) % 12 == 0;
   }

   public final boolean isMidiNoteInScale(int midiRootKey, int midiNote)
   {
      if (midiNote < 0)
         return false;

      // set midiNote to be an offset between 0 and 11 relative to the root.
      midiRootKey %= 12;
      midiNote += 12 - midiRootKey;
      midiNote %= 12;

      // check if the note is in the scale
      for (final int note : mNotes)
         if (midiNote == note)
            return true;

      return false;
   }

   public void setIndexInLibrary(final int indexInLibrary)
   {
      mIndexInLibrary = indexInLibrary;
   }

   public int getIndexInLibrary()
   {
      return mIndexInLibrary;
   }

   public int getNotesCount()
   {
      return mNotes.length;
   }

   public int computeNote(final int rootNote, int octave, int offset)
   {
      octave += offset / getNotesCount();
      offset = offset % getNotesCount();

      if (offset < 0)
      {
         --octave;
         offset += getNotesCount();
      }

      final int note = rootNote + 12 * octave + mNotes[offset];

      if (note > 127)
         return -1;
      if (note < 0)
         return -1;
      return note;
   }

   private final int[] mNotes;
   private final String mName;
   private int mIndexInLibrary;
}
