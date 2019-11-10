package com.bitwig.extensions.controllers.presonus.faderport;

import com.bitwig.extensions.framework.Binding;

class DisplayBinding extends Binding<Display, DisplayTarget>
{
   public DisplayBinding(final Display source, final DisplayTarget target)
   {
      super(source, target);
   }

   @Override
   protected void deactivate()
   {
      getSource().setDisplayTarget(null);
   }

   @Override
   protected void activate()
   {
      getSource().setDisplayTarget(getTarget());
   }

}
