package com.bitwig.extensions.framework;

import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareControl;

public class RelativeHardwareControlBinding extends
   HardwareBinding<RelativeHardwareControl, RelativeHardwarControlBindable, com.bitwig.extension.controller.api.RelativeHardwareControlBinding>
   implements BindingWithSensitivity
{
   public RelativeHardwareControlBinding(
      final RelativeHardwareControl source,
      final RelativeHardwarControlBindable target)
   {
      super(source, target);
   }

   @Override
   protected com.bitwig.extension.controller.api.RelativeHardwareControlBinding addHardwareBinding()
   {
      return getTarget().addBindingWithSensitivity(getSource(), mSensitivity);
   }

   @Override
   public double getSensitivity()
   {
      return mSensitivity;
   }

   @Override
   public RelativeHardwareControlBinding setSensitivity(final double sensitivity)
   {
      if (sensitivity != mSensitivity)
      {
         mSensitivity = sensitivity;

         if (isActive())
            getHardwareBinding().setSensitivity(sensitivity);
      }

      return this;
   }

   private double mSensitivity = 1;
}
