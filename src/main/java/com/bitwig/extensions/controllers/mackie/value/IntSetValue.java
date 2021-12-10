package com.bitwig.extensions.controllers.mackie.value;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.stream.Stream;

public class IntSetValue {
   private final Set<Integer> values = new HashSet<>();
   private final List<IntConsumer> sizeListener = new ArrayList<>();

   public IntSetValue() {
   }

   public void addSizeValueListener(final IntConsumer listener) {
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
         sizeListener.forEach(l -> l.accept(newSize));
      }
   }

   public void add(final int index) {
      final int oldSize = values.size();
      values.add(index);
      final int newSize = values.size();
      if (oldSize != newSize) {
         sizeListener.forEach(l -> l.accept(newSize));
      }
   }

}
