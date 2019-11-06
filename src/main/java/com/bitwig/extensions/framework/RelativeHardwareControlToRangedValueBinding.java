package com.bitwig.extensions.framework;

import com.bitwig.extension.controller.api.RelativeHardwareControl;
import com.bitwig.extension.controller.api.SettableRangedValue;

public class RelativeHardwareControlToRangedValueBinding extends
   HardwareBinding<RelativeHardwareControl, SettableRangedValue, com.bitwig.extension.controller.api.RelativeHardwareControlToRangedValueBinding>
{
   public RelativeHardwareControlToRangedValueBinding(
      final RelativeHardwareControl source,
      final SettableRangedValue target)
   {
      super(source, target);
   }

   @Override
   protected com.bitwig.extension.controller.api.RelativeHardwareControlToRangedValueBinding addHardwareBinding()
   {
      return getTarget().addBindingWithRangeAndSensitivity(getSource(), mMin, mMax, mSensitivity);
   }

   public double getMin()
   {
      return mMin;
   }

   public RelativeHardwareControlToRangedValueBinding setMin(final double min)
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

   public RelativeHardwareControlToRangedValueBinding setMax(final double max)
   {
      if (max != mMax)
      {
         mMax = max;

         if (isActive())
            getHardwareBinding().setMaxNormalizedValue(max);
      }

      return this;
   }

   public RelativeHardwareControlToRangedValueBinding setRange(final double min, final double max)
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

   public double getSensitivity()
   {
      return mSensitivity;
   }

   public RelativeHardwareControlToRangedValueBinding setSensitivity(final double sensitivity)
   {
      if (sensitivity != mSensitivity)
      {
         mSensitivity = sensitivity;

         if (isActive())
            getHardwareBinding().setSensitivity(sensitivity);
      }

      return this;
   }

   private double mMin = 0, mMax = 1;

   private double mSensitivity = 1;
}
