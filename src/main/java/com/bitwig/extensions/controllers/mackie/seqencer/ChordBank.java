package com.bitwig.extensions.controllers.mackie.seqencer;

import com.bitwig.extensions.controllers.mackie.value.ChordType;
import com.bitwig.extensions.controllers.mackie.value.IntValueObject;

import java.util.ArrayList;
import java.util.List;

public class ChordBank {
   private final List<Chord> chords = new ArrayList<>();
   private final IntValueObject selectedIndex = new IntValueObject(0, 0, 7);

   public void init() {
      chords.add(new Chord(ChordType.MAJ, 0, 5));
      chords.add(new Chord(ChordType.MAJ, 5, 5));
      chords.add(new Chord(ChordType.MAJ, 7, 5));
      chords.add(new Chord(ChordType.SUS4, 0, 5));

      chords.add(new Chord(ChordType.C7, 0, 5));
      chords.add(new Chord(ChordType.MAJ7, 0, 5));
      chords.add(new Chord(ChordType.MIN, 9, 4));
      chords.add(new Chord(ChordType.MIN, 7, 4));
   }

   public Chord get() {
      return chords.get(selectedIndex.get());
   }

   public IntValueObject getSelectedIndex() {
      return selectedIndex;
   }
}
