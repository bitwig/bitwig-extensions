package com.bitwig.extensions.controllers.mackie.bindings.ring;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extensions.controllers.mackie.display.RingDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;

public class RingDisplayBoolBinding extends RingDisplayBinding<BooleanValue> {


   public RingDisplayBoolBinding(final BooleanValue source, final RingDisplay target, final RingDisplayType type) {
      super(target, source, type);
      source.addValueObserver(this::handleBooleanValue);
   }

   public void handleBooleanValue(final boolean exist) {
      if (isActive()) {
         valueChange(calcValue(exist));
      }
   }

   private void valueChange(final int value) {
      if (isActive()) {
         getTarget().sendValue(value, false);
      }
   }

   private int calcValue(final boolean exists) {
      return exists ? type.getOffset() + type.getRange() : type.getOffset();
   }

   @Override
   protected int calcValue() {
      return calcValue(getSource().get());
   }


}
