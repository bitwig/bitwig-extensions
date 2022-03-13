package com.bitwig.extensions.controllers.mackie.seqencer;

import com.bitwig.extensions.controllers.mackie.value.DerivedStringValueObject;
import com.bitwig.extensions.controllers.mackie.value.IncrementalValue;
import com.bitwig.extensions.controllers.mackie.value.IntValue;
import com.bitwig.extensions.controllers.mackie.value.RangeChangedCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class EditorValue extends DerivedStringValueObject implements IncrementalValue, IntValue {
   private final Converter converter;

   public interface Converter {
      String convert(boolean edit, int value);
   }

   private boolean edit = false;
   private int setValue;
   private final List<IntConsumer> valueChangedCallbacks = new ArrayList<>();
   private int editValue;

   public EditorValue(final int initValue, final Converter converter) {
      setValue = initValue;
      this.converter = converter;
   }

   @Override
   public int getMax() {
      return 127;
   }

   @Override
   public int getMin() {
      return 0;
   }

   @Override
   public void addIntValueObserver(final IntConsumer callback) {
      valueChangedCallbacks.add(callback);
   }

   @Override
   public void addRangeObserver(final RangeChangedCallback callback) {

   }

   @Override
   public int getIntValue() {
      return edit ? editValue : setValue;
   }

   public int getSetValue() {
      return setValue;
   }

   @Override
   public String get() {
      return displayedValue();
   }

   public void set(final int noteValue) {
      if (noteValue != setValue) {
         setValue = noteValue;
         fireChanged(displayedValue());
         fireChanged(noteValue);
      }
   }

   public void setEditValue(final int value) {
      edit = true;
      editValue = value;
      fireChanged(displayedValue());
      fireChanged(editValue);
   }

   public void exitEdit() {
      edit = false;
      fireChanged(displayedValue());
      fireChanged(setValue);
   }

   public boolean isEdit() {
      return edit;
   }

   public void fireChanged(final int value) {
      valueChangedCallbacks.forEach(callback -> callback.accept(value));
   }

   @Override
   public void increment(final int inc) {
      final int newValue = setValue + inc;
      if (newValue >= 0 && newValue < 128) {
         setValue = newValue;
         fireChanged(displayedValue());
         fireChanged(newValue);
      }
   }

   @Override
   public String displayedValue() {
      if (edit) {
         return converter.convert(edit, editValue);
      }
      return converter.convert(edit, setValue);
   }

   @Override
   public void init() {

   }

}
