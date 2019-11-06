package com.bitwig.extensions.framework2;

import com.bitwig.extension.controller.api.ColorValue;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;

class LightColorOutputBinding extends Binding<ColorValue, MultiStateHardwareLight>
{

   protected LightColorOutputBinding(final ColorValue source, final MultiStateHardwareLight target)
   {
      super(target, source, target);
   }

   @Override
   protected void deactivate()
   {
      getTarget().setColorValue(null);
   }

   @Override
   protected void activate()
   {
      getTarget().setColorValue(getSource());
   }

}
