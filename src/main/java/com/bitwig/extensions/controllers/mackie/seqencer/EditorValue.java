package com.bitwig.extensions.controllers.mackie.seqencer;

import com.bitwig.extensions.controllers.mackie.value.DerivedStringValueObject;
import com.bitwig.extensions.controllers.mackie.value.IncrementalValue;
import com.bitwig.extensions.remoteconsole.RemoteConsole;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class EditorValue extends DerivedStringValueObject implements IncrementalValue {
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

   public int getIntValue() {
      return setValue;
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
   }

   public void exitEdit() {
      edit = false;
      RemoteConsole.out.println("Exit");
      fireChanged(displayedValue());
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

   @Override
   public String get() {
      return displayedValue();
   }
}
