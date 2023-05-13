package com.bitwig.extensions.controllers.mackie.value;

public enum ChordType {
   THIRDS("3t", 0, 4),
   FOURTHS("4t", 0, 5),
   FIFTHS("5t", 0, 7),
   MAJ("Maj", 0, 4, 7),
   MIN("Min", 0, 3, 7),
   MAJ7("Maj7", 0, 4, 7, 11),
   M_MAJ7("mMaj7", 0, 3, 7, 11),
   C6("6", 0, 4, 7, 9),
   M6("m6", 0, 3, 7, 9),
   CM6("M6", 0, 4, 7, 9),
   C7("7", 0, 4, 7, 10),
   M7("m7", 0, 3, 7, 10),
   C9("9", 0, 4, 7, 10, 14),
   MAJ9("maj9", 0, 4, 7, 11, 14),
   C11("11", 0, 4, 7, 10, 14, 17),
   M9("m9", 0, 3, 7, 10, 14),
   ADD9("add9", 0, 4, 7, 14),
   M_ADD9("madd9", 0, 3, 7, 14),
   SSU2("Sus2", 0, 2, 7),
   SUS4("Sus4", 0, 5, 7),
   SUS74("7Sus4", 0, 5, 7, 10),
   SUS94("9Sus4", 0, 5, 7, 10, 14),
   M_B_5("mb5", 0, 3, 6),
   M7_B_5("mb5", 0, 3, 6, 10),
   MM7_B_5("Mb5", 0, 4, 6, 10),
   M_C_5("M#5", 0, 4, 8),
   DIM("dim", 0, 4, 6);

   private final String name;
   private final int[] notes;

   ChordType(final String name, final int... notes) {
      this.name = name;
      this.notes = notes;
   }

   public int[] getNotes() {
      return notes;
   }

   public String getName() {
      return name;
   }

   public static ChordType increment(final ChordType current, final int amount) {
      final int ord = current.ordinal();
      final ChordType[] values = ChordType.values();
      final int newOrd = ord + amount;
      if (newOrd < 0) {
         return values[0];
      }
      if (newOrd >= values.length) {
         return values[values.length - 1];
      }
      return values[newOrd];
   }

   public static String convert(final ChordType scale) {
      return scale.getName();
   }


}
