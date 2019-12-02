package com.bitwig.extensions.framework;

import java.util.function.Supplier;

import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;

public class InternalLightStateBinding extends Binding<Supplier<InternalHardwareLightState>, MultiStateHardwareLight>
{
   protected InternalLightStateBinding(
      final Supplier<InternalHardwareLightState> source, final MultiStateHardwareLight target)
   {
      super(target, source, target);
   }

   @Override
   protected void deactivate()
   {
      getTarget().state().setValueSupplier(null);
   }

   @Override
   protected void activate()
   {
      getTarget().state().setValueSupplier(getSource());
   }
}
