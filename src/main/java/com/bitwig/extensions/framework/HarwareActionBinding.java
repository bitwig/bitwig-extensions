package com.bitwig.extensions.framework;

import com.bitwig.extension.controller.api.HardwareAction;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareActionBinding;

class HarwareActionBinding extends
   HardwareBinding<HardwareAction, HardwareActionBindable, com.bitwig.extension.controller.api.HardwareActionBinding>
{
   public HarwareActionBinding(final Object actionOwner, final HardwareAction source, final HardwareActionBindable target)
   {
      super(actionOwner, source, target);
   }

   @Override
   protected HardwareActionBinding addHardwareBinding()
   {
      return getSource().addBinding(getTarget());
   }

}
