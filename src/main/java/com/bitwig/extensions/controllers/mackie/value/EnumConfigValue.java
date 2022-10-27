package com.bitwig.extensions.controllers.mackie.value;

import com.bitwig.extension.callback.StringValueChangedCallback;
import com.bitwig.extension.controller.api.StringValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class EnumConfigValue<T> implements IncrementalValue, StringValue, IntValue {

   private final List<StringValueChangedCallback> callbacks = new ArrayList<>();
   private final List<IntConsumer> intCallbacks = new ArrayList<>();
   private final List<Consumer<T>> valueCallbacks = new ArrayList<>();
   private final List<T> values = new ArrayList<>();
   private final Map<T, String> valueToDisplay = new HashMap<>();
   private final Map<T, Integer> valueToIndex = new HashMap<>();
   private T value;
   private boolean set = false;

   public EnumConfigValue<T> add(final T value, final String display) {
      values.add(value);
      valueToDisplay.put(value, display);
      valueToIndex.put(value, values.size() - 1);
      return this;
   }

   public EnumConfigValue<T> init(final T value) {
      this.value = value;
      return this;
   }

   public void set(final T value) {
      if (!set || value != this.value) {
         this.value = value;
         set = true;
         callbacks.forEach(c -> c.valueChanged(displayedValue()));
         intCallbacks.forEach(c -> c.accept(getIntValue()));
         valueCallbacks.forEach(c -> c.accept(value));
      }
   }

   public void unset() {
      set = false;
      callbacks.forEach(c -> c.valueChanged(displayedValue()));
      intCallbacks.forEach(c -> c.accept(getIntValue()));
      valueCallbacks.forEach(c -> c.accept(value));
   }


   public void reset() {
      set(values.get(0));
   }

   @Override
   public void increment(final int inc) {
      final Integer index = valueToIndex.get(value);
      if (index != null) {
         final int next = index + inc;
         if (next >= 0 && next < values.size()) {
            value = values.get(next);
            callbacks.forEach(c -> c.valueChanged(displayedValue()));
            intCallbacks.forEach(c -> c.accept(getIntValue()));
            valueCallbacks.forEach(c -> c.accept(value));
         }
      }
   }

   @Override
   public int getMax() {
      return values.size() - 1;
   }

   @Override
   public int getMin() {
      return 0;
   }

   @Override
   public void addValueObserver(final StringValueChangedCallback callback) {
      callbacks.add(callback);
   }

   public void addEnumValueObserver(final Consumer<T> callback) {
      valueCallbacks.add(callback);
   }

   @Override
   public void addIntValueObserver(final IntConsumer callback) {
      intCallbacks.add(callback);
   }

   @Override
   public void addRangeObserver(final RangeChangedCallback callback) {
   }

   @Override
   public int getIntValue() {
      if (value == null || !set) {
         return 0;
      }
      return valueToIndex.get(value);
   }

   @Override
   public String displayedValue() {
      return set ? valueToDisplay.get(value) : "<--->";
   }

   @Override
   public String get() {
      return displayedValue();
   }

   @Override
   public String getLimited(final int maxLength) {
      return displayedValue();
   }

   @Override
   public void markInterested() {
   }

   @Override
   public boolean isSubscribed() {
      return false;
   }

   @Override
   public void setIsSubscribed(final boolean value) {
   }

   @Override
   public void subscribe() {
   }

   @Override
   public void unsubscribe() {
   }


}
