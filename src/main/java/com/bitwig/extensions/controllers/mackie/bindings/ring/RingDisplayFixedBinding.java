package com.bitwig.extensions.controllers.mackie.bindings.ring;

import com.bitwig.extensions.controllers.mackie.display.RingDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;

public class RingDisplayFixedBinding extends RingDisplayBinding<Integer> {

   public RingDisplayFixedBinding(final Integer source, final RingDisplay target, final RingDisplayType type) {
      super(target, source, type);
   }

   @Override
   protected void activate() {
      getTarget().sendValue(type.getOffset() + getSource(), false);
   }

   @Override
   protected int calcValue() {
      return type.getOffset() + getSource();
   }

}
