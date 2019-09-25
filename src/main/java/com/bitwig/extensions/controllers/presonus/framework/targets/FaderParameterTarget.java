package com.bitwig.extensions.controllers.presonus.framework.targets;

import com.bitwig.extension.controller.api.Parameter;

public class FaderParameterTarget implements TouchFaderTarget
{
   public FaderParameterTarget(final Parameter parameter)
   {
      mParameter = parameter;
   }

   @Override
   public float get()
   {
      return (float) mParameter.get();
   }

   @Override
   public void set(final int value, final int steps)
   {
      mParameter.set(value, steps);
   }

   @Override
   public void touch(final boolean b)
   {
      mParameter.touch(b);
   }

   private final Parameter mParameter;
}
