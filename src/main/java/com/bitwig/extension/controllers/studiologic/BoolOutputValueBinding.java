package com.bitwig.extension.controllers.studiologic;

import com.bitwig.extension.controller.api.BoolHardwareOutputValue;
import com.bitwig.extension.controller.api.BooleanValue;

public class BoolOutputValueBinding extends Binding<BooleanValue, BoolHardwareOutputValue>
{
   public BoolOutputValueBinding(final BooleanValue source, final BoolHardwareOutputValue target)
   {
      super(source, target);
   }

   @Override
   protected void deactivate()
   {
      getTarget().set(null);
   }

   @Override
   protected void activate()
   {
      getTarget().set(getSource());
   }

}
