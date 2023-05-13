package com.bitwig.extensions.controllers.mackie.value;

import com.bitwig.extension.callback.StringValueChangedCallback;
import com.bitwig.extension.controller.api.StringValue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * A list of double Values with their corresponding String values that can be incremented.
 */
public class ValueSet implements IncrementalValue, StringValue {
   private int currentIndex = 0;
   private final List<StringValueChangedCallback> listeners = new ArrayList<>();
   private final List<ValueString> values = new ArrayList<>();
   private final List<IntConsumer> indexListeners = new ArrayList<>();


   private static class ValueString {
      private final double value;
      private final String representation;

      public ValueString(final double value, final String representation) {
         this.value = value;
         this.representation = representation;
      }
   }

   @Override
   public void addValueObserver(final StringValueChangedCallback callback) {
      listeners.add(callback);
   }

   public void addIndexValueObserver(final IntConsumer callback) {
      indexListeners.add(callback);
   }

   public ValueSet add(final String valStr, final double value) {
      values.add(new ValueString(value, valStr));
      return this;
   }

   public ValueSet select(final int i) {
      if (i >= 0 && i < values.size()) {
         currentIndex = i;
      }
      return this;
   }

   public int size() {
      return values.size();
   }

   public int getCurrentIndex() {
      return currentIndex;
   }

   @Override
   public void increment(final int inc) {
      select(currentIndex + inc);
      listeners.forEach(listener -> listener.valueChanged(values.get(currentIndex).representation));
      indexListeners.forEach(listener -> listener.accept(currentIndex));
   }

   @Override
   public String displayedValue() {
      return values.get(currentIndex).representation;
   }

   @Override
   public String get() {
      return values.get(currentIndex).representation;
   }

   public double getValue() {
      return values.get(currentIndex).value;
   }

   @Override
   public String getLimited(final int maxLength) {
      return values.get(currentIndex).representation;
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
