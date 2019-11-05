package com.bitwig.extension.controllers.studiologic;

import java.util.function.BooleanSupplier;

import com.bitwig.extension.controller.api.BoolHardwareOutputValue;

public class BooleanSupplierOutputValueBinding extends Binding<BooleanSupplier, BoolHardwareOutputValue>
{
   public BooleanSupplierOutputValueBinding(final BooleanSupplier source, final BoolHardwareOutputValue target)
   {
      super(source, target);
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
