package com.bitwig.extensions.controllers.mackie.value;

import com.bitwig.extension.callback.StringValueChangedCallback;
import com.bitwig.extension.controller.api.StringValue;

import java.util.ArrayList;
import java.util.List;

public class BasicStringValue implements StringValue {
   private final List<StringValueChangedCallback> callbacks = new ArrayList<>();
   private String value;

   public BasicStringValue(final String initValue) {
      value = initValue;
   }

   @Override
   public void markInterested() {
   }

   @Override
   public void addValueObserver(final StringValueChangedCallback callback) {
      if (!callbacks.contains(callback)) {
         callbacks.add(callback);
      }
   }

   @Override
   public boolean isSubscribed() {
      return !callbacks.isEmpty();
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
   public String get() {
      return value;
   }

   @Override
   public String getLimited(final int maxLength) {
      return get();
   }

   public void set(final String value) {
      this.value = value;
      callbacks.forEach(callback -> callback.valueChanged(value));
   }

}
