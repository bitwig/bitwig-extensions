package com.bitwig.extensions.controllers.mackie.bindings.ring;

import com.bitwig.extensions.controllers.mackie.display.RingDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.value.ValueSet;

public class RingDisplayValueSetBinding extends RingDisplayBinding<ValueSet> {

   private final double factor;

   public RingDisplayValueSetBinding(final ValueSet source, final RingDisplay target, final RingDisplayType type) {
      super(target, source, type);
      factor = (double) type.getRange() / (double) (source.size() - 1);
      source.addIndexValueObserver(this::valueChange);
   }

   private void valueChange(final int index) {
      if (isActive()) {
         getTarget().sendValue(calcValue(index), false);
      }
   }

   @Override
   protected int calcValue() {
      return calcValue(getSource().getCurrentIndex());
   }

   private int calcValue(final int index) {
      return (int) Math.round(index * factor) + type.getOffset();
   }

}
