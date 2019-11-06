package com.bitwig.extensions.framework;

import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareControl;

public abstract class AbstractRelativeHardwareControlBinding<TargetType extends RelativeHardwarControlBindable, HardwareBindingType extends com.bitwig.extension.controller.api.RelativeHardwareControlBinding>
   extends HardwareBinding<RelativeHardwareControl, TargetType, HardwareBindingType>
   implements BindingWithSensitivity
{
   public AbstractRelativeHardwareControlBinding(
      final RelativeHardwareControl source,
      final TargetType target)
   {
      super(source, target);
   }

   public double getSensitivity()
   {
      return mSensitivity;
   }

   @Override
   protected final HardwareBindingType addHardwareBinding()
   {
      return addHardwareBinding(mSensitivity * mGlobalSensitivity);
   }

   protected abstract HardwareBindingType addHardwareBinding(double sensitivity);

   public AbstractRelativeHardwareControlBinding setSensitivity(final double sensitivity)
   {
      if (sensitivity != mSensitivity)
      {
         mSensitivity = sensitivity;

         updateEffectiveSensitivity();
      }

      return this;
   }

   @Override
   public void setGlobalSensitivity(double value)
   {
      if (value != mGlobalSensitivity)
      {
         mGlobalSensitivity = value;
         updateEffectiveSensitivity();
      }
   }

   private void updateEffectiveSensitivity()
   {
      if (isActive())
         getHardwareBinding().setSensitivity(mSensitivity * mGlobalSensitivity);
   }

   @Override
   protected void setLayer(Layer layer)
   {
      super.setLayer(layer);

      setGlobalSensitivity(layer.getLayers().getGlobalSensitivity());
   }

   private double mSensitivity = 1, mGlobalSensitivity = 1;
}
