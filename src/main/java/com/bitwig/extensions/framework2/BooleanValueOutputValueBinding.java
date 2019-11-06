package com.bitwig.extensions.framework2;

import com.bitwig.extension.controller.api.BoolHardwareOutputValue;
import com.bitwig.extension.controller.api.BooleanValue;

class BooleanValueOutputValueBinding extends Binding<BooleanValue, BoolHardwareOutputValue>
{
   public BooleanValueOutputValueBinding(final BooleanValue source, final BoolHardwareOutputValue target)
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
