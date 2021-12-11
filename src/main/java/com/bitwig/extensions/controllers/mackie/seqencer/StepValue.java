package com.bitwig.extensions.controllers.mackie.seqencer;

import com.bitwig.extensions.controllers.mackie.value.DerivedStringValueObject;
import com.bitwig.extensions.controllers.mackie.value.DoubleRangeValue;
import com.bitwig.extensions.controllers.mackie.value.IncrementalValue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

public class StepValue extends DerivedStringValueObject implements IncrementalValue, DoubleRangeValue {

   private final List<DoubleConsumer> callbacks = new ArrayList<>();
   private final double min;
   private final double max;
   private final double defaultValue;
   private boolean set = false;
   private double value;

   public StepValue(final double min, final double max, final double defaultValue) {
      this.min = min;
      this.max = max;
      this.defaultValue = defaultValue;
   }

   public void reset() {
      set(defaultValue);
   }

   @Override
   public double getMin() {
      return min;
   }

   @Override
   public double getMax() {
      return max;
   }

   public void unset() {
      set = false;
      fireChanged(convert(value));
      callbacks.forEach(callback -> callback.accept(value));
   }

   @Override
   public void addDoubleValueObserver(final DoubleConsumer callback) {
      callbacks.add(callback);
   }

   public void set(final double value) {
      this.value = value;
      set = true;
      fireChanged(convert(value));
      callbacks.forEach(callback -> callback.accept(value));
   }

   private String convert(final double value) {
      return set ? String.format("%2.1f", value * 100) + "%" : "<--->";
   }

   @Override
   public void init() {

   }

   @Override
   public double getRawValue() {
      return value;
   }

   @Override
   public String get() {
      return convert(value);
   }

   @Override
   public void increment(final int inc) {
      final double newValue = Math.min(Math.max(min, value + inc * 0.01), max);
      if (newValue != value) {
         value = newValue;
         callbacks.forEach(callback -> callback.accept(value));
         fireChanged(convert(value));
      }
   }

   @Override
   public String displayedValue() {
      return convert(value);
   }

   @Override
   public int scale(final double v, final int range) {
      if (!set) {
         if (min == 0) {
            return 0;
         } else {
            return range / 2;
         }
      }
      final double rng = range / (max - min);
      return (int) Math.round((v - min) * rng);
   }
}
