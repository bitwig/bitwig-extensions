package com.bitwig.extension.controllers.studiologic;

import com.bitwig.extension.controller.api.BoolHardwareOutputValue;
import com.bitwig.extension.controller.api.BooleanValue;

public class BooleanValueOutputValueBinding extends Binding<BooleanValue, BoolHardwareOutputValue>
{
   public BooleanValueOutputValueBinding(final BooleanValue source, final BoolHardwareOutputValue target)
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
      getTarget().setSourceValue(getSource());
   }

}
