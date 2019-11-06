package com.bitwig.extensions.framework;

import com.bitwig.extension.controller.api.HardwareBindable;
import com.bitwig.extension.controller.api.HardwareBindingSource;

public abstract class HardwareBinding<SourceType extends HardwareBindingSource, TargetType extends HardwareBindable, HardwareBindingType extends com.bitwig.extension.controller.api.HardwareBinding>
   extends Binding<SourceType, TargetType>
{
   protected HardwareBinding(final Object exclusiveSource, final SourceType source, final TargetType target)
   {
      super(exclusiveSource, source, target);
   }

   protected HardwareBinding(final SourceType source, final TargetType target)
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
      mHardwareBinding = null;
   }

   protected HardwareBindingType getHardwareBinding()
   {
      return mHardwareBinding;
   }

   private HardwareBindingType mHardwareBinding;
}
