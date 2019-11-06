package com.bitwig.extensions.framework;

import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareControl;

public class RelativeHardwareControlBinding extends
   AbstractRelativeHardwareControlBinding<RelativeHardwarControlBindable, com.bitwig.extension.controller.api.RelativeHardwareControlBinding>
{
   public RelativeHardwareControlBinding(
      final RelativeHardwareControl source,
      final RelativeHardwarControlBindable target)
   {
      super(source, target);
   }

   @Override
   protected com.bitwig.extension.controller.api.RelativeHardwareControlBinding addHardwareBinding(
      double sensitivity)
   {
      return getTarget().addBindingWithSensitivity(getSource(), sensitivity);
   }

}
