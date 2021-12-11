package com.bitwig.extensions.controllers.mackie.bindings.ring;

import com.bitwig.extension.controller.api.RangedValue;
import com.bitwig.extensions.controllers.mackie.display.RingDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;

public class RingDisplayRangedValueBinding extends RingDisplayBinding<RangedValue> {

   public RingDisplayRangedValueBinding(final RangedValue source, final RingDisplay target,
                                        final RingDisplayType type) {
      super(target, source, type);
      final int vintRange = type.getRange() + 1;
      source.addValueObserver(vintRange, v -> valueChange(type.getOffset() + v));
   }

   private void valueChange(final int value) {
      if (isActive()) {
         getTarget().sendValue(value, false);
      }
   }

   @Override
   protected int calcValue() {
      return type.getOffset() + (int) (getSource().get() * type.getRange());
   }


}
