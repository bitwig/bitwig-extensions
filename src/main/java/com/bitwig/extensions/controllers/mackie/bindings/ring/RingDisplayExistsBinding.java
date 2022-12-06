package com.bitwig.extensions.controllers.mackie.bindings.ring;

import com.bitwig.extension.controller.api.ObjectProxy;
import com.bitwig.extensions.controllers.mackie.display.RingDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;

public class RingDisplayExistsBinding extends RingDisplayBinding<ObjectProxy> {

   public RingDisplayExistsBinding(final ObjectProxy source, final RingDisplay target, final RingDisplayType type) {
      super(target, source, type);
      source.exists().addValueObserver(this::handleExists);
   }

   public void handleExists(final boolean exist) {
      if (isActive()) {
         getTarget().sendValue(calcValue(exist), false);
      }
   }

   protected int calcValue(final boolean exists) {
      return exists ? type.getOffset() + type.getRange() - 1 : type.getOffset();
   }

   @Override
   protected int calcValue() {
      return calcValue(getSource().exists().get());
   }

}
