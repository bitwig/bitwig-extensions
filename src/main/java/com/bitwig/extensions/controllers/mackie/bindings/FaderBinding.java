package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mackie.display.FaderResponse;
import com.bitwig.extensions.framework.Binding;

public class FaderBinding extends Binding<Parameter, FaderResponse> {

   private double lastValue = 0.0;

   public FaderBinding(final Parameter source, final FaderResponse target) {
      super(target, source, target);
      source.value().addValueObserver(this::valueChange);
   }

   private void valueChange(final double value) {
      lastValue = value;
      if (isActive()) {
         getTarget().sendValue(value);
      }
   }

   @Override
   protected void deactivate() {
   }

   @Override
   protected void activate() {
      getTarget().sendValue(lastValue);
   }

}
