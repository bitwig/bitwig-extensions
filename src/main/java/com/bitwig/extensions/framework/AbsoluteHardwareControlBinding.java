package com.bitwig.extensions.framework;

import com.bitwig.extension.controller.api.AbsoluteHardwarControlBindable;
import com.bitwig.extension.controller.api.AbsoluteHardwareControl;

public class AbsoluteHardwareControlBinding extends
   HardwareBinding<AbsoluteHardwareControl, AbsoluteHardwarControlBindable, com.bitwig.extension.controller.api.AbsoluteHardwareControlBinding>
{
   public AbsoluteHardwareControlBinding(
      final AbsoluteHardwareControl source,
      final AbsoluteHardwarControlBindable target)
   {
      super(source, source, target);
   }

   @Override
   protected com.bitwig.extension.controller.api.AbsoluteHardwareControlBinding addHardwareBinding()
   {
      return getSource().addBindingWithRange(getTarget(), mMin, mMax);
   }

   public double getMin()
   {
      return mMin;
   }

   public AbsoluteHardwareControlBinding setMin(final double min)
   {
      if (min != mMin)
      {
         mMin = min;

         if (isActive())
            getHardwareBinding().setMinNormalizedValue(min);
      }

      return this;
   }

   public double getMax()
   {
      return mMax;
   }

   public AbsoluteHardwareControlBinding setMax(final double max)
   {
      if (max != mMax)
      {
         mMax = max;

         if (isActive())
            getHardwareBinding().setMaxNormalizedValue(max);
      }

      return this;
   }

   public AbsoluteHardwareControlBinding setRange(final double min, final double max)
   {
      if (min != mMin || max != mMax)
      {
         mMin = min;
         mMax = max;

         if (isActive())
            getHardwareBinding().setNormalizedRange(min, max);
      }

      return this;
   }

   private double mMin = 0, mMax = 1;
}
