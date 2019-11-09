package com.bitwig.extensions.framework.animation;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.bitwig.extension.controller.ControllerExtension;

public class BlinkAnimation extends BooleanAnimation
{
   public BlinkAnimation(
      final ControllerExtension controllerExtension,
      final BooleanSupplier isOnSupplier,
      final BooleanSupplier offValueSupplier,
      final double onDuration,
      final double offDuration)
   {
      super(controllerExtension, isOnSupplier, offValueSupplier);

      mOnDuration = onDuration;
      mOffDuration = offDuration;
   }

   public BlinkAnimation(
      final ControllerExtension controllerExtension,
      final BooleanSupplier isOnSupplier,
      final BooleanSupplier offValueSupplier,
      final double onOffDuration)
   {
      this(controllerExtension, isOnSupplier, offValueSupplier, onOffDuration, onOffDuration);
   }

   public BlinkAnimation(
      final ControllerExtension controllerExtension,
      final BooleanSupplier isOnSupplier,
      final Supplier<Boolean> offValueSupplier,
      final double onDuration,
      final double offDuration)
   {
      super(controllerExtension, isOnSupplier, offValueSupplier);

      mOnDuration = onDuration;
      mOffDuration = offDuration;
   }

   @Override
   protected boolean getAnimatedBoolValueAtTime(final double timeInSec)
   {
      final double cycleDuration = mOnDuration + mOffDuration;
      final double timeInCycle = timeInSec % cycleDuration;

      return timeInCycle <= mOnDuration;
   }

   public double getOnDuration()
   {
      return mOnDuration;
   }

   public void setOnDuration(final double onDuration)
   {
      mOnDuration = onDuration;
   }

   public double getOffDuration()
   {
      return mOffDuration;
   }

   public void setOffDuration(final double offDuration)
   {
      mOffDuration = offDuration;
   }

   private double mOnDuration = 0.1;

   private double mOffDuration = 0.1;
}
