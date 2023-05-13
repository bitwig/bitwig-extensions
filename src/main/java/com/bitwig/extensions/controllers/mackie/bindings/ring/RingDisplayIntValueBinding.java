package com.bitwig.extensions.controllers.mackie.bindings.ring;

import com.bitwig.extensions.controllers.mackie.display.RingDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.value.IntValue;

public class RingDisplayIntValueBinding extends RingDisplayBinding<IntValue> {

   public RingDisplayIntValueBinding(final RingDisplay target, final IntValue source, final RingDisplayType type) {
      super(target, source, type);
      source.addRangeObserver(this::rangeChanged);
      source.addIntValueObserver(this::valueChange);
   }

   private void rangeChanged(final int min, final int max) {
      if (isActive()) {
         getTarget().sendValue(calcValue(getSource().getIntValue(), min, max), false);
      }
   }

   private void valueChange(final int value) {
      if (isActive()) {
         getTarget().sendValue(calcValue(value, getSource().getMin(), getSource().getMax()), false);
      }
   }


   private int calcValue(final int value, final int min, final int max) {
      final double factor = (double) type.getRange() / (double) (max - min);
      return (int) Math.round((value - min) * factor) + type.getOffset();
   }

   @Override
   protected int calcValue() {
      return calcValue(getSource().getIntValue(), getSource().getMin(), getSource().getMax());
   }
}
