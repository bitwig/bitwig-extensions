package com.bitwig.extension.controllers.studiologic;

import com.bitwig.extension.controller.api.HardwareAction;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareActionBinding;

public class HarwareActionBinding extends
   HardwareBinding<HardwareAction, HardwareActionBindable, com.bitwig.extension.controller.api.HardwareActionBinding>
{
   public HarwareActionBinding(final HardwareAction source, final HardwareActionBindable target)
   {
      super(source, target);
   }

   @Override
   protected HardwareActionBinding addHardwareBinding()
   {
      return getSource().addBinding(getTarget());
   }

}
