package com.bitwig.extensions.controllers.mackie.bindings.ring;

import com.bitwig.extensions.controllers.mackie.display.RingDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.value.IntValueObject;

public class RingDisplayIntValueBinding extends RingDisplayBinding<IntValueObject> {

   public RingDisplayIntValueBinding(final RingDisplay target, final IntValueObject source,
                                     final RingDisplayType type) {
      super(target, source, type);
      source.addMaxValueObserver(this::maxChange);
      source.addValueObserver(this::valueChange);
   }

   private void maxChange(final int max) {
      if (isActive()) {
         getTarget().sendValue(calcValue(getSource().get(), max), false);
      }
   }

   private void valueChange(final int value) {
      if (isActive()) {
         getTarget().sendValue(calcValue(value, getSource().getMax()), false);
      }
   }


   private int calcValue(final int value, final int max) {
      final int min = getSource().getMin();
      final double factor = (double) type.getRange() / (double) (max - min);
      return (int) Math.round((value - min) * factor) + type.getOffset();
   }

   @Override
   protected int calcValue() {
      return calcValue(getSource().get(), getSource().getMax());
   }
}
