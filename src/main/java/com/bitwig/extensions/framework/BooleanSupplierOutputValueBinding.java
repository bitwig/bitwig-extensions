package com.bitwig.extensions.framework;

import java.util.function.BooleanSupplier;

import com.bitwig.extension.controller.api.BooleanHardwareOutputValue;

class BooleanSupplierOutputValueBinding extends Binding<BooleanSupplier, BooleanHardwareOutputValue>
{
   public BooleanSupplierOutputValueBinding(final BooleanSupplier source, final BooleanHardwareOutputValue target)
   {
      super(target, source, target);
   }

   @Override
   protected void deactivate()
   {
      getTarget().setValue(false);
   }

   @Override
   protected void activate()
   {
      getTarget().setValueSupplier(getSource());
   }

}
