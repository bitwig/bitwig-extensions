package com.bitwig.extensions.controllers.mackie.seqencer;

import com.bitwig.extensions.controllers.mackie.NotePlayingSetup;
import com.bitwig.extensions.controllers.mackie.value.DerivedStringValueObject;
import com.bitwig.extensions.controllers.mackie.value.IncrementalValue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class NoteValue extends DerivedStringValueObject implements IncrementalValue {

   private final boolean active = false;
   private int noteValue;
   private final List<IntConsumer> valueChangedCallbacks = new ArrayList<>();

   public NoteValue(final int initValue) {
      noteValue = initValue;
   }

   public int getIntValue() {
      return noteValue;
   }

   public void set(final int noteValue) {
      if (noteValue != this.noteValue) {
         this.noteValue = noteValue;
         fireChanged(displayedValue());
         fireChanged(noteValue);
      }
   }

   public void addIntValueObserver(final IntConsumer callback) {
      if (!valueChangedCallbacks.contains(callback)) {
         valueChangedCallbacks.add(callback);
      }
   }

   public void fireChanged(final int value) {
      valueChangedCallbacks.forEach(callback -> callback.accept(value));
   }

   @Override
   public void increment(final int inc) {
      final int newValue = noteValue + inc;
      if (newValue >= 0 && newValue < 128) {
         noteValue = newValue;
         fireChanged(displayedValue());
         fireChanged(newValue);
      }
   }

   @Override
   public String displayedValue() {
      final String base = NotePlayingSetup.NOTES[noteValue % 12];
      final int octave = noteValue / 12;
      if (active) {
         return String.format("<%s%d>", base, octave - 2);
      }
      return String.format("%s%d", base, octave - 2);
   }

   @Override
   public void init() {

   }

   @Override
   public String get() {
      return displayedValue();
   }
}
