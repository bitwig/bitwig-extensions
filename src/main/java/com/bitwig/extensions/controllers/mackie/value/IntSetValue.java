package com.bitwig.extensions.controllers.mackie.value;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class IntSetValue extends DerivedStringValueObject {
   private final Set<Integer> values = new HashSet<>();
   private final List<SizeChangeCallback> sizeListener = new ArrayList<>();

   public interface SizeChangeCallback {
      void valueChanged(int oldValue, int newValue);
   }

   public IntSetValue() {
      super();
   }

   @Override
   public void init() {
   }

   public void addSizeValueListener(final SizeChangeCallback listener) {
      sizeListener.add(listener);
   }

   public Stream<Integer> stream() {
      return values.stream();
   }

   public void remove(final int index) {
      final int oldSize = values.size();
      values.remove(index);
      final int newSize = values.size();
      if (oldSize != newSize) {
         sizeListener.forEach(l -> l.valueChanged(oldSize, newSize));
         fireChanged(Integer.toString(newSize));
      }
   }

   public void add(final int index) {
      final int oldSize = values.size();
      values.add(index);
      final int newSize = values.size();
      if (oldSize != newSize) {
         sizeListener.forEach(l -> l.valueChanged(oldSize, newSize));
         fireChanged(convert(newSize));
      }
   }

   private String convert(final int value) {
      if (value == 0) {
         return "[---]";
      }
      return "[ " + values.size() + " ]";
   }

   @Override
   public String get() {
      return convert(values.size());
   }

   public boolean isEmpty() {
      return values.isEmpty();
   }
}
