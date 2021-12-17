package com.bitwig.extensions.controllers.mackie.section;

public enum Scale {

//    /* 5 keys */
//    addScale(new MusicalScale("Hirajoshi", new int[]{ 0, 2, 3, 7, 8 }));
//    addScale(new MusicalScale("Iwato", new int[]{ 0, 1, 5, 6, 10 })); // related to Hirajoshi (after)
//    addScale(new MusicalScale("Kumoi", new int[]{ 0, 2, 3, 7, 9 }));
//    addScale(new MusicalScale("In Sen", new int[]{ 0, 1, 5, 7, 10 })); // related to Kumoi (after)
//    addScale(new MusicalScale("Yo scale", new int[]{ 0, 2, 5, 7, 9 }));
//
//    /* scales with augmented second */
//    addScale(new MusicalScale("Todi", new int[]{ 0, 1, 3, 5, 6, 7, 11 }));
//    addScale(new MusicalScale("Phrygian Dominant", new int[]{ 0, 1, 4, 5, 7, 8, 10 }));
//    addScale(new MusicalScale("Marva", new int[]{ 0, 1, 4, 6, 7, 9, 11 }));

   CHROMATIC("Chromatic", "Chrom", 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), //
   MAJOR("Major", "Major", 0, 2, 4, 5, 7, 9, 11), //
   MINOR("Minor", "Minor", 0, 2, 3, 5, 7, 8, 10), //
   PENTATONIC("Pentatonic", "Penta", 0, 2, 4, 7, 9), //
   PENTA_MINOR("Pentatonic Minor", "PenMin", 0, 3, 5, 7, 10), //
   DORIAN("Dorian", "Dorian", 0, 2, 3, 5, 7, 9, 10), //
   PHRYGIAN("Phrygian", "Phryg", 0, 1, 3, 5, 7, 8, 10), //
   LYDIAN("Lydian", "Lydian", 0, 2, 4, 6, 7, 9, 11), //
   MIXOLYDIAN("Mixolydian", "Myxold", 0, 2, 4, 5, 7, 9, 10), //
   LOCRIAN("Locrian", "Locrn", 0, 1, 3, 5, 6, 8, 10), //
   BLUES("Blues", "Blues", 0, 3, 5, 6, 7, 10), //
   BLUESMAJOR("Major Blues", "MBlues", 0, 2, 3, 4, 7, 9), //
   WHOLETONE("Whole Tone", "WholeT", 0, 2, 4, 6, 8, 10), //
   HARMONICMINOR("Harmonic Minor", "HrmMin", 0, 2, 3, 5, 7, 8, 11),
   DOUBLEHARMONIC("Double Harmonic", "DblHrm", 0, 1, 4, 5, 7, 8, 11);

   private final String name;
   private final int[] notes;
   private final String shortName;
   private final boolean[] inScale = new boolean[12];

   Scale(final String name, final String shortName, final int... notes) {
      this.name = name;
      this.notes = notes;
      this.shortName = shortName;
      for (final int note : notes) {
         inScale[note] = true;
      }
   }

   public String getName() {
      return name;
   }

   public int[] getNotes() {
      return notes;
   }

   public String getShortName() {
      return shortName;
   }

   public boolean inScale(final int baseNote, final int note) {
      if (note < 0 || note > 127) {
         return false;
      }
      return inScale[(note + 12 - baseNote) % 12];
   }

}
