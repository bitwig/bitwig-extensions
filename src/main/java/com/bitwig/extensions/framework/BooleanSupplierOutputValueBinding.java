package com.bitwig.extensions.framework;

import java.util.function.BooleanSupplier;

import com.bitwig.extension.controller.api.BooleanHardwareProperty;

class BooleanSupplierOutputValueBinding extends Binding<BooleanSupplier, BooleanHardwareProperty>
{
   public BooleanSupplierOutputValueBinding(final BooleanSupplier source, final BooleanHardwareProperty target)
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
