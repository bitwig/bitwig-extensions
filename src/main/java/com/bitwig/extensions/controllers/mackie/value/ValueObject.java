package com.bitwig.extensions.controllers.mackie.value;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class ValueObject<T> implements IncrementalValue, IntRange {

   private final List<ChangeCallback<T>> callbacks = new ArrayList<>();
   private final List<Consumer<T>> intCallbacks = new ArrayList<>();

   public interface IntValueConverter<T> {
      int convert(int range, T value);
   }

   private final IncrementHandler<T> incrementHandler;
   private final StringConverter<T> converter;
   private IntValueConverter<T> integerConverter;

   private T value;

   @Override
   public void addRangeListener(final int range, final IntConsumer callback) {
      intCallbacks.add(value -> callback.accept(convert(range, value)));
   }

   private int convert(final int range, final T value) {
      if (integerConverter != null) {
         return integerConverter.convert(range, value);
      }
      return 0;
   }

   @Override
   public int getIntValue() {
      return 0;
   }

   @FunctionalInterface
   public interface ChangeCallback<T> {
      void valueChanged(T oldValue, T newValue);
   }

   @FunctionalInterface
   public interface StringConverter<T> {
      String convert(T value);
   }

   @FunctionalInterface
   public interface IncrementHandler<T> {
      T increment(T value, int increment);
   }

   public ValueObject(final T initValue) {
      value = initValue;
      incrementHandler = null;
      converter = null;
   }

   public ValueObject(final T initValue, final IncrementHandler<T> incrementHandler,
                      final StringConverter<T> converter) {
      value = initValue;
      this.incrementHandler = incrementHandler;
      this.converter = converter;
   }

   public void addValueObserver(final ChangeCallback<T> callback) {
      if (!callbacks.contains(callback)) {
         callbacks.add(callback);
      }
   }

   public void setIntegerConverter(final IntValueConverter<T> integerConverter) {
      this.integerConverter = integerConverter;
   }

   public void set(final T value) {
      if (this.value == value) {
         return;
      }
      final T oldValue = this.value;
      this.value = value;
      for (final ChangeCallback<T> listener : callbacks) {
         listener.valueChanged(oldValue, value);
      }
   }

   @Override
   public void increment(final int inc) {
      if (incrementHandler != null) {
         set(incrementHandler.increment(value, inc));
      }
   }

   @Override
   public String displayedValue() {
      if (converter != null) {
         return converter.convert(value);
      }
      return value.toString();
   }

   public T get() {
      return value;
   }

}
