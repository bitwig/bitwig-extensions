package com.bitwig.extensions.framework2;

import com.bitwig.extension.controller.api.HardwareAction;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareActionBinding;

public class HarwareActionBinding extends
   HardwareBinding<HardwareAction, HardwareActionBindable, com.bitwig.extension.controller.api.HardwareActionBinding>
{
   public HarwareActionBinding(final Object exclusiveSource, final HardwareAction source, final HardwareActionBindable target)
   {
      super(exclusiveSource, source, target);
   }

   @Override
   protected HardwareActionBinding addHardwareBinding()
   {
      return getSource().addBinding(getTarget());
   }

}
