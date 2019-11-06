package com.bitwig.extensions.framework;

import com.bitwig.extension.controller.api.BooleanHardwareOutputValue;
import com.bitwig.extension.controller.api.BooleanValue;

class BooleanValueOutputValueBinding extends Binding<BooleanValue, BooleanHardwareOutputValue>
{
   public BooleanValueOutputValueBinding(final BooleanValue source, final BooleanHardwareOutputValue target)
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
      getTarget().setSourceValue(getSource());
   }

}
