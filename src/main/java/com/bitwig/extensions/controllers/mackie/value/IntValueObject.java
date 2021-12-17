package com.bitwig.extensions.controllers.mackie.value;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class IntValueObject implements IncrementalValue, IntValue {
   private final List<IntConsumer> valueChangedCallbacks = new ArrayList<>();
   private final List<RangeChangedCallback> rangeChangedCallbacks = new ArrayList<>();
   private int value;
   private int min;
   private int max;
   private final IntValueConverter converter;

   public IntValueObject(final int initValue, final int min, final int max) {
      value = initValue;
      this.min = min;
      this.max = max;
      converter = null;
   }

   public IntValueObject(final int initValue, final int min, final int max, final IntValueConverter converter) {
      value = initValue;
      this.min = min;
      this.max = max;
      this.converter = converter;
   }

   @Override
   public int getMax() {
      return max;
   }

   @Override
   public int getMin() {
      return min;
   }

   @Override
   public void addIntValueObserver(final IntConsumer callback) {
      valueChangedCallbacks.add(callback);
   }

   public void setMin(final int min) {
      this.min = min;
      if (value < min) {
         value = min;
         valueChangedCallbacks.forEach(listener -> listener.accept(value));
      }
      rangeChangedCallbacks.forEach(callback -> callback.rangeChanged(min, max));
   }

   public void setMax(final int max) {
      this.max = max;
      if (value > max) {
         value = max;
         valueChangedCallbacks.forEach(listener -> listener.accept(value));
      }
      rangeChangedCallbacks.forEach(callback -> callback.rangeChanged(min, max));
   }

   public void addValueObserver(final IntConsumer callback) {
      valueChangedCallbacks.add(callback);
   }

   @Override
   public void addRangeObserver(final RangeChangedCallback callback) {
      rangeChangedCallbacks.add(callback);
   }

   /**
    * Sets value to -1 for special purposes.
    */
   public void setDisabled() {
      if (value == -1) {
         return;
      }
      value = -1;
      valueChangedCallbacks.forEach(listener -> listener.accept(value));
   }

   public void set(final int value) {
      final int newValue = Math.max(min, Math.min(max, value));
      if (this.value == newValue) {
         return;
      }
      this.value = newValue;
      valueChangedCallbacks.forEach(listener -> listener.accept(value));
   }

   @Override
   public void increment(final int amount) {
      final int newValue = Math.max(min, Math.min(max, value + amount));
      set(newValue);
   }

   public int get() {
      return value;
   }

   @Override
   public int getIntValue() {
      return value;
   }

   @Override
   public String displayedValue() {
      if (converter != null) {
         return converter.convert(value);
      }
      return Integer.toString(value);
   }


}
