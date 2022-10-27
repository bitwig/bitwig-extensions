package com.bitwig.extensions.controllers.mackie.value;

import com.bitwig.extension.callback.StringValueChangedCallback;
import com.bitwig.extension.controller.api.StringValue;

import java.util.ArrayList;
import java.util.List;

public abstract class DerivedStringValueObject implements StringValue {
   private final List<StringValueChangedCallback> callbacks = new ArrayList<>();

   public DerivedStringValueObject() {
      init();
   }

   public abstract void init();

   public void fireChanged(final String value) {
      callbacks.forEach(callback -> callback.valueChanged(value));
   }

   @Override
   public String getLimited(final int maxLength) {
      return get();
   }

   @Override
   public void markInterested() {
   }

   @Override
   public void addValueObserver(final StringValueChangedCallback callback) {
      callbacks.add(callback);
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
