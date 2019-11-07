package com.bitwig.extensions.framework.animation;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;

public abstract class Animation<ValueType> implements Supplier<ValueType>
{
   protected Animation(
      final ControllerExtension controllerExtension,
      final BooleanSupplier isOnSupplier,
      final Supplier<ValueType> offValueSupplier)
   {
      super();
      mControllerExtension = controllerExtension;
      mIsOnSupplier = isOnSupplier;
      mOffValueSupplier = offValueSupplier;
   }

   @Override
   public ValueType get()
   {
      final boolean isOn = mIsOnSupplier.getAsBoolean();

      if (isOn)
      {
         long timeSinceStartInMs = 0;

         if (mAnimationStartTime == -1)
         {
            // Animation started

            mAnimationStartTime = System.currentTimeMillis();

            pumpFlush();
         }
         else
         {
            timeSinceStartInMs = System.currentTimeMillis() - mAnimationStartTime;
         }

         final double timeSinceStartInSec = timeSinceStartInMs / 1000.0;

         return getAnimatedValueAtTime(timeSinceStartInSec);
      }
      else
      {
         if (mAnimationStartTime != -1)
         {
            // Animation stopped

            mAnimationStartTime = -1;
         }

         return mOffValueSupplier.get();
      }
   }

   private void pumpFlush()
   {
      final ControllerHost host = mControllerExtension.getHost();

      host.requestFlush();

      if (mIsOnSupplier.getAsBoolean())
         host.scheduleTask(this::pumpFlush, 50);
   }

   protected abstract ValueType getAnimatedValueAtTime(double timeInSec);

   private final ControllerExtension mControllerExtension;

   private final BooleanSupplier mIsOnSupplier;

   private long mAnimationStartTime = -1;

   private final Supplier<ValueType> mOffValueSupplier;
}
