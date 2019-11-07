package com.bitwig.extensions.framework.animation;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.bitwig.extension.controller.ControllerExtension;

public class BlinkAnimation extends BooleanAnimation
{
   public BlinkAnimation(
      final ControllerExtension controllerExtension,
      final BooleanSupplier isOnSupplier,
      final BooleanSupplier offValueSupplier)
   {
      super(controllerExtension, isOnSupplier, offValueSupplier);
   }

   public BlinkAnimation(
      final ControllerExtension controllerExtension,
      final BooleanSupplier isOnSupplier,
      final Supplier<Boolean> offValueSupplier)
   {
      super(controllerExtension, isOnSupplier, offValueSupplier);
   }

   @Override
   protected boolean getAnimatedBoolValueAtTime(final double timeInSec)
   {
      final double cycleDuration = mOnDuration + mOffDuration;
      final double timeInCycle = timeInSec % cycleDuration;

      return timeInCycle <= mOnDuration;
   }

   private double mOnDuration = 0.1;

   private double mOffDuration = 0.1;
}
