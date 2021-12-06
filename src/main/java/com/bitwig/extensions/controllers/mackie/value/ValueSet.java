package com.bitwig.extensions.controllers.mackie.value;

import com.bitwig.extension.callback.DoubleValueChangedCallback;
import com.bitwig.extension.controller.api.SettableDoubleValue;

import java.util.ArrayList;
import java.util.List;

public class ValueSet implements IncrementalValue, SettableDoubleValue {
   private double value;
   private int currentIndex = 0;
   private List<DoubleValueChangedCallback> listeners;
   private List<ValueString> values;

   private static class ValueString {
      private double value;
      private String representation;

      public ValueString(final double value, final String representation) {
         this.value = value;
         this.representation = representation;
      }
   }

   {
      values = new ArrayList<>();
      listeners = new ArrayList<>();
   }

   public ValueSet add(final String valStr, final double value) {
      values.add(new ValueString(value, valStr));
      return this;
   }

   @Override
   public void increment(final int inc) {
   }

   @Override
   public String displayedValue() {
      return null;
   }

   @Override
   public double get() {
      return value;
   }

   @Override
   public void markInterested() {
   }

   @Override
   public void addValueObserver(final DoubleValueChangedCallback callback) {
      listeners.add(callback);
   }

   @Override
   public boolean isSubscribed() {
      return !listeners.isEmpty();
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

   @Override
   public void set(final double value) {
      if (this.value != value) {
         this.value = value;
         for (final DoubleValueChangedCallback listener : listeners) {
            listener.valueChanged(value);
         }
      }
   }

   @Override
   public void inc(final double amount) {
      set(value + amount);
   }
}
