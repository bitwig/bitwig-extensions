package com.bitwig.extensions.framework;

import java.util.function.Supplier;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;

class LightColorOutputBinding extends Binding<Supplier<Color>, MultiStateHardwareLight>
{

   protected LightColorOutputBinding(final Supplier<Color> source, final MultiStateHardwareLight target)
   {
      super(target, source, target);
   }

   @Override
   protected void deactivate()
   {
      getTarget().setColorSupplier(null);
   }

   @Override
   protected void activate()
   {
      getTarget().setColorSupplier(getSource());
   }

}
