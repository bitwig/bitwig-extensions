package com.bitwig.extensions.framework.values;

public enum Scale {

   CHROMATIC("Chromatic", 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), //
   MAJOR("Ionian/Major", 0, 2, 4, 5, 7, 9, 11), //
   MINOR("Aeolian/Minor", 0, 2, 3, 5, 7, 8, 10), //
   PENTATONIC("Pentatonic", 0, 2, 4, 7, 9), //
   PENTATONIC_MINOR("Pentatonic Minor", 0, 3, 5, 7, 10), //
   DORIAN("Dorian (B/g)", 0, 2, 3, 5, 7, 9, 10), //
   PHRYGIAN("Phrygian (A-flat/f)", 0, 1, 3, 5, 7, 8, 10), //
   LYDIAN("Lydian (D/e)", 0, 2, 4, 6, 7, 9, 11), //
   MIXOLYDIAN("Mixolydian (F/d)", 0, 2, 4, 5, 7, 9, 10), //
   LOCRIAN("Locrian", 0, 1, 3, 5, 6, 8, 10), //
   DIMINISHED("Diminished", 0, 2, 3, 5, 6, 8, 9, 10), //
   MAJOR_BLUES("Major Blues", 0, 3, 4, 7, 9, 10), //
   MINOR_BLUES("Minor Blues", 0, 3, 4, 6, 7, 10), //
   WHOLE_TONE("Whole", 0, 2, 4, 6, 8, 10), //
   ARABIAN("Arabian", 0, 2, 4, 5, 6, 8, 10), //
   EGYPTIAN("Egyptian", 0, 2, 5, 7, 10), //
   GYPSI("Gypsi", 0, 2, 3, 6, 7, 8, 11), //
   SPANISH("Spanish", 0, 1, 3, 4, 5, 7, 8, 10);


   private final String name;
   private final int[] intervals;
   private final boolean[] inscaleMatch = new boolean[12];

   Scale(final String name, final int... notes) {
      this.name = name;
      this.intervals = notes;
      for (int i = 0; i < this.intervals.length; i++) {
         inscaleMatch[this.intervals[i] % 12] = true;
      }
   }

   public String getName() {
      return name;
   }

   public int[] getIntervals() {
      return intervals;
   }

   public int getNextNote(final int startNote, final int baseNote, final int amount) {
      final int noteIndex = (startNote + 12 - baseNote) % 12;
      int octave = startNote < baseNote ? (startNote - baseNote - 12) / 12 : (startNote - baseNote) / 12;

      final int index = findScaleIndex(noteIndex, intervals);

      int nextIndex = index + amount;
      if (nextIndex >= intervals.length) {
         nextIndex = 0;
         octave++;
      } else if (nextIndex < 0) {
         nextIndex = intervals.length - 1;
         octave--;
      }
      return intervals[nextIndex] + baseNote + octave * 12;
   }

   public boolean inScale(int noteBase) {
      return inscaleMatch[noteBase % 12];
   }

   public int nextInScale(int index) {
      int calcIndex = index;
      while (index < 26) {
         if (inScale(index)) {
            return index;
         }
         index++;
      }
      return index;
   }

   private static int findScaleIndex(final int noteIndex, final int[] intervalls) {
      for (int i = 0; i < intervalls.length; i++) {
         if (intervalls[i] >= noteIndex) {
            return i;
         }
      }
      return -1;
   }

   /**
    * Given a starting note, determines the highest note at the end of the range.
    *
    * @param startNote starting note
    * @param noteRange available notes
    * @return last note in range
    */
   public int highestNote(final int startNote, final int noteRange) {
      final int octaves = noteRange / intervals.length;
      final int lastValue = intervals[(noteRange - 1) % intervals.length];
      return startNote + octaves * 12 + lastValue;
   }


}
