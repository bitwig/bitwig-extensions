package com.bitwig.extensions.controllers.mackie.bindings.ring;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mackie.display.RingDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;

public class RingDisplayParameterBinding extends RingDisplayBinding<Parameter> {

   public RingDisplayParameterBinding(final Parameter source, final RingDisplay target, final RingDisplayType type) {
      super(target, source, type);
      final int vintRange = type.getRange() + 1;
      source.value().addValueObserver(vintRange, v -> valueChange(type.getOffset() + v));
      source.exists().addValueObserver(this::handleExists);
   }

   public void handleExists(final boolean exist) {
      if (isActive()) {
         valueChange(calcValue(exist));
      }
   }

   private void valueChange(final int value) {
      if (isActive()) {
         getTarget().sendValue(value, false);
      }
   }

   protected int calcValue(final boolean exists) {
      return exists ? type.getOffset() + (int) (getSource().value().get() * type.getRange()) : 0;
   }

   @Override
   protected int calcValue() {
      return getSource().exists().get() ? type.getOffset() + (int) (getSource().value().get() * type.getRange()) : 0;
   }
}
