package com.bitwig.extensions.framework2;

import com.bitwig.extension.controller.api.HardwareBindable;
import com.bitwig.extension.controller.api.HardwareBindingSource;

public abstract class HardwareBinding<SourceType extends HardwareBindingSource, TargetType extends HardwareBindable, HardwareBindingType extends com.bitwig.extension.controller.api.HardwareBinding>
   extends Binding<SourceType, TargetType>
{

   protected HardwareBinding(SourceType source, TargetType target)
   {
      super(source, target);
   }

   @Override
   protected void activate()
   {
      assert mHardwareBinding == null;

      mHardwareBinding = addHardwareBinding();
   }

   protected abstract HardwareBindingType addHardwareBinding();

   @Override
   protected void deactivate()
   {
      assert mHardwareBinding != null;

      mHardwareBinding.removeBinding();
   }

   protected HardwareBindingType getHardwareBinding()
   {
      return mHardwareBinding;
   }

   private HardwareBindingType mHardwareBinding;
}
