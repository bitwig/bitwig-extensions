package com.bitwig.extensions.framework2;

import com.bitwig.extension.controller.api.HardwareAction;

class HardwareActionRunnableBinding extends Binding<HardwareAction, Runnable>
{
   public HardwareActionRunnableBinding(final HardwareAction source, final Runnable target)
   {
      super(source, target);
   }

   @Override
   protected void deactivate()
   {
      getSource().setActionCallback(null);
   }

   @Override
   protected void activate()
   {
      getSource().setActionCallback(getTarget());
   }

}
