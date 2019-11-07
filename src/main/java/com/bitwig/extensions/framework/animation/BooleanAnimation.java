package com.bitwig.extensions.framework.animation;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.BooleanValue;

public abstract class BooleanAnimation extends Animation<Boolean> implements BooleanSupplier
{
   protected BooleanAnimation(
      final ControllerExtension controllerExtension,
      final BooleanSupplier isOnSupplier,
      final Supplier<Boolean> offValueSupplier)
   {
      super(controllerExtension, isOnSupplier, offValueSupplier);

      if (isOnSupplier instanceof BooleanValue)
         ((BooleanValue)isOnSupplier).markInterested();
   }

   protected BooleanAnimation(
      final ControllerExtension controllerExtension,
      final BooleanSupplier isOnSupplier,
      final BooleanSupplier offValueSupplier)
   {
      this(controllerExtension, isOnSupplier, (Supplier<Boolean>)() -> offValueSupplier.getAsBoolean());

      if (offValueSupplier instanceof BooleanValue)
         ((BooleanValue)offValueSupplier).markInterested();
   }

   @Override
   public final boolean getAsBoolean()
   {
      return get().booleanValue();
   }

   @Override
   protected final Boolean getAnimatedValueAtTime(final double timeInSec)
   {
      return getAnimatedBoolValueAtTime(timeInSec);
   }

   protected abstract boolean getAnimatedBoolValueAtTime(final double timeInSec);
}
