package com.bitwig.extensions.framework;

import com.bitwig.extension.controller.api.HardwareAction;

class HardwareActionRunnableBinding extends Binding<HardwareAction, Runnable>
{
   public HardwareActionRunnableBinding(final Object actionOwner, final HardwareAction source, final Runnable target)
   {
      super(actionOwner, source, target);
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
