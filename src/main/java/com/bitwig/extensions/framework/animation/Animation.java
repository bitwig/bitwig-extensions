package com.bitwig.extensions.framework.animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;

public abstract class Animation<ValueType> implements Supplier<ValueType>
{
   private static final int FRAMES_PER_SEC = 20;

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

            startedAnimation(this);
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

            stoppedAnimation(this);
         }

         return mOffValueSupplier.get();
      }
   }

   private static void startedAnimation(final Animation animation)
   {
      final ControllerHost host = animation.mControllerExtension.getHost();

      List<Animation> runningAnimations = HOST_TO_RUNNING_ANIMATIONS_MAP.get(host);

      if (runningAnimations == null)
      {
         runningAnimations = new ArrayList<>();
         HOST_TO_RUNNING_ANIMATIONS_MAP.put(host, runningAnimations);
      }

      runningAnimations.add(animation);

      if (runningAnimations.size() == 1)
      {
         // Start pumping the animations for this host.

         pumpFlush(host, runningAnimations);
      }
   }

   private static void stoppedAnimation(final Animation animation)
   {
      final ControllerHost host = animation.mControllerExtension.getHost();

      final List<Animation> runningAnimations = HOST_TO_RUNNING_ANIMATIONS_MAP.get(host);

      assert runningAnimations != null;
      assert runningAnimations.contains(animation);

      runningAnimations.remove(animation);
   }

   private static void pumpFlush(final ControllerHost host, final List<Animation> runningAnimations)
   {
      host.requestFlush();

      if (!runningAnimations.isEmpty())
         host.scheduleTask(() -> pumpFlush(host, runningAnimations), 1000 / FRAMES_PER_SEC);
   }

   protected abstract ValueType getAnimatedValueAtTime(double timeInSec);

   private final ControllerExtension mControllerExtension;

   private final BooleanSupplier mIsOnSupplier;

   private long mAnimationStartTime = -1;

   private final Supplier<ValueType> mOffValueSupplier;

   private static final Map<ControllerHost, List<Animation>> HOST_TO_RUNNING_ANIMATIONS_MAP = new HashMap<>();
}
